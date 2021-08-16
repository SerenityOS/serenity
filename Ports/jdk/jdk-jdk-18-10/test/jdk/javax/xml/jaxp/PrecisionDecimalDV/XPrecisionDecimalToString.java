/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;

/**
 * @test
 * @bug 8013900
 * @summary More warning compiling jaxp.
 *   This test only test one of the methods used to implement hashCode()
 *   in com.sun.org.apache.xerces.internal.impl.dv.xs.PrecisionDecimalDV$XPrecisionDecimal.
 *   Since that method is private the test unfortunately needs to use reflection
 *   to invoke the method.
 * @modules java.xml/com.sun.org.apache.xerces.internal.impl.dv.xs:open
 * @run main XPrecisionDecimalToString
 * @author Daniel Fuchs
 */
public class XPrecisionDecimalToString {

    private static final String className =
            "com.sun.org.apache.xerces.internal.impl.dv.xs.PrecisionDecimalDV$XPrecisionDecimal";
    private static final String methodName = "canonicalToStringForHashCode";
    private static final Class<?>[] signature = { String.class, String.class, int.class, int.class };
    private static Method method;

    // Invokes XPrecisionDecimal.canonicalToStringForHashCode through reflection,
    // because the method is private...
    //
    // Construct a canonical String representation of this number
    // for the purpose of deriving a hashCode value compliant with
    // equals.
    // The toString representation will be:
    // NaN for NaN, INF for +infinity, -INF for -infinity, 0 for zero,
    // and [1-9]\.[0-9]*[1-9]?(E[1-9][0-9]*)? for other numbers.
    private static String canonicalToStringForHashCode(String ivalue, String fvalue, int sign, int pvalue) {
        try {
            if (method == null) {
                Class<?> type = Class.forName(className);
                method = type.getDeclaredMethod(methodName, signature);
                method.setAccessible(true);
            }
        } catch (Exception x) {
            throw new Error("Impossible to find '"+className+"."+methodName+"': "+ x, x);
        }
        try {
            return (String) method.invoke(null, new Object[] {ivalue, fvalue, sign, pvalue} );
        } catch(Exception x) {
            throw new Error("Failed to invoke "+className+"."+methodName+"(\""+
                    ivalue+"\", \""+fvalue+"\", "+sign+", "+pvalue+"): " +x, x);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        test("123","7890",-1,0,"-1.23789E2");
        test("0","007890",-1,0,"-7.89E-3");
        test("123","7890",1,0,"1.23789E2");
        test("0","007890",1,0,"7.89E-3");
        test("123","7890",1,10,"1.23789E12");
        test("0","007890",1,33,"7.89E30");
        test("INF","",1,0,"INF");
        test("INF","",-1,0,"-INF");
        test("NaN","",0,0,"NaN");
        test("0","",1,0,"0");
        test("00000","00000",1,10,"0");
        test("00000","00000",-1,10,"0");
        test("00000","000001",-1,-10,"-1E-16");
    }

    private static void test(String ival, String fval, int sign, int pvalue, String expected) {
        final String canonical = canonicalToStringForHashCode(ival, fval, sign, pvalue);
        System.out.println((sign == -1 ? "-" : "") + ival +
                ("INF".equals(ival) || "NaN".equals(ival) ? ""
                 : ( "." + fval + "E" + pvalue))
                + " => "+ canonical);
        if (!expected.equals(canonical)) {
            throw new Error("expected: "+expected+" got: "+ canonical);
        }
    }


}
