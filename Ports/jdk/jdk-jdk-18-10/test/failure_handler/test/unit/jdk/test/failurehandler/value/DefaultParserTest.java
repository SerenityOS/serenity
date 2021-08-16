/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler.value;

import org.junit.Assert;
import org.junit.Test;

public class DefaultParserTest {
    @Test
    public void testParseStringArray() throws Exception {
        DefaultParser parser = new DefaultParser();
        String line = "a aa   aaa";
        String[] result = {"a", "aa", "", "", "aaa"};
        Assert.assertArrayEquals(result,
                (Object[]) parser.parse(result.getClass(), line, " "));

        line = null;
        result = new String[]{};
        Assert.assertArrayEquals(result,
                (Object[]) parser.parse(result.getClass(), line, " "));
    }

    @Test
    public void testParseObjectArray() throws Exception {
        DefaultParser parser = new DefaultParser();
        String line = "a aa   aaa";
        String[] result = {"a", "aa", "", "", "aaa"};
        Assert.assertArrayEquals(result,
                (String[]) parser.parse(result.getClass(), line, " "));
        Object[] result2 = {"a", "aa", "", "", "aaa"};
        Assert.assertArrayEquals(result2,
                (Object[]) parser.parse(result.getClass(), line, " "));
    }

    @Test
    public void testParseCharArray() throws Exception {
        DefaultParser parser = new DefaultParser();
        String line = "a b c a";
        char[] result = {'a', 'b', 'c', 'a'};
        Assert.assertArrayEquals(result,
                (char[]) parser.parse(result.getClass(), line, " "));

        Character[] result2 = {'a', 'b', 'c', 'a'};
        Assert.assertArrayEquals(result2,
                (Character[]) parser.parse(result2.getClass(), line, " "));
    }

    @Test
    public void testParseBoolean() throws Exception {
        DefaultParser parser = new DefaultParser();
        String line = "a b c a";
        Assert.assertEquals(false,
                (boolean) parser.parse(boolean.class, line, " "));
        Assert.assertEquals(Boolean.FALSE,
                parser.parse(Boolean.class, line, " "));
        line = "trUe";
        Assert.assertEquals(true,
                (boolean) parser.parse(boolean.class, line, " "));
        Assert.assertEquals(Boolean.TRUE,
                parser.parse(Boolean.class, line, " "));
    }

    @Test
    public void testParseShort() throws Exception {
        DefaultParser parser = new DefaultParser();
        Assert.assertSame("10", (short) 10,
                parser.parse(short.class, "10", " "));
        Assert.assertSame("010", (short) 8,
                parser.parse(short.class, "010", " "));
        Assert.assertSame("0x10", (short) 16,
                parser.parse(short.class, "0x10", " "));
    }

    @Test
    public void testParseByte() throws Exception {
        DefaultParser parser = new DefaultParser();
        Assert.assertSame("11", (byte) 11,
                parser.parse(byte.class, "11", " "));
        Assert.assertSame("011", (byte) 9,
                parser.parse(byte.class, "011", " "));
        Assert.assertSame("0x11", (byte) 17,
                parser.parse(byte.class, "0x11", " "));
    }

    @Test
    public void testParseInt() throws Exception {
        DefaultParser parser = new DefaultParser();
        Assert.assertEquals("20", (int) 20,
                parser.parse(int.class, "20", " "));
        Assert.assertEquals("020", (int) 16,
                parser.parse(int.class, "020", " "));
        Assert.assertEquals("0x20", (int) 32,
                parser.parse(int.class, "0x20", " "));
    }


}
