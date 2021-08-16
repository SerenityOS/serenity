/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main {
    public static void main(String... args) throws Exception {
        ModuleFinder afterFinder = ModuleFinder.of(Paths.get(args[0], "mods"));

        Configuration cf = ModuleLayer.boot().configuration()
                .resolveAndBind(ModuleFinder.of(), afterFinder,
                    List.of("m1", "m2"));

        System.out.println("Configuration: " + cf);

        ModuleLayer l = ModuleLayer.defineModulesWithManyLoaders(cf,
                List.of(ModuleLayer.boot()),
                        ClassLoader.getPlatformClassLoader())
                .layer();

        Module m1 = l.findModule("m1").get();
        ResourceBundle bundle =
            ResourceBundle.getBundle("p.resources.MyResource",
                                     Locale.US, m1);
        ResourceBundle jabundle =
            ResourceBundle.getBundle("p.resources.MyResource",
                                     Locale.JAPANESE, m1);

        String enResult = bundle.getString("key");
        String jaResult = jabundle.getString("key");
        if (!"hi".equals(enResult) || !"ja".equals(jaResult)) {
            throw new RuntimeException("Unexpected resources loaded: en: " +
                                        enResult + ", ja: " + jaResult);
        }

        Class<?> c = Class.forName(m1, "p.Main");
        Method m = c.getDeclaredMethod("run");
        m.invoke(null);
    }
}
