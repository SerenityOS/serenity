/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package somelib;

import java.lang.module.ModuleDescriptor;

public class Invariants {
    public static void test(String expectPatch) {
        ModuleDescriptor ownDesc = Invariants.class.getModule().getDescriptor();

        assertThat(ownDesc.isAutomatic(), "Expected to be executed in an automatic module");
        assertThat(ownDesc.requires().stream().anyMatch(
                r -> r.name().equals("java.base") && r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED)),
                "requires mandated java.base");
        assertThat(Dummy.returnTrue(), "Dummy.returnTrue returns true");
        assertThat(expectPatch.equals(PatchInfo.patchName()), "Module is patched with the right patch");
    }

    private static void assertThat(boolean expected, String message) {
        if (!expected) {
            throw new AssertionError(message);
        }
    }
}
