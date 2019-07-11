package com.hcq.sharplucene.rpc.netty;

import com.hcq.sharplucene.core.provider.LocalIndexService;
import com.hcq.sharplucene.rpc.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.joor.Reflect;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Slf4j
public class RegistryHandler  extends ChannelInboundHandlerAdapter {

	ConcurrentHashMap<String, Object> registryMap;
    public RegistryHandler(ConcurrentHashMap<String, Object> registryMap){
		this.registryMap = registryMap;
    }

//	@Override
//	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		for(String interfaceName:registryMap.keySet()){
//			registerCenter.register(interfaceName,serviceAddress);
//			System.out.println("注册服务成功："+interfaceName+"->"+serviceAddress);
//		}
//	}

	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    	log.info("channelRead===="+msg.toString());
    	Object result = new Object();
		RpcRequest  request = (RpcRequest)msg;

		//以下均为反射操作，目的是通过反射调用服务
		Object[] args=request.getParameters();
		Class<?>[] types=new Class[args.length];
		for(int i=0;i<args.length;i++){
			types[i]=args[i].getClass();
		}
		String serviceName=request.getClassName();
		String version=request.getVersion();
		if(version!=null&&!version.equals("")){
			serviceName=serviceName+"-"+version;
		}
		//从registryMap中，根据客户端请求的地址，去拿到响应的服务，通过反射发起调用
		if(registryMap.containsKey(serviceName)) {
//			Object service=registryMap.get(serviceName);
			String service = (String) registryMap.get(serviceName);
			//@see 	https://github.com/jOOQ/jOOR
			Object localIndexService = Reflect.on(
					Class.forName(service)).create(request.getConstryctArgs()).get();//反射调用有参构造方法

			result = Reflect.on(localIndexService).call(request.getMethodName(), request.getParameters()).get();
//			Method method=service.getClass().getMethod(request.getMethodName(),types);
//			result = method.invoke(service,args);
		}


        //当客户端建立连接时，需要从自定义协议中获取信息，拿到具体的服务和实参
		//使用反射调用
//        if(registryMap.containsKey(request.getClassName())){
//        	Object clazz = registryMap.get(request.getClassName());
//        	Method method = clazz.getClass().getMethod(request.getMethodName(), request.getParames());
//        	result = method.invoke(clazz, request.getValues());
//        }
		ctx.write(result);
		ctx.flush();
		ctx.close();
    }
    
    @Override    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {    
         cause.printStackTrace();    
         ctx.close();    
    }



  
}
