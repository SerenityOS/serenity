/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4015701 4127654
   @summary Test if the constructor would detect
            illegal arguments.
*/

import java.io.*;

public class NegativeInitSize {
    public static void main(String[] args) throws Exception {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(-1);
        } catch (IllegalArgumentException e) {
        } catch (Exception e){
            System.out.println(e.getMessage());
            throw new Exception
                ("ByteArrayOutputStream failed to detect negative init size");
        }
        CharArrayReader CAR = new CharArrayReader("test".toCharArray());
        try {
            PushbackReader pbr = new PushbackReader(CAR, -1);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            System.out.println(e.getClass().getName());
            throw new Exception
                ("PushbackReader failed to detect negative init size");
        }

        try {
            PushbackInputStream pbis = new PushbackInputStream(null, -1);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            throw new Exception
                ("PushbackInputStream failed to detect negative init size");
        }

        ByteArrayOutputStream goodbos = new ByteArrayOutputStream();
        try {
            BufferedOutputStream bos = new BufferedOutputStream(goodbos, -1);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            throw new Exception
                ("BufferedOutputStream failed to detect negative init size");
        }

        byte[] ba = { 123 };
        ByteArrayInputStream goodbis = new ByteArrayInputStream(ba);
        try {
            BufferedInputStream bis = new BufferedInputStream(goodbis, -1);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            throw new Exception
                ("BufferedInputStream failed to detect negative init size");
        }

        try {
            CharArrayWriter caw = new CharArrayWriter(-1);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            throw new Exception
                ("CharArrayWriter failed to detect negative init size");
        }
    }
}
