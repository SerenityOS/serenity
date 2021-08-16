/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6795356
 * @summary Leak caused by javax.swing.UIDefaults.ProxyLazyValue.acc
 * @author Alexander Potochkin
 * @library ../../regtesthelpers
 * @build Util
 * @run main/othervm -mx128m bug6795356
 */

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlContext;
import java.util.LinkedList;
import java.util.List;
import javax.swing.*;

public class bug6795356 {
    volatile static WeakReference<ProtectionDomain> weakRef;

    public static void main(String[] args) throws Exception {

        ProtectionDomain domain = new ProtectionDomain(null, null);

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {

                // this initialize ProxyLazyValues
                UIManager.getLookAndFeel();

                return null;
            }
        }, new AccessControlContext(new ProtectionDomain[]{domain}));

        weakRef = new WeakReference<ProtectionDomain>(domain);
        domain = null;

        Util.generateOOME();

        if (weakRef.get() != null) {
            throw new RuntimeException("Memory leak found!");
        }
        System.out.println("Test passed");
    }
}
