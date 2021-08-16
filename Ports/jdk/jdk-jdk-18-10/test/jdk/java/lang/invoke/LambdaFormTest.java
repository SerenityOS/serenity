/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @summary unit tests for java.lang.invoke.LambdaForm
 * @modules java.base/java.lang.invoke:open
 * @run junit/othervm test.java.lang.invoke.LambdaFormTest
 */
package test.java.lang.invoke;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class LambdaFormTest {
    static final Method M_shortenSignature;
    static {
        try {
            Class<?> impl = Class.forName("java.lang.invoke.LambdaForm", false, null);
            Method m = impl.getDeclaredMethod("shortenSignature", String.class);
            m.setAccessible(true);
            M_shortenSignature = m;
        } catch(Exception e) {
            throw new AssertionError(e);
        }
    }

    public static String shortenSignature(String signature) throws ReflectiveOperationException {
        return (String)M_shortenSignature.invoke(null, signature);
    }

    @Test
    public void testShortenSignature() throws ReflectiveOperationException {
        for (String s : new String[] {
                // invariant strings:
                "L", "LL", "ILL", "LIL", "LLI", "IILL", "ILIL", "ILLI",
                // a few mappings:
                "LLL=L3", "LLLL=L4", "LLLLLLLLLL=L10",
                "IIIDDD=I3D3", "IDDD=ID3", "IIDDD=IID3", "IIID=I3D", "IIIDD=I3DD"
            }) {
            String s2 = s.substring(s.indexOf('=')+1);
            String s1 = s.equals(s2) ? s : s.substring(0, s.length() - s2.length() - 1);
            // mix the above cases with before and after reps of Z*
            for (int k = -3; k <= 3; k++) {
                String beg = (k < 0 ? "ZZZZ".substring(-k) : "");
                String end = (k > 0 ? "ZZZZ".substring(+k) : "");
                String ks1 = beg+s1+end;
                String ks2 = shortenSignature(beg)+s2+shortenSignature(end);
                String ks3 = shortenSignature(ks1);
                assertEquals(ks2, ks3);
            }
        }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        LambdaFormTest test = new LambdaFormTest();
        test.testShortenSignature();
    }
}
