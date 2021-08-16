/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFormat;

/**
 * @test
 * @bug 4925483
 * @summary RFE: equals() should compare string in Encoding and Type
 */
public class EncodingEquals {

    public static void main(String argv[]) throws Exception {
         // first test that we can create our own encoding
         // (the constructor was made public)
         AudioFormat.Encoding myType = new AudioFormat.Encoding("PCM_SIGNED");

         // then check if this one equals this new one
         // with the static instance in AudioFormat.Encoding
         if (!myType.equals(AudioFormat.Encoding.PCM_SIGNED)) {
             throw new Exception("Encodings do not equal!");
         }
    }
}
