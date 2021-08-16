/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.generatecharacter;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public class CharacterName {

    public static void main(String[] args) {
        FileReader reader = null;
        try {
            if (args.length != 2) {
                System.err.println("Usage: java CharacterName UnicodeData.txt uniName.dat");
                System.exit(1);
            }
            reader = new FileReader(args[0]);
            BufferedReader bfr = new BufferedReader(reader);
            String line = null;

            StringBuilder namePool = new StringBuilder();
            byte[] cpPoolBytes = new byte[0x100000];
            boolean[] cpBlocks = new boolean[(Character.MAX_CODE_POINT + 1) >> 8];
            int bkNum = 0;
            ByteBuffer cpBB = ByteBuffer.wrap(cpPoolBytes);
            int lastCp = 0;
            int cpNum = 0;

            while ((line = bfr.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                UnicodeSpec spec = UnicodeSpec.parse(line);
                if (spec != null) {
                    int cp = spec.getCodePoint();
                    String name = spec.getName();
                    if (name.equals("<control>") && spec.getOldName() != null) {
                        if (cp == 0x7)  // <control>BELL -> BEL; u+1f514 <-> BELL
                            name = "BEL";
                        else if (spec.getOldName().length() != 0)
                            name = spec.getOldName();
                        /*
                           3 "figment" characters from NameAliases.txt
                           Several documented labels for C1 control code points which
                           were never actually approved in any standard...but were
                           implemented in Perl regex.
                           0080;PADDING CHARACTER;figment
                           0081;HIGH OCTET PRESET;figment
                           0099;SINGLE GRAPHIC CHARACTER INTRODUCER;figment
                        */
                        else if (cp == 0x80)
                            name = "PADDING CHARACTER";
                        else if (cp == 0x81)
                            name = "HIGH OCTET PRESET";
                        else if (cp == 0x99)
                            name = "SINGLE GRAPHIC CHARACTER INTRODUCER";
                        else
                            continue;
                    } else if (name.startsWith("<")) {
                        /*
                          3400    <CJK Ideograph Extension A, First>
                          4db5    <CJK Ideograph Extension A, Last>
                          4e00    <CJK Ideograph, First>
                          9fc3    <CJK Ideograph, Last>
                          ac00    <Hangul Syllable, First>
                          d7a3    <Hangul Syllable, Last>
                          d800    <Non Private Use High Surrogate, First>
                          db7f    <Non Private Use High Surrogate, Last>
                          db80    <Private Use High Surrogate, First>
                          dbff    <Private Use High Surrogate, Last>
                          dc00    <Low Surrogate, First>
                          dfff    <Low Surrogate, Last>
                          e000    <Private Use, First>
                          f8ff    <Private Use, Last>
                         20000    <CJK Ideograph Extension B, First>
                         2a6d6    <CJK Ideograph Extension B, Last>
                         f0000    <Plane 15 Private Use, First>
                         ffffd    <Plane 15 Private Use, Last>
                        */
                        continue;
                    }
                    cpNum++;
                    if (!cpBlocks[cp >> 8]) {
                        cpBlocks[cp >> 8] = true;
                        bkNum++;
                    }
                    if (cp == lastCp + 1) {
                        cpBB.put((byte)name.length());
                    } else {
                        cpBB.put((byte)0);  // segment start flag
                        cpBB.putInt((name.length() << 24) | (cp & 0xffffff));
                    }
                    namePool.append(name);
                    lastCp = cp;
                }
            }

            byte[] namePoolBytes = namePool.toString().getBytes("ASCII");
            int cpLen = cpBB.position();
            int total = cpLen + namePoolBytes.length;
            DataOutputStream dos = new DataOutputStream(
                                       new DeflaterOutputStream(
                                           new FileOutputStream(args[1])));
            dos.writeInt(total);  // total
            dos.writeInt(bkNum);  // bkNum;
            dos.writeInt(cpNum);  // cpNum
            dos.writeInt(cpLen);  // nameOff
            dos.write(cpPoolBytes, 0, cpLen);
            dos.write(namePoolBytes);
            dos.close();

        } catch (Throwable e) {
            System.out.println("Unexpected exception:");
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ee) { ee.printStackTrace(); }
            }
        }
    }
}
