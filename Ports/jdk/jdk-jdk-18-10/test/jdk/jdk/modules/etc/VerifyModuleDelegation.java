/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify the defining class loader of each module never delegates
 *          to its child class loader.
 * @run testng/othervm --add-modules=ALL-SYSTEM VerifyModuleDelegation
 */

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import static java.util.stream.Collectors.toSet;

import org.testng.annotations.*;

import static org.testng.Assert.*;

public class VerifyModuleDelegation {
    private static final String JAVA_BASE = "java.base";

    private static final ModuleDescriptor BASE
        = ModuleDescriptor.newModule(JAVA_BASE).build();

    private static final Set<ModuleDescriptor> MREFS
            = ModuleLayer.boot().modules().stream().map(Module::getDescriptor)
                .collect(toSet());

    private void check(ModuleDescriptor md, ModuleDescriptor ref) {
        assertTrue(md.requires().size() == ref.requires().size());
        assertTrue(md.requires().containsAll(ref.requires()));
    }

    @Test
    public void checkJavaBase() {
        ModuleDescriptor md =
                MREFS.stream()
                     .filter(d -> d.name().equals(JAVA_BASE))
                     .findFirst().orElseThrow(Error::new);

        check(md, BASE);
    }

    @Test
    public void checkLoaderDelegation() {
        ModuleLayer boot = ModuleLayer.boot();
        MREFS.stream()
             .forEach(md -> md.requires().stream().forEach(req ->
                 {
                     // check if M requires D and D's loader must be either the
                     // same or an ancestor of M's loader
                     ClassLoader loader1 = boot.findLoader(md.name());
                     ClassLoader loader2 = boot.findLoader(req.name());
                     if (loader1 != loader2 && !isAncestor(loader2, loader1)) {
                         throw new Error(loader1.getName() + "/" + md.name() +
                             " can't delegate to find classes from " +
                             loader2.getName() + "/" + req.name());
                     }
                 }));
    }

    // Returns true if p is an ancestor of cl i.e. class loader 'p' can
    // be found in the cl's delegation chain
    private static boolean isAncestor(ClassLoader p, ClassLoader cl) {
        if (p != null && cl == null) {
            return false;
        }
        ClassLoader acl = cl;
        do {
            acl = acl.getParent();
            if (p == acl) {
                return true;
            }
        } while (acl != null);
        return false;
    }
}
