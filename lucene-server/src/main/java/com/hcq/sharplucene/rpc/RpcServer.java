package com.hcq.sharplucene.rpc;

import com.hcq.sharplucene.rpc.anno.RpcAnnotation;
import com.hcq.sharplucene.rpc.netty.RegistryHandler;
import com.hcq.sharplucene.rpc.zk.IRegisterCenter;
import com.hcq.sharplucene.util.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Slf4j
public class RpcServer {
    //用保存所有可用的服务
    public static ConcurrentHashMap<String, Object> registryMap = new ConcurrentHashMap<String, Object>();
    private IRegisterCenter registerCenter; //注册中心
    private String serviceAddress; //服务发布地址
    private List<String> classNames = new ArrayList<String>();
    private Properties config = new Properties();

    public RpcServer(IRegisterCenter registerCenter, String serviceAddress) {
        this.registerCenter = registerCenter;
        this.serviceAddress = serviceAddress;
        scannerClass(Config.getInstance().getProvider("lucene.provider"));
        doRegister();
    }

    /*
     * 递归扫描
     */
    private void scannerClass(String packageName) {
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        try {
            Files.walkFileTree(Paths.get(url.getPath().replaceFirst("/","")),new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    classNames.add(packageName + "." + file.toFile().getName().replace(".class", "").trim());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
//        for (File file : dir.listFiles()) {
//            //如果是一个文件夹，继续递归
//            if (file.isDirectory()) {
//                scannerClass(packageName + "." + file.getName());
//            } else {
//                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
//            }
//        }
    }

    /**
     * 完成注册
     */
    private void doRegister() {
        if (classNames.size() == 0) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                RpcAnnotation annotation = clazz.getAnnotation(RpcAnnotation.class);
                String serviceName = annotation.value().getName();
                String version = annotation.version();
                if (version != null && !version.equals("")) {
                    serviceName = serviceName + "-" + version;
                }
//                registryMap.put(serviceName, clazz.newInstance());
                registryMap.put(serviceName, className);//由于存在有参构造，故等到真正invoke时再去实例化
                registerCenter.register(serviceName, serviceAddress);
                log.info("注册服务成功：" + serviceName + "->" + serviceAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        String[] addrs=serviceAddress.split(":");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                    Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            pipeline.addLast("encoder", new ObjectEncoder());
                            pipeline.addLast("decoder", new ObjectDecoder(
                                    Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                            pipeline.addLast(new RegistryHandler(registryMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(8080).sync();
            log.info("服务已启动 正在监听端口"+addrs[1]);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
