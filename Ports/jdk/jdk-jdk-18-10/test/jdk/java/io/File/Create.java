/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4812991 4804456
 * @summary Test creation of new files with long names
 */

import java.io.*;

public class Create {

    static final int length = 512;

    public static void main(String[] args) throws Exception {
        String fileName = createFileName(length);
        File file = new File(fileName);
        try {
            boolean result = file.createNewFile();
            if (result) {
                if (!file.exists())
                    throw new RuntimeException("Result is incorrect");
            } else {
                if (file.exists())
                    throw new RuntimeException("Result is incorrect");
            }
        } catch (IOException ioe) {
            // Correct result on some platforms
        }
    }

    public static String createFileName(int length){
        char[] array = new char[length];
        for(int i = 0 ; i < length ; i++)
            array[i] = (char)('0' + (i % 10));
        return new String(array);
    }
}
