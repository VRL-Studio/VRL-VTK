package edu.gcsc.vrl.vtk;

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
public class Int8Decoder extends DataDecoder {

    public Int8Decoder() {
        setType("Int8");
    }

    @Override
    public void decode(byte[] data) throws IOException {
        
            DataInputStream in =
                    new DataInputStream(
                    new BufferedInputStream(
                    new ByteArrayInputStream(data)));

            int arraySize = data.length / 4;

            setArray(new int[arraySize]);

            for (int i = 0; i < arraySize; i++) {
                ((int[]) getArray())[i] = in.readByte();
            }
    }
}
