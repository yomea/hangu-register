package org.hangu.common.serialization.factory;

import java.io.OutputStream;
import org.hangu.common.serialization.SerialOutput;
import org.hangu.common.serialization.impl.Hessian2SerialOutput;

/**
 * @author wuzhenhong
 * @date 2023/8/1 16:31
 */
public class Hessian2SerialOutputFactory {

    public static SerialOutput createSerialization(OutputStream outputStream) {

        Hessian2SerialOutput hessian2SerialOutput = new Hessian2SerialOutput(outputStream);

        return hessian2SerialOutput;
    }
}
