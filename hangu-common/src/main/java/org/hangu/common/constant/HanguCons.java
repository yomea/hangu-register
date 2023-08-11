package org.hangu.common.constant;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:53
 */
public final class HanguCons {

    /**
     * 魔数
     */
    public static final short MAGIC = (short) 0xabcd;

    public static final int CPUS = Runtime.getRuntime().availableProcessors();

    public static final int DEF_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);
}
