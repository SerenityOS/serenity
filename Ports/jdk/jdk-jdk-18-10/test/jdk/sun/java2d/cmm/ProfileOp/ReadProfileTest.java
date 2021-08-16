/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6476665 6523403
 * @summary Verifies reading profiles of the standard color spaces
 * @run main ReadProfileTest
 */

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.*;
import java.nio.*;
import java.util.Hashtable;

public class ReadProfileTest implements Runnable {
    /* Location of the tag sig counter in 4-byte words */
    final static int TAG_COUNT_OFFSET = 32;

    /* Location of the tag sig table in 4-byte words */
    final static int TAG_ELEM_OFFSET = 33;

    static byte[][] profiles;
    static int [][] tagSigs;
    static Hashtable [] tags;
    boolean status;

    static int [] cspaces = {ColorSpace.CS_sRGB, ColorSpace.CS_PYCC,
                             ColorSpace.CS_LINEAR_RGB, ColorSpace.CS_CIEXYZ,
                             ColorSpace.CS_GRAY};

    static String [] csNames = {"sRGB", "PYCC", "LINEAR_RGB", "CIEXYZ", "GRAY"};

    static void getProfileTags(byte [] data, Hashtable tags) {
        ByteBuffer byteBuf = ByteBuffer.wrap(data);
        IntBuffer intBuf = byteBuf.asIntBuffer();
        int tagCount = intBuf.get(TAG_COUNT_OFFSET);
        intBuf.position(TAG_ELEM_OFFSET);
        for (int i = 0; i < tagCount; i++) {
            int tagSig = intBuf.get();
            int tagDataOff = intBuf.get();
            int tagSize = intBuf.get();

            byte [] tagData = new byte[tagSize];
            byteBuf.position(tagDataOff);
            byteBuf.get(tagData);
            tags.put(tagSig, tagData);
        }
    }

    static {
        profiles = new byte[cspaces.length][];
        tags = new Hashtable[cspaces.length];

        for (int i = 0; i < cspaces.length; i++) {
            ICC_Profile pf = ICC_Profile.getInstance(cspaces[i]);
            profiles[i] = pf.getData();
            tags[i] = new Hashtable();
            getProfileTags(profiles[i], tags[i]);
        }
    }

    public ReadProfileTest() {
        status = true;
    }

    public void run() {
        for (int i = 0; i < cspaces.length; i++) {
            ICC_Profile pf = ICC_Profile.getInstance(cspaces[i]);
            byte [] data = pf.getData();
            if (!Arrays.equals(data, profiles[i])) {
                status = false;
                System.err.println("Incorrect result of getData() " + "with " +
                                   csNames[i] + " profile");
                throw new RuntimeException("Incorrect result of getData()");
            }

            Iterator<Integer> iter = tags[i].keySet().iterator();
            while(iter.hasNext()) {
                int tagSig = iter.next();
                byte [] tagData = pf.getData(tagSig);
                if (!Arrays.equals(tagData,
                                   (byte[]) tags[i].get(tagSig)))
                {
                    status = false;
                    System.err.println("Incorrect result of getData(int) with" +
                                       " tag " +
                                       Integer.toHexString(tagSig) +
                                       " of " + csNames[i] + " profile");

                    throw new RuntimeException("Incorrect result of " +
                                               "getData(int)");
                }
            }
        }
    }

    public boolean getStatus() {
        return status;
    }

    public static void main(String [] args) {
        ReadProfileTest test = new ReadProfileTest();
        test.run();
    }
}
