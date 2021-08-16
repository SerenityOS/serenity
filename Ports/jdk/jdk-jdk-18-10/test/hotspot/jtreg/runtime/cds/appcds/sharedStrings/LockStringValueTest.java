/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.reflect.*;
import sun.hotspot.WhiteBox;

/*
 * Lock the 'value' field of a known shared string, java.lang.Object
 */
public class LockStringValueTest {
    public static void main(String args[]) {
        String s = "LiveOak";
        WhiteBox wb = WhiteBox.getWhiteBox();

        if (wb.areSharedStringsIgnored()) {
            System.out.println("The shared strings are ignored");
            System.out.println("LockStringValueTest: PASS");
            return;
        }

        if (!wb.isShared(s)) {
            throw new RuntimeException("LockStringValueTest Failed: String is not shared.");
        }

        Class c = s.getClass();
        try {
            Field f = c.getDeclaredField("value");
            f.setAccessible(true);
            Object v = f.get(s);
            lock(v);
        } catch (NoSuchFieldException nfe) {
        } catch (IllegalAccessException iae) {}
    }

    public static void lock(Object o) {
        synchronized (o) {
            System.out.println("LockStringValueTest: PASS");
        }
    }
}
