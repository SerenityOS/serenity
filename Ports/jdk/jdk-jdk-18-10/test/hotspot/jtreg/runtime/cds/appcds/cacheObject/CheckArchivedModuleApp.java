/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Set;
import sun.hotspot.WhiteBox;

//
// Test archived system module graph when open archive heap objects are mapped:
//
public class CheckArchivedModuleApp {
    static WhiteBox wb;
    public static void main(String args[]) throws Exception {
        wb = WhiteBox.getWhiteBox();

        if (!wb.areOpenArchiveHeapObjectsMapped()) {
            System.out.println("Archived open_archive_heap objects are not mapped.");
            System.out.println("This may happen during normal operation. Test Skipped.");
            return;
        }

        if (args.length != 2) {
           throw new RuntimeException(
               "FAILED. Incorrect argument length: " + args.length);
        }

        boolean expectArchivedDescriptors = "yes".equals(args[0]);
        boolean expectArchivedConfiguration = "yes".equals(args[1]);
        // -XX:+EnableJVMCI adds extra system modules, in which case the system
        // module objects are not archived.
        Boolean enableJVMCI = wb.getBooleanVMFlag("EnableJVMCI");
        if (enableJVMCI != null && enableJVMCI) {
            expectArchivedDescriptors = false;
            expectArchivedConfiguration = false;
        }

        checkModuleDescriptors(expectArchivedDescriptors);
        checkConfiguration(expectArchivedConfiguration);
        checkEmptyConfiguration(expectArchivedConfiguration);
        checkEmptyLayer();
    }

    private static void checkModuleDescriptors(boolean expectArchivedDescriptors) {
        Set<Module> modules = ModuleLayer.boot().modules();
        for (Module m : modules) {
            ModuleDescriptor md = m.getDescriptor();
            String name = md.name();
            if (expectArchivedDescriptors) {
                if (wb.isShared(md)) {
                    System.out.println(name + " is archived. Expected.");
                } else {
                    throw new RuntimeException(
                        "FAILED. " + name + " is not archived. Expect archived.");
                }
            } else {
                if (!wb.isShared(md)) {
                    System.out.println(name + " is not archived. Expected.");
                } else {
                    throw new RuntimeException(
                        "FAILED. " + name + " is archived. Expect not archived.");
                }
            }
        }
    }

    private static void checkEmptyConfiguration(boolean expectArchivedConfiguration) {
        // Configuration.EMPTY_CONFIGURATION uses the singletons,
        // ListN.EMPTY_LIST, SetN.EMPTY_SET and MapN.EMPTY_MAP in
        // ImmutableCollections for the 'parents', 'modules' and
        // 'graph' fields. The ImmutableCollections singletons
        // can be accessed via List.of(), Set.of() and Map.of() APIs.
        // Configuration public APIs also allow access to the
        // EMPTY_CONFIGURATION's 'parents' and 'modules'. When the
        // archived java heap data is enabled at runtime, make sure
        // the EMPTY_CONFIGURATION.parents and EMPTY_CONFIGURATION.modules
        // are the archived ImmutableCollections singletons.
        Configuration emptyCf = Configuration.empty();
        List emptyCfParents = emptyCf.parents();
        Set emptyCfModules = emptyCf.modules();
        if (expectArchivedConfiguration) {
            if (emptyCfParents == List.of() &&
                wb.isShared(emptyCfParents)) {
                System.out.println("Empty Configuration has expected parents.");
            } else {
                throw new RuntimeException(
                    "FAILED. Unexpected parents for empty Configuration.");
            }
            if (emptyCfModules == Set.of() &&
                wb.isShared(emptyCfModules)) {
                System.out.println("Empty Configuration has expected module set.");
            } else {
                throw new RuntimeException(
                    "FAILED. Unexpected module set for empty Configuration.");
            }
        }
    }



    private static void checkConfiguration(boolean expectArchivedConfiguration) {
        Configuration cf = ModuleLayer.boot().configuration();

        if (expectArchivedConfiguration) {
            if (wb.isShared(cf)) {
                System.out.println("Boot layer configuration is archived. Expected.");
            } else {
                throw new RuntimeException(
                    "FAILED. Boot layer configuration is not archived.");
            }
        } else {
            if (!wb.isShared(cf)) {
                System.out.println("Boot layer configuration is not archived. Expected.");
            } else {
                throw new RuntimeException(
                    "FAILED. Boot layer configuration is archived.");
            }
        }
    }

    private static void checkEmptyLayer() {
        // ModuleLayer.EMPTY_FIELD returned by empty() method is singleton.
        // Check that with CDS there is still a single instance of EMPTY_LAYER
        // and boot() layer parent is THE empty layer.
        if (ModuleLayer.empty() != ModuleLayer.boot().parents().get(0)) {
            throw new RuntimeException("FAILED. Empty module layer is not singleton");
        }
    }
}
