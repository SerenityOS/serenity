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

/*
 * @test
 * @bug 4915187
 * @summary Test java.lang.String constructor that takes StringBuilder
 * @key randomness
 */
import java.util.*;

public class SBConstructor {
    private static Random rnd = new Random();
    public static void main (String[] argvs) throws Exception {
        for (int i=0; i<1000; i++) {
            int length = rnd.nextInt(20) + 1;
            StringBuffer testStringBuffer = new StringBuffer();
            StringBuilder testStringBuilder = new StringBuilder();
            for(int x=0; x<length; x++) {
                char aChar = (char)rnd.nextInt();
                testStringBuffer.append(aChar);
                testStringBuilder.append(aChar);
            }
            String testString1 = new String(testStringBuffer);
            String testString2 = new String(testStringBuilder);
            if (!testString1.equals(testString2))
                throw new RuntimeException("Test failure");
        }
    }
}
