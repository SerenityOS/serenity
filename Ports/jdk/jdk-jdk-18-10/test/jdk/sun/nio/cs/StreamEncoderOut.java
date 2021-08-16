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

/* @test
   @bug 8030179
   @summary test if the charset encoder deails with surrogate correctly
 * @run testng/othervm -esa StreamEncoderOut
 */
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Test
public class StreamEncoderOut {

    enum Input {
        HIGH("\ud834"),
        LOW("\udd1e"),
        HIGH_LOW("\ud834\udd1e");

        final String value;

        Input(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return name() + " : \'" + value + "\"";
        }
    }

    @DataProvider(name = "CharsetAndString")
    // [Charset, Input]
    public static Object[][] makeStreamTestData() {
        // Cross product of supported charsets and inputs
        return Charset.availableCharsets().values().stream().
                filter(Charset::canEncode).
                flatMap(cs -> Stream.of(Input.values()).map(i -> new Object[]{cs, i})).
                toArray(Object[][]::new);
    }

    private static String generate(String s, int n) {
        return Stream.generate(() -> s).limit(n).collect(joining());
    }

    static final OutputStream DEV_NULL = new OutputStream() {
        @Override
        public void write(byte b[], int off, int len) throws IOException {}

        @Override
        public void write(int b) throws IOException {}
    };

    @Test(dataProvider = "CharsetAndString")
    public void test(Charset cs, Input input) throws IOException {
        OutputStreamWriter w = new OutputStreamWriter(DEV_NULL, cs);
        String t = generate(input.value, 8193);
        for (int i = 0; i < 10; i++) {
            w.append(t);
        }
    }
}
