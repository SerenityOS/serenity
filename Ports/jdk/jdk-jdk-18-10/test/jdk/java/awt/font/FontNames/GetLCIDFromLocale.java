/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4351212
 * @modules java.desktop/sun.font:open
 * @summary Verify that new getLCIDFromLocale method works
 */

import java.lang.reflect.Method;
import java.util.Locale;

public class GetLCIDFromLocale {

     static Method getLCIDMethod = null;
     public static void main(String args[]) {
        try {
            Class ttClass = Class.forName("sun.font.TrueTypeFont");
            getLCIDMethod =
                ttClass.getDeclaredMethod("getLCIDFromLocale",
                                           java.util.Locale.class);
            getLCIDMethod.setAccessible(true); // its private
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Reflection failed");
        }
        if (getLCIDMethod == null) {
            throw new RuntimeException("No method found");
        }

        test(Locale.US, 0x0409);
        test(Locale.GERMAN, 0x0407);
        test(Locale.GERMANY, 0x0407);
        test(new Locale("de", "AT"), 0x0c07);
        test(new Locale("ar"), 0x0401);
        test(new Locale("ar", "SA"), 0x0401);
        test(new Locale("ar", "EG"), 0x0c01);
        test(new Locale("??"), 0x0409);
        test(new Locale("??", "??"), 0x0409);
        test(Locale.KOREA, 0x0412);
    }

    private static void test(Locale locale, int expectedLCID) {
        try {
            short lcid = (Short)getLCIDMethod.invoke(null, locale);
            System.out.println("lcid="+lcid+" expected="+expectedLCID);
            if (lcid != expectedLCID) {
                 throw new RuntimeException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Method invocation exception");
        }
    }
}
