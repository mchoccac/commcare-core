package org.javarosa.core.util.externalizable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class ExtWrapIntEncodingSmall extends ExtWrapIntEncoding {
    public static final int DEFAULT_BIAS = 1;

    /* max magnitude of negative number encodable in one byte; allowed range [0,254]
     * increasing this steals from the max positive range
     * ex.: BIAS = 0   -> [0,254] will fit in one byte; all other values will overflow
     *      BIAS = 30  -> [-30,224]
     *      BIAS = 254 -> [-254,0]
     */
    private int bias;

    /* serialization */

    public ExtWrapIntEncodingSmall(long l) {
        this(l, DEFAULT_BIAS);
    }

    public ExtWrapIntEncodingSmall(long l, int bias) {
        val = l;
        this.bias = bias;
    }

    /* deserialization */

    public ExtWrapIntEncodingSmall() {
        this(DEFAULT_BIAS);
    }

    //need the garbage param or else it conflicts with (long) constructor
    public ExtWrapIntEncodingSmall(int bias) {
        this.bias = bias;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapIntEncodingSmall(ExtUtil.toLong(val), bias);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException {
        byte b = in.readByte();
        long l;

        if (b == (byte)0xff) {
            l = in.readInt();
        } else {
            l = (b < 0 ? b + 256 : b) - bias;
        }

        val = l;
    }

    /**
     * serialize a numeric value, only using as many bytes as needed. splits up the value into
     * chunks of 7 bits, using as many chunks as needed to unambiguously represent the value. each
     * chunk is serialized as a single byte, where the most-significant bit is set to 1 to indicate
     * there are more bytes to follow, or 0 to indicate the last byte
     */
    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        int n = ExtUtil.toInt(((Long)val));

        if (n >= -bias && n < 255 - bias) {
            n += bias;
            out.writeByte((byte)(n >= 128 ? n - 256 : n));
        } else {
            out.writeByte(0xff);
            out.writeInt(n);
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException {
        bias = in.readUnsignedByte();
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {
        out.writeByte((byte)bias);
    }
}
