/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8008981
 * @summary Test that selected Toolkit and Window methods/constructors do
 *   the appropriate permission check
 * @run main/othervm -Djava.security.manager=allow Permissions
 */

import java.awt.AWTPermission;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.security.Permission;

public class Permissions {

    static class MySecurityManager extends SecurityManager {
        private List<Permission> permissionsChecked = new ArrayList<>();

        static MySecurityManager install() {
            MySecurityManager sm = new MySecurityManager();
            System.setSecurityManager(sm);
            return sm;
        }

        @Override
        public void checkPermission(Permission perm) {
            permissionsChecked.add(perm);
        }

        void prepare(String msg) {
            System.out.println(msg);
            permissionsChecked.clear();
        }

        /**
         * Checks the security manager's checkPermission method was invoked
         * to check the given permission and target name.
         */
        void assertChecked(Class<? extends Permission> type, String name) {
            for (Permission perm: permissionsChecked) {
                if (type.isInstance(perm) && perm.getName().equals(name))
                    return;
            }
            throw new RuntimeException(type.getName() + "(\"" + name + "\") not checked");
        }
    }

    public static void main(String[] args) {
        MySecurityManager sm = MySecurityManager.install();

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        sm.prepare("Toolkit.getSystemClipboard()");
        toolkit.getSystemClipboard();
        sm.assertChecked(AWTPermission.class, "accessClipboard");

        sm.prepare("Toolkit.getSystemEventQueue()");
        toolkit.getSystemEventQueue();
        sm.assertChecked(AWTPermission.class, "accessEventQueue");

        sm.prepare("Toolkit.getSystemSelection()");
        toolkit.getSystemSelection();
        //sm.assertChecked(AWTPermission.class, "accessClipboard");

        sm.prepare("Window(Frame)");
        new Window((Frame)null);
        sm.assertChecked(AWTPermission.class, "showWindowWithoutWarningBanner");

        sm.prepare("Window(Window)");
        new Window((Window)null);
        sm.assertChecked(AWTPermission.class, "showWindowWithoutWarningBanner");

        sm.prepare("Window(Window,GraphicsConfiguration)");
        new Window((Window)null, (GraphicsConfiguration)null);
        sm.assertChecked(AWTPermission.class, "showWindowWithoutWarningBanner");
    }
}
