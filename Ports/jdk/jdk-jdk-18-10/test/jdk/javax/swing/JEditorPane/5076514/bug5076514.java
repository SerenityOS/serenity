/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
   @bug 5076514 8025430 8198321
   @summary Tests if SecurityManager.checkPermission()
                  used for clipboard access with permission 'accessClipboard'
   @run main/othervm -Djava.security.manager=allow bug5076514
   @run main/othervm -Djava.security.manager=allow -Djava.awt.headless=true bug5076514
*/

import java.awt.GraphicsEnvironment;
import java.security.Permission;

import javax.swing.JEditorPane;

public class bug5076514 {
    private final static String ACCESS_CLIPBOARD = "accessClipboard";
    private static boolean isCheckPermissionCalled = false;

    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());

        // no system clipboard in the headless mode
        boolean expected  = !GraphicsEnvironment.isHeadless();

        JEditorPane editor = new JEditorPane();
        editor.copy();
        if (isCheckPermissionCalled != expected) {
            throw new RuntimeException("JEditorPane's clipboard operations "
                    + "didn't call SecurityManager.checkPermission() with "
                    + "permission 'accessClipboard' when there is a security"
                    + " manager installed");
        }
    }

    private static class MySecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            if (ACCESS_CLIPBOARD.equals(perm.getName())) {
                isCheckPermissionCalled = true;
            }
        }
    }
}
