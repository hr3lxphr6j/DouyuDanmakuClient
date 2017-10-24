import moe.chigusa.DouyuDanmaku.DouyuDanmakuClient;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;

/**
 * @author chigusa
 * @function
 * @date 2017/10/24
 */
public class DouyuDanmakuClientTest {
    @Test
    public void test() throws InterruptedException {
        DouyuDanmakuClient client = new DouyuDanmakuClient(96291);
        BlockingQueue queue = client.subscribe();
        client.run();
        while (true) {
            System.out.println(queue.take());
        }
    }
}