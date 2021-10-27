package mytest;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

public class MyClient {

    public Bootstrap clientBootStrap;
    public boolean isConnected = false;

    public HttpResponseHandler responseHandler = null;
    public Http2ClientInitializer initializer = null;
    public ChannelFuture connectFuture = null;
    public Channel clientChannel = null;

    public boolean isQSeed = false;
    public String getHostname(){
        //return isQSeed ? "id.qseed.jp" : "api.game-idolypride.jp";
        return "127.0.0.1";
    }
    public static int PORT = 8443;

    public void connect() {
        if (!isConnected) {

            clientBootStrap = new Bootstrap();
            //  EventLoopGroup group = ctx.channel().eventLoop();  // 关键点？

            clientBootStrap
                    //.group(group)
                    .group(new NioEventLoopGroup())
                    .option(ChannelOption.TCP_NODELAY, true);    //  ??

            clientBootStrap.channel(NioSocketChannel.class);

            final SslContext sslCtx;

            SslProvider provider = SslProvider.OPENSSL;
            //OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
            try {
                sslCtx = SslContextBuilder.forClient()
                        .sslProvider(provider)
                        /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                         * Please refer to the HTTP/2 specification for cipher requirements. */
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .applicationProtocolConfig(new ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2,
                                ApplicationProtocolNames.HTTP_1_1))
                        .build();

                initializer = new Http2ClientInitializer(
                        sslCtx,
                        16*1024);

                clientBootStrap.handler(initializer);
                System.out.println("Connecting...");
                connectFuture = clientBootStrap.connect(
                        getHostname(), PORT);
                        //"127.0.0.1",
                        //8443);
                connectFuture.sync();
                clientChannel = connectFuture.channel();

                System.out.println("Connected.");
                isConnected = true;

            } catch (SSLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public FullHttpResponse forward(FullHttpRequest request, int streamId){
        Http2SettingsHandler http2SettingsHandler = this.initializer.settingsHandler();
        try {
            http2SettingsHandler.awaitSettings(30, TimeUnit.SECONDS);  //  TODO：超时时间

            HttpResponseHandler responseHandler = this.initializer.responseHandler();

//            FullHttpRequest request1 = new DefaultFullHttpRequest(
//                    request.protocolVersion(),
//                    request.method(),
//                    request.uri(),
//                    request.content().copy(),
//                    request.headers().copy(),
//                    request.trailingHeaders().copy());
            FullHttpRequest request1 = request.copy();
            request1.headers().set(HttpHeaderNames.HOST, this.getHostname());   //  是否有必要？
            //     HTTP extension header which will identify the scheme pseudo header from the HTTP/2
            //     event(s) responsible for generating an DotNetty.Codecs.Http.IHttpObject
            //     "x-http2-scheme"
            request1.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTPS);    //  是否有必要？
            responseHandler.put(streamId,
                    this.clientChannel.write(request1),
                    this.clientChannel.newPromise());

            //Task task = clientChannel.WriteAndFlushAsync(request1);
            //task.Wait(TimeSpan.FromMinutes(1)); // TODO 设置超时时间

            //request.retain();
            this.clientChannel.flush();     //  此行执行前 request refCnt=1 执行后refCnt=0？咋回事
            //  第一个GET / refCnt=0时不能retain 抛异常
            responseHandler.awaitResponses(1, TimeUnit.MINUTES);

            FullHttpResponse response = responseHandler.response;

            //  这里添加循环超时，用来规避高并发时随机偶现response.content()不可读
            //  并且response与responseHandler.response不同的奇怪问题。真搞不懂咋发生的！
            //  规避后Chrome、Edge、Firefox均不复现，IE仍有问题，但content-length对的，怀疑IE有BUG
            for(long now = System.currentTimeMillis();
                response.content().readableBytes() <= 0 && System.currentTimeMillis() <= now + 1000L;){
                if(response == responseHandler.response)
                    Thread.sleep(100L);
                else
                    response = responseHandler.response;
            }
            HttpUtil.setContentLength(response, response.content().readableBytes());
            return response;
        } catch (Exception e) {
            e.printStackTrace();    //  settings超时？
        }
        return null;
    }
}
