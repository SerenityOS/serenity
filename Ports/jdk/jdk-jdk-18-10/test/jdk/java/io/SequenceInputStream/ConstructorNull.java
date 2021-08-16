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

/*
 * @test
 * @bug 4016189
 * @summary Test operation of nextStream method of SIS
 *
 */

import java.io.*;

/**
 * This class tests to see if java.io.SequenceInputStream
 * nextStream() method operates properly when some of the
 * streams it is given on input are null
 */

public class ConstructorNull {

   public static void main( String[] argv ) throws Exception {
       byte[] data = {10,20};
       int b1,b2;
       ByteArrayInputStream is = new ByteArrayInputStream(data);

       try {
           SequenceInputStream sis = new SequenceInputStream(null,is);
           int b = sis.read();
           throw new RuntimeException("No exception with null stream");
       } catch(NullPointerException e) {
           System.err.println("Test passed: NullPointerException thrown");
       }

   }
}
