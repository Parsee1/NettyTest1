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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.io.Console;
import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
//import static io.netty.example.http2.Http2ExampleUtil.firstValue;
//import static io.netty.example.http2.Http2ExampleUtil.toInt;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.lang.Integer.parseInt;

public class Http2RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public Http2RequestHandler(MyClient myClient) {
        super();
        this.myClient = myClient;
    }

    public MyClient myClient = null;


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        try {
            super.channelActive(ctx);
//            if(!myClient.isConnected)
//                myClient.Connect(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        //get session from ctx.channel and pass it to ServerA.session
        try {
            super.channelRegistered(ctx);
//            if(!myClient.isConnected)
//                myClient.Connect(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder queryString = new QueryStringDecoder(request.uri());
        if (!myClient.isConnected) {
            //new Thread(() -> myClient.Connect(ctx)).start();
            myClient.Connect();
        }
        while (myClient.connectFuture == null || !myClient.connectFuture.isDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        if (myClient.connectFuture != null && myClient.connectFuture.isDone()) {
            String streamId = streamId(request);
        }

        //  TODO: bla bla blah
    }

    private static String streamId(FullHttpRequest request) {
        return request.headers().get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
