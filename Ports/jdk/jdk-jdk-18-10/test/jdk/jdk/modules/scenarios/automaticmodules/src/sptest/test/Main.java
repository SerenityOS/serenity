/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package test;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Provides;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.script.ScriptEngineFactory;

/**
 * Test that the automatic module "bananascript" is in the boot layer and
 * it behaves as a service provider.
 */

public class Main {

    public static void main(String[] args) throws Exception {

        Optional<Module> om = ModuleLayer.boot().findModule("bananascript");
        assertTrue(om.isPresent());

        ModuleDescriptor descriptor = om.get().getDescriptor();

        // requires java.base
        Set<String> requires
            = descriptor.requires().stream()
                .map(Requires::name)
                .collect(Collectors.toSet());
        assertTrue(requires.size() == 1);
        assertTrue(requires.contains("java.base"));

        // uses ScriptEngineFactory
        Set<Provides> provides = descriptor.provides();
        assertTrue(provides.size() == 1);
        String sn = ScriptEngineFactory.class.getName();
        assertTrue(provides.iterator().next().service().equals(sn));

        // Check that it is iterated over with ServiceLoader
        ServiceLoader<ScriptEngineFactory> sl
            = ServiceLoader.load(ScriptEngineFactory.class);
        boolean found = false;
        for (ScriptEngineFactory factory : sl) {
            String name = factory.getEngineName();
            System.out.println(name);
            if (name.equals("BananaScriptEngine"))
                found = true;
        }
        assertTrue(found);
    }

    static void assertTrue(boolean e) {
        if (!e)
            throw new RuntimeException();
    }
}
