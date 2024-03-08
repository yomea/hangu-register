package org.hangu.center.common.serialization;

import java.io.IOException;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:15
 */
public interface SerialInput {

    <T> T readObject(Class<T> tClass) throws IOException;

    int readInt() throws IOException;

    String readString() throws IOException;


}
