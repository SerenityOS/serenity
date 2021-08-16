/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.lang.module.ModuleDescriptor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import sun.hotspot.WhiteBox;

public class ModuleHelper {

    public static void DefineModule(Object module, boolean is_open, String version,
                                    String location, String[] pkgs) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.DefineModule(module, is_open, version, location, pkgs);
    }

    public static void AddModuleExports(Object from, String pkg, Object to) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.AddModuleExports(from, pkg, to);
        java.lang.ModuleHelper.addExportsNoSync((Module)from, pkg, (Module)to);
    }

    public static void AddReadsModule(Object from, Object to) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.AddReadsModule(from, to);
        java.lang.ModuleHelper.addReadsNoSync((Module)from, (Module)to);
    }

    public static void AddModuleExportsToAllUnnamed(Object m, String pkg) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.AddModuleExportsToAllUnnamed(m, pkg);
        //java.lang.ModuleHelper.addExportsToAllUnnamedNoSync((Module)m, pkg);
    }

    public static void AddModuleExportsToAll(Object m, String pkg) throws Throwable {
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.AddModuleExportsToAll(m, pkg);
        java.lang.ModuleHelper.addExportsNoSync((Module)m, pkg, (Module)null);
    }

    public static Module ModuleObject(String name, ClassLoader loader, String[] pkgs) throws Throwable {
        Set<String> pkg_set = new HashSet<>();
        if (pkgs != null) {
            for (String pkg: pkgs) {
                pkg_set.add(pkg.replace('/', '.'));
            }
        } else {
            pkg_set = Collections.emptySet();
        }

        ModuleDescriptor descriptor =
            ModuleDescriptor.newModule(name).packages(pkg_set).build();
        URI uri = URI.create("module:/" + name);

        return java.lang.ModuleHelper.newModule(loader, descriptor);
    }

}
