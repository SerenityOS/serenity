/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6312358
 * @summary Verify that an NPE is thrown by issueing Locale.getInstance() with
 *      any argument being null.
 * @modules java.base/java.util:open
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class bug6312358 {


    public static void main(String[] args) throws Exception {

        try {
            // Locale.getInstance is not directly accessible.
            Method getInstanceMethod = Locale.class.getDeclaredMethod(
                "getInstance", String.class, String.class, String.class
            );
            getInstanceMethod.setAccessible(true);

            getInstanceMethod.invoke(null, "null", "GB", "");
            try {
                getInstanceMethod.invoke(null, null, "GB", "");
                throw new RuntimeException("Should NPE with language set to null");
            } catch (InvocationTargetException exc) {
                Throwable cause = exc.getCause();
                if (!(cause instanceof NullPointerException)) {
                    throw new RuntimeException(cause+" is thrown with language set to null");
                }
            }

            getInstanceMethod.invoke(null, "en", "null", "");
            try {
                getInstanceMethod.invoke(null, "en", null, "");
                throw new RuntimeException("Should NPE with country set to null");
            } catch (InvocationTargetException exc) {
                Throwable cause = exc.getCause();
                if (!(cause instanceof NullPointerException)) {
                    throw new RuntimeException(cause+" is thrown with country set to null");
                }
            }

            getInstanceMethod.invoke(null, "en", "GB", "null");
            try {
                getInstanceMethod.invoke(null, "en", "GB", null);
                throw new RuntimeException("Should NPE with variant set to null");
            } catch (InvocationTargetException exc) {
                Throwable cause = exc.getCause();
                if (!(cause instanceof NullPointerException)) {
                    throw new RuntimeException(cause+" is thrown with variant set to null");
                }
            }
        } catch (java.lang.NoSuchMethodException exc) {
            // method is not found.  consider it as a success
        }
    }
}
