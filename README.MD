斗鱼弹幕Java客户端 DouyuDanmakuClient
==================================
emmm，交个netty的作业  
弹幕协议细节参考[斗鱼弹幕服务器第三方接入协议](http://dev-bbs.douyutv.com/forum.php?mod=viewthread&tid=399&extra=page%3D1)

栗子
----
```java
public class DouyuDanmakuClientTest {
    @Test
    public void test() throws InterruptedException {
        moe.chigusa.DouyuDanmaku.DouyuDanmakuClient client = new moe.chigusa.DouyuDanmaku.DouyuDanmakuClient(96291);
        BlockingQueue queue = client.subscribe();
        client.run();
        while (true) {
            System.out.println(queue.take());
        }
    }
}
```
存在问题
--------
反序列化算法不彻底，嵌套的List暂时没处理