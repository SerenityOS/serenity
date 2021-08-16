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
   @bug 4111750 4114745
   @summary test if Deflater/Inflater constructor will
            check the arguments correctly.
   */
import java.util.zip.*;
import java.io.*;

public class StreamConstructor {

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Deflater def = new Deflater();
        ByteArrayInputStream bis = new ByteArrayInputStream(new byte[10]);
        Inflater inf = new Inflater();
        InflaterInputStream infOS;
        DeflaterOutputStream defOS;

        try {
            defOS = new DeflaterOutputStream(bos, null);
            throw new Exception("didn't catch illegal argument");
        } catch (NullPointerException e){
        }

        try {
            defOS = new DeflaterOutputStream(null, def);
            throw new Exception("didn't catch illegal argument");
        } catch (NullPointerException e){
        }

        try {
            defOS = new DeflaterOutputStream(bos, def, -1);
            throw new Exception("didn't catch illegal argument");
        } catch (IllegalArgumentException e) {
        }


        try {
            infOS = new InflaterInputStream(bis, null);
            throw new Exception("didn't catch illegal argument");
        } catch (NullPointerException e){
        }

        try {
            infOS = new InflaterInputStream(null, inf);
            throw new Exception("didn't catch illegal argument");
        } catch (NullPointerException e){
        }

        try {
            infOS = new InflaterInputStream(bis, inf, -1);
            throw new Exception("didn't catch illegal argument");
        } catch (IllegalArgumentException e) {
        }
    }
}
