/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static jdk.test.lib.util.SerializationUtils.*;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8077559
 * @library /test/lib
 * @build jdk.test.lib.util.SerializationUtils
 * @summary Tests Compact String. This one is testing StringBuilder serialization
 *          among -XX:+CompactStrings/-XX:-CompactStrings/LegacyStringBuilder
 * @run testng/othervm -XX:+CompactStrings CompactStringBuilderSerialization
 * @run testng/othervm -XX:-CompactStrings CompactStringBuilderSerialization
 */

public class CompactStringBuilderSerialization {
    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                // every byte array is serialized from corresponding StringBuffer object
                // by previous JDK(build 1.8.0_45-b14).
                new Object[] {
                        new StringBuilder(""),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 0, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("A"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 1, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 17, 0, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("AB"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 2, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 18, 0, 65, 0, 66, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("abcdefghijk"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 11, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 27, 0, 97, 0, 98, 0, 99, 0, 100, 0, 101, 0, 102, 0, 103, 0, 104, 0,
                                105, 0, 106, 0, 107, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("\uff21"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 1, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 17, -1, 33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("\uff21\uff22"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 2, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 18, -1, 33, -1, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("\uff21A\uff21A\uff21A\uff21A\uff21A"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 10, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 26, -1, 33, 0, 65, -1, 33, 0, 65, -1, 33, 0, 65, -1, 33, 0, 65, -1,
                                33, 0, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("A\uff21B\uff22C\uff23D\uff24E\uff25F\uff26G\uff27H\uff28"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 16, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 32, 0, 65, -1, 33, 0, 66, -1, 34, 0, 67, -1, 35, 0, 68, -1, 36, 0,
                                69, -1, 37, 0, 70, -1, 38, 0, 71, -1, 39, 0, 72, -1, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("\uff21A\uff22B\uff23C\uff24D\uff25E\uff26F\uff27G\uff28H"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 16, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 32, -1, 33, 0, 65, -1, 34, 0, 66, -1, 35, 0, 67, -1, 36, 0, 68, -1,
                                37, 0, 69, -1, 38, 0, 70, -1, 39, 0, 71, -1, 40, 0, 72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("\ud801\udc00\ud801\udc01\uff21A"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 6, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 22, -40, 1, -36, 0, -40, 1, -36, 1, -1, 33, 0, 65, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } },
                new Object[] {
                        new StringBuilder("\uff21\uff22\uff21\uff22\uff21\uff22\uff21\uff22\uff21\uff22"),
                        new byte[] { -84, -19, 0, 5, 115, 114, 0, 23, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103, 66, 117, 105,
                                108, 100, 101, 114, 60, -43, -5, 20, 90, 76, 106, -53, 3, 0, 0, 120, 112, 119, 4, 0, 0, 0, 10, 117, 114, 0, 2, 91, 67, -80, 38,
                                102, -80, -30, 93, -124, -84, 2, 0, 0, 120, 112, 0, 0, 0, 26, -1, 33, -1, 34, -1, 33, -1, 34, -1, 33, -1, 34, -1, 33, -1, 34,
                                -1, 33, -1, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 120 } } };
    }

    /*
     * Verify serialization works between Compact StringBuilder/Legacy StringBuilder
     */
    @Test(dataProvider = "provider")
    public void test(StringBuilder sbContent, byte[] baInJDK8) throws Exception {
        // Serialize a StringBuilder object into byte array.
        byte[] ba = serialize(sbContent);
        assertEquals(ba, baInJDK8);
        // Deserialize a StringBuilder object from byte array which is generated by previous JDK(build 1.8.0_45-b14).
        Object obj = deserialize(ba);
        assertEquals(obj.getClass(), StringBuilder.class);
        assertTrue(equals((StringBuilder)obj, sbContent));
    }

    boolean equals(StringBuilder sb, StringBuilder expected) {
        if(sb.length() == expected.length()
                && sb.capacity() == expected.capacity()
                && sb.toString().equals(expected.toString())) {
            return true;
        }
        return false;
    }
}
