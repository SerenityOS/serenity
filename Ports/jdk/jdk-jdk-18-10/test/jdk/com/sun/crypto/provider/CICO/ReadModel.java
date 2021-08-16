/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 * ReadModel provides different way to test
 * CipherInputStream.read()/read(byte[])/read(byte[], int, int) and
 * CipherOutputStream.write(int)/write(byte[], int, int)/read(byte[]) API
 */
enum ReadModel {
    READ_BYTE {
        @Override
        public void read(CipherInputStream cInput, CipherOutputStream ciOutput,
                Cipher ciIn, int inputLen) throws IOException {
            int buffer0 = cInput.read();
            while (buffer0 != -1) {
                ciOutput.write(buffer0);
                buffer0 = cInput.read();
            }
        }
    },
    READ_BUFFER {
        @Override
        public void read(CipherInputStream cInput, CipherOutputStream ciOutput,
                Cipher ciIn, int inputLen) throws IOException {
            byte[] buffer1 = new byte[20];
            int len1;
            while ((len1 = cInput.read(buffer1)) != -1) {
                ciOutput.write(buffer1, 0, len1);
            }

        }
    },
    READ_BUFFER_OFFSET {
        @Override
        public void read(CipherInputStream cInput, CipherOutputStream ciOutput,
                Cipher ciIn, int inputLen) throws IOException {
            byte[] buffer2 = new byte[ciIn.getOutputSize(inputLen)];
            int offset2 = 0;
            int len2 = 0;
            while (len2 != -1) {
                len2 = cInput.read(buffer2, offset2, buffer2.length - offset2);
                offset2 += len2;
            }
            ciOutput.write(buffer2);

        }
    };

    abstract public void read(CipherInputStream cInput,
            CipherOutputStream ciOutput, Cipher ciIn, int inputLen)
            throws IOException;
}
