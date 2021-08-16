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

/*
 * @test
 * @bug 8001029
 * @summary Make sure that TimeZone.setDefault throws a SecurityException if the
 *          security manager doesn't permit.
 * @run main/othervm -Djava.security.manager=allow SetDefaultSecurityTest
 */

import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class SetDefaultSecurityTest {
    static final TimeZone NOWHERE = new SimpleTimeZone(Integer.MAX_VALUE, "Nowhere");

    public static void main(String[] args)   {
        TimeZone defaultZone = TimeZone.getDefault();

        // Make sure that TimeZone.setDefault works for trusted code
        TimeZone.setDefault(NOWHERE);
        if (!NOWHERE.equals(TimeZone.getDefault())) {
            new RuntimeException("TimeZone.setDefault doesn't work for trusted code.");
        }
        // Restore defaultZone
        TimeZone.setDefault(defaultZone);
        if (!defaultZone.equals(TimeZone.getDefault())) {
            new RuntimeException("TimeZone.setDefault doesn't restore defaultZone.");
        }

        // Install a SecurityManager.
        System.setSecurityManager(new SecurityManager());
        try {
            TimeZone.setDefault(NOWHERE);
            throw new RuntimeException("TimeZone.setDefault doesn't throw a SecurityException.");
        } catch (SecurityException se) {
            // OK
        }
        TimeZone tz = TimeZone.getDefault();
        if (!defaultZone.equals(tz)) {
            throw new RuntimeException("Default TimeZone changed: " + tz);
        }
    }
}
