package moe.chigusa.DouyuDanmaku;

import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 斗鱼弹幕客户端 netty实现
 *
 * @author chigusa Sara、YF
 * @function
 * @date 2017/10/24
 */
public class DouyuDanmakuClient {
    private static final String ADDRESS = "openbarrage.douyutv.com";
    private static final int PORT = 8601;
    private Bootstrap b;
    private ChannelFuture future;
    private BlockingQueue<Map<String, Object>> queue;
    private Gson gson;
    private int rid;

    /**
     * @param rid 房间ID
     */
    public DouyuDanmakuClient(int rid) {
        this.rid = rid;
        gson = new Gson();
        queue = new LinkedBlockingQueue<>();
        b = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        // TCP粘包拆包处理，去头部
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                                ByteOrder.LITTLE_ENDIAN,
                                2 << 15,
                                0,
                                4,
                                0,
                                12,
                                false));

                        // 去尾部分隔符
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(2 << 15, Unpooled.copiedBuffer("\0".getBytes())));

                        // 解码为字符串
                        ch.pipeline().addLast(new StringDecoder());

                        // STT2JSON测试
                        ch.pipeline().addLast(new MessageToMessageDecoder<String>() {
                            @Override
                            protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
                                String json = msg.replaceAll("@=", "\":\"")
                                        .replaceAll("/", "\",\"")
                                        .replaceAll("@A", "@")
                                        .replaceAll("@S", "/");
                                StringBuilder builder = new StringBuilder(json);
                                builder.replace(json.length() - 2, json.length(), "}");
                                builder.insert(0, "{\"");
                                out.add(gson.fromJson(builder.toString(), LinkedHashMap.class));
                            }
                        });

                        // 反序列化算法
//                        ch.pipeline().addLast(new MessageToMessageDecoder<String>() {
//                            @Override
//                            protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
//                                String[] res = msg.split("/");
//                                Map<String, Object> map = new HashMap<>();
//                                int flag = 0;
//                                for (int i = 0; i < res.length; i++) {
//
//                                    String[] temp = res[i].split("@=");
//
//                                    String key = temp[0];
//                                    String value = null;
//                                    if (temp.length > 1) {
//                                        while (temp[1].contains("@A")) {
//                                            temp[1] = temp[1].replaceAll("@A", "@");
//                                        }
//                                        value = temp[1].replaceAll("@S", "/");
//                                    }
//
//                                    if (value.contains("@=") && value.indexOf("//") == value.length() - 2) {
//                                        //System.out.println(value);
//                                        Map<String, String> user = new HashMap<>();
//                                        String[] user_temp = value.substring(0, value.indexOf("//")).split("/");
//
//                                        for (int j = 0; j < user_temp.length; j++) {
//                                            String[] ans = user_temp[j].split("@=");
//                                            String user_key = ans[0];
//                                            String user_value = null;
//                                            if (ans.length > 1) {
//                                                while (ans[1].contains("@A")) {
//                                                    ans[1] = ans[1].replaceAll("@A", "@");
//                                                }
//                                                user_value = ans[1].replaceAll("@S", "/");
//                                            }
//                                            user.put(user_key, user_value);
//                                        }
//                                        //System.out.println(user.toString());
//                                        map.put(key, user);
//                                        continue;
//                                    }
//                                    map.put(key, value);
//                                }
//                                //System.out.println(map.toString());
//                                out.add(map);
//                            }
//                        });

                        /*
                                  =================================================
                                  |   Byte0   |   Byte1   |   Byte2   |   Byte3   |
                                  =================================================
                                  |                    消息长度                     |
                                  =================================================
                                  |                    消息长度                     |
                           Header -------------------------------------------------
                                  |         消息类型        |  加密字段  |  保留字段   |
                                  =================================================
                           Body   |                数据部分('\0'结尾)                |
                                  =================================================
                         */
                        // 封包处理
                        ch.pipeline().addLast(new MessageToByteEncoder<String>() {
                            @Override
                            protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
                                out = out.order(ByteOrder.LITTLE_ENDIAN);
                                out.writeInt(msg.getBytes().length + 9);
                                out.writeInt(msg.getBytes().length + 9);
                                out.writeShort(689);
                                out.writeShort(0);
                                out.writeBytes(msg.getBytes());
                                out.writeByte(0);
                            }
                        });

                        // 逻辑处理
                        ch.pipeline().addLast(new ChannelHandlerAdapter() {
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            }

                            // 首次请求，登陆和加入分组，加入心跳连接
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ctx.writeAndFlush(String.format("type@=loginreq/roomid@=%d/", rid));
                                ctx.writeAndFlush(String.format("type@=joingroup/rid@=%d/gid@=-9999/", rid));
                                ctx.executor().scheduleAtFixedRate(
                                        () -> ctx.writeAndFlush("type@=mrkl/"), 10, 30, TimeUnit.SECONDS);
                            }

                            // 接收到数据加入队列
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                queue.offer((Map) msg);
                            }
                        });
                    }
                });
    }

    /**
     * @return 包含弹幕数据的队列对象
     */
    public BlockingQueue subscribe() {
        return queue;
    }

    /**
     * @return 房间ID
     */
    public int getRid() {
        return rid;
    }

    /**
     * 建立连接
     */
    public void run() {
        future = b.connect(new InetSocketAddress(ADDRESS, PORT));
    }

    /**
     * 停止
     */
    public void stop() {
        future.channel().closeFuture();
    }
}
