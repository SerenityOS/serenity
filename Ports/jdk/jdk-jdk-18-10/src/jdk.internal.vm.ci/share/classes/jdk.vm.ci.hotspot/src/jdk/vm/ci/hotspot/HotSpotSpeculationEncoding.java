/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.vm.ci.hotspot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.SpeculationLog.SpeculationReasonEncoding;

/**
 * Implements a {@link SpeculationReasonEncoding} that {@linkplain #getByteArray() produces} a byte
 * array. Data is added via a {@link DataOutputStream}. When producing the final byte array, if the
 * total length of data exceeds {@value HotSpotSpeculationEncoding#MAX_LENGTH}, then a SHA-1 digest
 * of the data is produced instead.
 */
final class HotSpotSpeculationEncoding extends ByteArrayOutputStream implements SpeculationReasonEncoding {

    /**
     * Number of bits used for the length of an encoded speculation. The bit size of 5 is chosen to
     * accommodate specifying // the length of a SHA-1 digest (i.e., 20 bytes).
     */
    // Also defined in C++ JVMCINMethodData class - keep in sync.
    static final int LENGTH_BITS = 5;

    /**
     * The maximum length of an encoded speculation.
     */
    static final int MAX_LENGTH = (1 << LENGTH_BITS) - 1;

    static final int LENGTH_MASK = MAX_LENGTH;

    private DataOutputStream dos = new DataOutputStream(this);
    private byte[] result;

    HotSpotSpeculationEncoding() {
        super(SHA1_LENGTH);
    }

    private void checkOpen() {
        if (result != null) {
            throw new IllegalArgumentException("Cannot update closed speculation encoding");
        }
    }

    private static final int NULL_METHOD = -1;
    private static final int NULL_TYPE = -2;
    private static final int NULL_STRING = -3;

    @Override
    public void addByte(int value) {
        checkOpen();
        try {
            dos.writeByte(value);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public void addShort(int value) {
        checkOpen();
        try {
            dos.writeShort(value);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public void addMethod(ResolvedJavaMethod method) {
        if (!addNull(method, NULL_METHOD)) {
            checkOpen();
            if (method instanceof HotSpotResolvedJavaMethodImpl) {
                try {
                    dos.writeLong(((HotSpotResolvedJavaMethodImpl) method).getMetaspaceMethod());
                } catch (IOException e) {
                    throw new InternalError(e);
                }
            } else {
                throw new IllegalArgumentException("Cannot encode unsupported type " + method.getClass().getName() + ": " + method.format("%H.%n(%p)"));
            }
        }
    }

    @Override
    public void addType(ResolvedJavaType type) {
        if (!addNull(type, NULL_TYPE)) {
            checkOpen();
            if (type instanceof HotSpotResolvedObjectTypeImpl) {
                try {
                    dos.writeLong(((HotSpotResolvedObjectTypeImpl) type).getMetaspaceKlass());
                } catch (IOException e) {
                    throw new InternalError(e);
                }
            } else {
                throw new IllegalArgumentException("Cannot encode unsupported type " + type.getClass().getName() + ": " + type.toClassName());
            }
        }
    }

    @Override
    public void addString(String value) {
        if (!addNull(value, NULL_STRING)) {
            checkOpen();
            try {
                dos.writeChars(value);
            } catch (IOException e) {
                throw new InternalError(e);
            }
        }
    }

    @Override
    public void addInt(int value) {
        checkOpen();
        try {
            dos.writeInt(value);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public void addLong(long value) {
        checkOpen();
        try {
            dos.writeLong(value);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    private boolean addNull(Object o, int nullValue) {
        if (o == null) {
            addInt(nullValue);
            return true;
        }
        return false;
    }

    /**
     * Prototype SHA1 digest.
     */
    private static final MessageDigest SHA1;

    /**
     * Cloning the prototype is quicker than calling {@link MessageDigest#getInstance(String)} every
     * time.
     */
    private static final boolean SHA1_IS_CLONEABLE;
    private static final int SHA1_LENGTH = 20;

    static {
        MessageDigest sha1 = null;
        boolean sha1IsCloneable = false;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
            sha1.clone();
            sha1IsCloneable = true;
        } catch (NoSuchAlgorithmException e) {
            // Should never happen given that SHA-1 is mandated in a
            // compliant Java platform implementation.
            throw new JVMCIError(e);
        } catch (CloneNotSupportedException e) {
        }
        SHA1 = sha1;
        SHA1_IS_CLONEABLE = sha1IsCloneable;
        assert SHA1.getDigestLength() == SHA1_LENGTH;
        assert SHA1_LENGTH < MAX_LENGTH;
    }

    /**
     * Gets the final encoded byte array and closes this encoding such that any further attempts to
     * update it result in an {@link IllegalArgumentException}.
     */
    byte[] getByteArray() {
        if (result == null) {
            if (count > MAX_LENGTH) {
                try {
                    MessageDigest md = SHA1_IS_CLONEABLE ? (MessageDigest) SHA1.clone() : MessageDigest.getInstance("SHA-1");
                    md.update(buf, 0, count);
                    result = md.digest();
                } catch (CloneNotSupportedException | NoSuchAlgorithmException e) {
                    throw new InternalError(e);
                }
            } else {
                if (buf.length == count) {
                    // No need to copy the byte array
                    return buf;
                }
                result = Arrays.copyOf(buf, count);
            }
            dos = null;
        }
        return result;
    }
}
