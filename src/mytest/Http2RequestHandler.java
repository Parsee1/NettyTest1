/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package mytest;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.HttpConversionUtil;

import java.util.concurrent.TimeUnit;

//import static io.netty.example.http2.Http2ExampleUtil.firstValue;
//import static io.netty.example.http2.Http2ExampleUtil.toInt;
import static java.lang.Integer.parseInt;

public class Http2RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public Http2RequestHandler(MyClient myClient) {
        super();
        this.myClient = myClient;
    }

    public MyClient myClient = null;


//    @Override
//    public void channelActive(ChannelHandlerContext ctx) {
//        try {
//            super.channelActive(ctx);
////            if(!myClient.isConnected)
////                myClient.Connect(ctx);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void channelRegistered(ChannelHandlerContext ctx) {
//        //get session from ctx.channel and pass it to ServerA.session
//        try {
//            super.channelRegistered(ctx);
////            if(!myClient.isConnected)
////                myClient.Connect(ctx);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder queryString = new QueryStringDecoder(request.uri());
        if (!myClient.isConnected) {
            myClient.isQSeed = "/getId".equals(queryString);
            //new Thread(() -> myClient.Connect(ctx)).start();
            myClient.connect();
        }
        while (myClient.clientChannel == null || !myClient.connectFuture.isDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        String streamId = streamId(request);
        //  TODO: bla bla blah
        FullHttpResponse response = myClient.forward(request, Integer.parseInt(streamId));

        streamId(response, streamId);
        ctx.writeAndFlush(response);    //  hit

        if(request.refCnt() > 1)
            request.release(request.refCnt() - 1);  // 这里断点不该命中
        else if(request.refCnt() <= 0)
            request.retain();   // 会抛异常。谁吃了msg？？
    }

    private static String streamId(FullHttpRequest request) {
        return request.headers().get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
    }

    private static void streamId(FullHttpResponse response, String streamId) {
        response.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        try {
            super.channelRead(ctx, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
