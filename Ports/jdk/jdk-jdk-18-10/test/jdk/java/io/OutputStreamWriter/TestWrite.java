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
 * @bug 4066847 4147276 4131647
 * @summary Check for flush of output buffer before concluding it is too small
 */

import java.io.*;

public class TestWrite {

    public static void main(String args[])
        throws Exception
    {
        ByteArrayOutputStream bos;
        OutputStreamWriter osw;
        byte[] array;

        try{
            bos = new ByteArrayOutputStream();
            osw = new OutputStreamWriter(bos, "EUCJIS");
            osw.write('a');
            for(int count = 0; count < 10000; ++count)
                osw.write('\u3042');   // Hiragana
            osw.close();
            array = bos.toByteArray();
        } catch (UnsupportedEncodingException e){
            System.err.println("Unsupported encoding - EUCJIS. ext "
                               + " may not be properly installed. ext is  "
                               + " required for the test to run properly ");
            throw new Exception("Environment is incorrect");
        }
    }
}
