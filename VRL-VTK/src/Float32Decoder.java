/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Float32Decoder extends DataDecoder {

    public Float32Decoder() {
        setType("Float32");
    }

    @Override
    public void decode(byte[] data) throws IOException {
        DataInputStream in =
                new DataInputStream(
                new BufferedInputStream(
                new ByteArrayInputStream(data)));

        int arraySize = data.length / 4;

        setArray(new float[arraySize]);

        for (int i = 0; i < arraySize; i++) {
            ((float[]) getArray())[i] = in.readFloat();
        }
    }
}
