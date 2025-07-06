package com.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

public class ChatNode {

    private final int port;
    private final List<String> peerAddresses;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final EventLoopGroup clientGroup = new NioEventLoopGroup();

    public ChatNode(int port, List<String> peerAddresses) {
        this.port = port;
        this.peerAddresses = peerAddresses;
    }

    public void start() throws Exception {
    	
    	
    	try {
    		 // Start server
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    String received = msg.toString(StandardCharsets.UTF_8);
                                    System.out.println("Received: " + received);
                                    
                                    if (received.contains("exit()")) {
                                    	// Forward to peers
                                        peerAddresses.forEach(addr -> {
                                            String[] parts = addr.split(":");
                                            sendToPeer(parts[0], Integer.parseInt(parts[1]), received);
                                        });
                                    	System.out.println("Bye; connection after exit command.");
                                        ctx.writeAndFlush(Unpooled.copiedBuffer("Bye", StandardCharsets.UTF_8))
                                        .addListener(f -> {
                                        	ctx.close();
                                        	ctx.channel().parent().close();
                                            ch.close();
                                        });
                                        
                                        return;
                                    }else if(!received.contains("enriched")){
                                    	// Enrich message
                                        String enriched = received + " + enriched(" + new Random().nextInt(1000) + ")";
                                        ctx.writeAndFlush(Unpooled.copiedBuffer(enriched, StandardCharsets.UTF_8));
                                        // Forward to peers
                                        peerAddresses.forEach(addr -> {
                                            String[] parts = addr.split(":");
                                            sendToPeer(parts[0], Integer.parseInt(parts[1]), enriched);
                                        });
                                    } 
                                }
                            });
                        }
                    });

            ChannelFuture serverFuture = serverBootstrap.bind(port).sync();
            System.out.println("Server listening on port " + port);

            // Keep server running
            serverFuture.channel().closeFuture().sync();
    	}finally {
    		 System.out.println("Shutting down boss/worker groups...");
    	        bossGroup.shutdownGracefully();
    	        workerGroup.shutdownGracefully();
    	        clientGroup.shutdownGracefully();
    	}
       
    }

    private void sendToPeer(String host, int port, String message) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                ByteBuf buffer = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);
                                ctx.writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE);
                            }
                        });
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                System.err.println("Failed to connect to " + host + ":" + port);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java ChatNode <port> <peer1Host:port,peer2Host:port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        List<String> peers = List.of(args[1].split(","));
        new ChatNode(port, peers).start();
    }
}

