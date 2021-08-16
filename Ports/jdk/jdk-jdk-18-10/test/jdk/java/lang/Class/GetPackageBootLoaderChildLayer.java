/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @requires !vm.graal.enabled
 * @modules jdk.attach
 * @run main/othervm --limit-modules jdk.attach -Djdk.attach.allowAttachSelf
 *    GetPackageBootLoaderChildLayer
 * @summary Exercise Class.getPackage on a class defined to the boot loader
 *    but in a module that is in a child layer rather than the boot layer
 */

import com.sun.tools.attach.VirtualMachine;

public class GetPackageBootLoaderChildLayer {
    public static void main(String[] args) throws Exception {
        // ensure that the java.management module is not in the boot layer
        ModuleLayer.boot().findModule("java.management").ifPresent(m -> {
            throw new RuntimeException("java.management loaded!!!");
        });

        // start local JMX agent via the attach mechanism
        String vmid = "" + ProcessHandle.current().pid();
        VirtualMachine vm = VirtualMachine.attach(vmid);
        vm.startLocalManagementAgent();

        // check layer, class loader, and Package object
        Class<?> clazz = Class.forName("javax.management.MXBean");
        if (clazz.getModule().getLayer() == ModuleLayer.boot())
            throw new RuntimeException("Module is in boot layer!!!");
        ClassLoader loader = clazz.getClassLoader();
        if (loader != null)
            throw new RuntimeException("Unexpected class loader: " + loader);
        Package p = clazz.getPackage();
        if (!p.getName().equals("javax.management"))
            throw new RuntimeException("Unexpected package " + p);
    }
}
