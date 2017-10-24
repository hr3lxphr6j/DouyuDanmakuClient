斗鱼Java弹幕客户端 DouyuDanmakuClient
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

DLC
----
随手扔个python版的（py大法好
```python
import socket
import time
from select import select
from struct import pack, unpack


def _pack(data):
    return pack('iih??', len(data) + 9, len(data) + 9, 689, 0, 0) + data.encode() + b'\x00'


def douyu_client(rid):
    s = socket.socket()
    last_beat_time = time.time()
    s.connect(('openbarrage.douyutv.com', 8601))
    s.setblocking(False)
    s.sendall(_pack('type@=loginreq/roomid@=%d/' % rid))
    s.sendall(_pack('type@=joingroup/rid@=%d/gid@=-9999/' % rid))
    while 1:
        select([s], [], [], 30)
        if time.time() - last_beat_time >= 30:
            s.sendall(_pack('type@=mrkl/'))
            last_beat_time = time.time()
        head = s.recv(4)
        body_length = unpack('i', head)[0]
        msg = s.recv(body_length)
        if msg:
            print(msg[8:-1].decode('utf8'))


if __name__ == '__main__':
    try:
        douyu_client(96291)
    except KeyboardInterrupt:
        exit()
```