/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.*;
import java.util.*;

public class MissingResourceCauseTest {
    public static void main(String[] args) {
        callGetBundle("NonResourceBundle", ClassCastException.class);
        callGetBundle("MalformedDataRB", Locale.ENGLISH, IllegalArgumentException.class);
        callGetBundle("PrivateConstructorRB", IllegalAccessException.class);
        callGetBundle("AbstractRB", InstantiationException.class);
        callGetBundle("BadStaticInitRB", ExceptionInInitializerError.class);
        callGetBundle("UnreadableRB", IOException.class);
        callGetBundle("NoNoArgConstructorRB", InstantiationException.class);
    }

    private static void callGetBundle(String baseName,
                                      Class<? extends Throwable> expectedCause) {
        callGetBundle(baseName, Locale.getDefault(), expectedCause);
    }

    private static void callGetBundle(String baseName, Locale locale,
                                      Class<? extends Throwable> expectedCause) {
        ResourceBundle rb;
        try {
            rb = ResourceBundle.getBundle(baseName, locale);
            throw new RuntimeException("getBundle(\""+baseName+"\") doesn't throw "
                                      + expectedCause);
        } catch (MissingResourceException e) {
            Throwable cause = e.getCause();
            if (!expectedCause.isInstance(cause)) {
                throw new RuntimeException("getBundle(\""+baseName+"\") throws "
                                           + cause + ", expected " + expectedCause);
            }
        }
    }
}
