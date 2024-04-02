package org.hangu.center.discover.manager;

import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hangu.center.common.constant.HanguCons;

/**
 * @author wuzhenhong
 * @date 2024/3/7 10:00
 */
public class NettyClientEventLoopManager {

    private static final NioEventLoopGroup NIO_EVENT_LOOP_GROUP = new NioEventLoopGroup(HanguCons.DEF_IO_THREADS << 3);
    private static final AtomicBoolean CLOSE = new AtomicBoolean(false);

    public static final NioEventLoopGroup getEventLoop() {
        return NIO_EVENT_LOOP_GROUP;
    }

    public static final boolean isClose() {
        return CLOSE.get();
    }

    public static final void close() {
        CLOSE.set(true);
        NIO_EVENT_LOOP_GROUP.shutdownGracefully();
    }

}
