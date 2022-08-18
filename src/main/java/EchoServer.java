import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Secret;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * EchoServer 코드
 */

public class EchoServer {
    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

	public void start() throws Exception {
        myHandler handler = new myHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(this.port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(handler); // ChannelHandler 등록
                    }
                });
            ChannelFuture future = bootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}

//class myHandler extends ChannelInboundHandlerAdapter {
class myHandler extends ChannelHandlerAdapter {
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        System.out.println(">>> Server Received : " + in.toString(CharsetUtil.UTF_8));
        
        if(in.toString(CharsetUtil.UTF_8).equals("test")) {
        	System.out.println("test method start.");
        	this.test();
         }
        else if(in.toString(CharsetUtil.UTF_8).equals("serviceaccount")) {
        	this.serviceAccount();
         }
        else if(in.toString(CharsetUtil.UTF_8).equals("setvpn")) {
        	this.setVPN();
         }
        String toUpper = in.toString(CharsetUtil.UTF_8).toUpperCase();
        ctx.write(Unpooled.copiedBuffer(toUpper.getBytes()));
    }
	
	
	void test() {
    	try (KubernetesClient k8s = new KubernetesClientBuilder().build()) {
            // Print names of all pods in specified namespace
    		System.out.println("kubeclient start");
    		System.setProperty("kubernetes.auth.tryKubeConfig", "true");
          k8s.pods().inNamespace("default").list()
              .getItems()
              .stream()
              .map(Pod::getMetadata)
              .map(ObjectMeta::getName)
              //.forEach(logger::info);
              .forEach(System.out::println);
        }
	}
	
	
	void serviceAccount() {
		System.setProperty("kubernetes.auth.tryKubeConfig", "true");
     	ServiceAccount serviceAccount1 = new ServiceAccountBuilder()
     			  .withNewMetadata().withName("controller-user").endMetadata()
     			  .withAutomountServiceAccountToken(false)
     			  .build();
     	try (final KubernetesClient k8s = new KubernetesClientBuilder().build()) {
     		k8s.serviceAccounts().inNamespace("user1").create(serviceAccount1);
     	}
	}
	
	String getTokenName(KubernetesClient k8s, String serviceAccountName) {
    	List<String> tokenlist = k8s.serviceAccounts().inNamespace("user1").withName(serviceAccountName).get(). 
    	getSecrets(). //List<ObjectReference> 반환
    	stream().
    	map(ObjectReference::getName).
    	toList();
    	return tokenlist.get(0);
    }
    
   
    String getTokenString(KubernetesClient k8s, String tokenName) throws UnsupportedEncodingException {
    	//Secret secret = k8s.secrets().inNamespace("spring").withName(tokenName).get();
    	Secret secret = k8s.secrets().withName(tokenName).get();
    	String encodeToken = secret.getData().get("token");
    	Decoder decoder = Base64.getDecoder();
    	byte[] decodedBytes2 = decoder.decode(encodeToken);
    	String token = new String(decodedBytes2, "UTF-8");
    	return token;
    }
    
    String getCaCrt(KubernetesClient k8s, String tokenName) throws UnsupportedEncodingException {
    	//Secret secret = k8s.secrets().inNamespace("spring").withName(tokenName).get();
    	Secret secret = k8s.secrets().withName(tokenName).get();
    	String encodeCrt = secret.getData().get("ca.crt");
    	Decoder decoder = Base64.getDecoder();
    	byte[] decodedBytes2 = decoder.decode(encodeCrt);
    	String crt = new String(decodedBytes2, "UTF-8");
    	return crt;
    }
    
    String getNamespace(KubernetesClient k8s, String tokenName)  throws UnsupportedEncodingException {
    	//Secret secret = k8s.secrets().inNamespace("spring").withName(tokenName).get();
    	Secret secret = k8s.secrets().withName(tokenName).get();
    	String encodeNamespace = secret.getData().get("namespace");
    	Decoder decoder = Base64.getDecoder();
    	byte[] decodedBytes2 = decoder.decode(encodeNamespace);
    	String namespace = new String(decodedBytes2, "UTF-8");
    	return namespace;
    }
	
	void setVPN() {
    	KubernetesClient k8s = new KubernetesClientBuilder().build();
    	System.out.println("make controller client.");
    	String token;
    	String crt;
    	String namespace;
    	String tokenName = this.getTokenName(k8s,"controller-user");
    	System.out.println("get Token name..");
    	
    	try{
    		token = this.getTokenString(k8s, tokenName);
    		Path path = Paths.get("./token");
    		Files.write(path, token.getBytes());
    	} 
    	catch(UnsupportedEncodingException e) {
    	}
    	catch(IOException e) {
    		e.printStackTrace();
    	};
    	
    	try{
    		crt = this.getCaCrt(k8s, tokenName);
    		Path path = Paths.get("./ca.crt");
    		Files.write(path, crt.getBytes());
    	} catch(UnsupportedEncodingException e) {
    	}
    	catch(IOException e) {
    		e.printStackTrace();
    	};
    	
    	try{
    		namespace = this.getNamespace(k8s, tokenName);
    		Path path = Paths.get("./namespace");
    		Files.write(path, namespace.getBytes());
    	} catch(UnsupportedEncodingException e) {
    	}
    	catch (IOException e) {
    		e.printStackTrace();
    	};
    	System.out.println("get Token information..");
    	
    	//System.setProperty("kubernetes.disable.autoConfig", "false");
    	System.setProperty("kubernetes.master", "https://192.168.56.10:6443");
    	System.setProperty("kubernetes.auth.tryKubeConfig", "false");
    	System.setProperty("kubernetes.auth.serviceAccount.token", "./token");
    	System.setProperty("kubernetes.certs.ca.file", "./ca.crt");
    	System.setProperty("kubenamespace", "./namespace");
    	ConfigBuilder configBuilder = new ConfigBuilder().withMasterUrl("https://192.168.56.10:6443");
    	Config config = configBuilder.build();

    	try (KubernetesClient springUser = new KubernetesClientBuilder().withConfig(config).build()) {
    		ResourceDefinitionContext context = new ResourceDefinitionContext
    	        .Builder()
    	        .withGroup("network.tmaxanc.com")
    	        .withKind("VPN")
    	        .withPlural("vpns")
    	        .withNamespaced(true)
    	        .withVersion("v1")
    	        .build();// Load from Yaml
    		Resource<GenericKubernetesResource> vpnObject = springUser.genericKubernetesResources(context)
    				.load(myHandler.class.getResourceAsStream("./vpn_cr.yaml"));  // Create Custom Resource
    		vpnObject.create();
    	}
    }
	
	
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
            .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Error!!!!");
        cause.printStackTrace();
        ctx.close();
    }
}

