package mytest;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

public class MyClient {

    public Bootstrap clientBootStrap;
    public boolean isConnected = false;
    //public Channel clientChannel = null;
    public HttpResponseHandler responseHandler = null;
    public Http2ClientInitializer initializer = null;
    public ChannelFuture connectFuture = null;

    public void Connect() {
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
                        "127.0.0.1",
                        8443);
                connectFuture.sync();

                System.out.println("Connected.");
                isConnected = true;

            } catch (SSLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
