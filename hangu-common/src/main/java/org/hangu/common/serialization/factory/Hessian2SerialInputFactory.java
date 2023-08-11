package org.hangu.common.serialization.factory;

import java.io.InputStream;
import org.hangu.common.serialization.SerialInput;
import org.hangu.common.serialization.impl.Hessian2SerialInput;

/**
 * @author wuzhenhong
 * @date 2023/8/1 16:31
 */
public class Hessian2SerialInputFactory {

    public static SerialInput createSerialization(InputStream inputStream) {

        Hessian2SerialInput input = new Hessian2SerialInput(inputStream);
        return input;
    }
}
