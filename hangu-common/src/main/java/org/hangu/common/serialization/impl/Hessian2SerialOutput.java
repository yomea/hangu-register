package org.hangu.common.serialization.impl;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import java.io.IOException;
import java.io.OutputStream;
import org.hangu.common.serialization.SerialOutput;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:17
 */
public class Hessian2SerialOutput implements SerialOutput {

    private Hessian2Output output;

    public Hessian2SerialOutput(OutputStream outputStream) {
        output = new Hessian2Output(outputStream);
        output.setSerializerFactory(new SerializerFactory());
    }

    @Override
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        output.writeObject(obj);
    }

    @Override
    public void writeString(String text) throws IOException {
        output.writeString(text);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }
}
