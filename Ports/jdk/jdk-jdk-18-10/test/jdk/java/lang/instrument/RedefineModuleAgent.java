/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent used by RedefineModuleTest
 */

public class RedefineModuleAgent {

    private static Instrumentation inst;

    public static void premain(String args, Instrumentation inst) throws Exception {
        RedefineModuleAgent.inst = inst;
    }

    static void redefineModule(Module module,
                               Set<Module> extraReads,
                               Map<String, Set<Module>> extraExports,
                               Map<String, Set<Module>> extraOpens,
                               Set<Class<?>> extraUses,
                               Map<Class<?>, List<Class<?>>> extraProvides) {
        inst.redefineModule(module, extraReads, extraExports, extraOpens, extraUses, extraProvides);
    }

    static boolean isModifiableModule(Module module) {
        return inst.isModifiableModule(module);
    }
}
