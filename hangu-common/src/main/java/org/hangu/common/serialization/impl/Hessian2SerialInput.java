package org.hangu.common.serialization.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.SerializerFactory;
import java.io.IOException;
import java.io.InputStream;
import org.hangu.common.serialization.SerialInput;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:17
 */
public class Hessian2SerialInput implements SerialInput {

    private Hessian2Input input;

    public Hessian2SerialInput(InputStream inputStream) {
        input = new Hessian2Input(inputStream);
        input.setSerializerFactory(new SerializerFactory());
    }

    @Override
    public <T> T readObject(Class<T> tClass) throws IOException {
        return (T) input.readObject(tClass);
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public String readString() throws IOException {
        return input.readString();
    }
}
