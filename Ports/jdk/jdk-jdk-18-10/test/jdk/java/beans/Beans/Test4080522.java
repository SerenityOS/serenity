/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4080522
 * @summary Tests security checks for calls on:
 *          Beans.setDesignTime
 *          Beans.setGuiAvailable
 *          Introspector.setBeanInfoSearchPath
 *          PropertyEditorManager.setEditorSearchPath
 * @run main/othervm -Djava.security.manager=allow Test4080522
 * @author Graham Hamilton
 */

import java.beans.Introspector;
import java.beans.Beans;
import java.beans.PropertyEditorManager;

public class Test4080522 {
    public static void main(String[] args) {
        OurSecurityManager sm = new OurSecurityManager();
        String[] path = {"a", "b"};
        // with no security manager we shuld be able to do these calls OK
        test(path);
        // add our own security manager
        System.setSecurityManager(sm);
        // now each of the calls should raise an exception
        try {
            Beans.setDesignTime(true);
            throw new Error("Beans.setDesignTime should throw SecurityException");
        } catch (SecurityException exception) {
            // expected exception
        }
        try {
            Beans.setGuiAvailable(true);
            throw new Error("Beans.setGuiAvailable should throw SecurityException");
        } catch (SecurityException exception) {
            // expected exception
        }
        try {
            Introspector.setBeanInfoSearchPath(path);
            throw new Error("Introspector.setBeanInfoSearchPath should throw SecurityException");
        } catch (SecurityException exception) {
            // expected exception
        }
        try {
            PropertyEditorManager.setEditorSearchPath(path);
            throw new Error("PropertyEditorManager.setEditorSearchPath should throw SecurityException");
        } catch (SecurityException exception) {
            // expected exception
        }
        // now set the security manager to be friendly
        sm.friendly = true;
        // now the calls should be OK again.
        test(path);
    }

    private static void test(String[] path) {
        try {
            Beans.setDesignTime(true);
            Beans.setGuiAvailable(true);
            Introspector.setBeanInfoSearchPath(path);
            PropertyEditorManager.setEditorSearchPath(path);
        } catch (SecurityException exception) {
            throw new Error("unexpected security exception", exception);
        }
    }

    private static class OurSecurityManager extends SecurityManager {
        boolean friendly;

        public void checkPropertiesAccess() {
            if (!friendly) {
                throw new SecurityException("No way");
            }
        }
    }
}
