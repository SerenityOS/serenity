/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4134311 8247918
 * @summary Test if skip works correctly
 * @run testng Skip
*/

import java.io.CharArrayReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PushbackReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class Skip {
    private static String FILENAME =
        System.getProperty("test.src", ".") + File.separator + "SkipInput.txt";
    private static File file = new File(FILENAME);

    @Test
    public void skip() throws IOException {
        try (FileReader fr = new FileReader(file)) {
            long nchars = 8200;
            long actual = fr.skip(nchars);

            Assert.assertFalse(actual > nchars,
                "Should skip " + nchars + ", but skipped " +actual+" chars");
        }
    }

    @DataProvider(name = "readers")
    public Object[][] getReaders() throws IOException {
        return new Object[][] {
            {new LineNumberReader(new FileReader(file))},
            {new CharArrayReader(new char[] {27})},
            {new PushbackReader(new FileReader(file))},
            {new FileReader(file)},
            {new StringReader(new String(new byte[] {(byte)42}))}
        };
    }

    @Test(dataProvider = "readers")
    public void eof(Reader r) throws IOException {
         r.skip(Long.MAX_VALUE);
         Assert.assertEquals(r.skip(1), 0);
         Assert.assertEquals(r.read(), -1);
    }

    @DataProvider(name = "skipIAE")
    public Object[][] getSkipIAEs() throws IOException {
        return new Object[][] {
            {new LineNumberReader(new FileReader(file))},
            {new PushbackReader(new FileReader(file))},
            {new FileReader(file)}
        };
    }

    @Test(dataProvider = "skipIAE", expectedExceptions = IllegalArgumentException.class)
    public void testThrowsIAE(Reader r) throws IOException {
        r.skip(-1);
    }

    @DataProvider(name = "skipNoIAE")
    public Object[][] getSkipNoIAEs() throws IOException {
        return new Object[][] {
            {new CharArrayReader(new char[] {27})},
            {new StringReader(new String(new byte[] {(byte)42}))}
        };
    }

    @Test(dataProvider = "skipNoIAE")
    public void testNoIAE(Reader r) throws IOException {
        r.skip(-1);
    }
}
