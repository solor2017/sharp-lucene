package com.hcq.sharplucene.rpc.zk;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Slf4j
public class RpcProxyHandler extends ChannelInboundHandlerAdapter {  
	  
    private Object response;    
      
    public Object getResponse() {    
	    return response;    
	}    
  
    @Override    
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        response = msg;
    }    
        
    @Override    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {    
        System.out.println("client exception is general");    
    }    
} 
