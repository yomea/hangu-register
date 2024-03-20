package org.hangu.center.common.constant;

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

    public static final byte REQ_MARK = (byte) 0xF0;
    public static final byte COMMAND_MARK = (byte) 0x0F;

    public static final String GROUP_NAME = "HAN_GU_CENTER_GROUP_NAME";
    public static final String INTERFACE_NAME = "HAN_GU_CENTER_INTERFACE_NAME";
    public static final String VERSION = "1.1.1";
}
