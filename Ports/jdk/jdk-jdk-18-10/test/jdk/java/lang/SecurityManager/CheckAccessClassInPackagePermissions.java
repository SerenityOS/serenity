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

/*
 * @test
 * @bug 8055206
 * @summary Check that each module loaded by the platform loader has the
 *          proper "accessClassInPackage" RuntimePermissions to access its
 *          qualified exports.
 * @run main CheckAccessClassInPackagePermissions
 */

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckAccessClassInPackagePermissions {

    private static final String[] deployModules = {
        "jdk.javaws", "jdk.plugin", "jdk.plugin.server", "jdk.deploy" };

    public static void main(String[] args) throws Exception {

        // Get the modules in the boot layer loaded by the boot or platform
        // loader
        ModuleLayer bootLayer = ModuleLayer.boot();
        Set<Module> modules = bootLayer.modules()
            .stream()
            .filter(CheckAccessClassInPackagePermissions::isBootOrPlatformMod)
            .collect(Collectors.toSet());

        // Create map of target module's qualified export packages
        Map<String, List<String>> map = new HashMap<>();
        Set<Exports> qualExports =
            modules.stream()
                   .map(Module::getDescriptor)
                   .map(ModuleDescriptor::exports)
                   .flatMap(Set::stream)
                   .filter(Exports::isQualified)
                   .collect(Collectors.toSet());
        for (Exports e : qualExports) {
            Set<String> targets = e.targets();
            for (String t : targets) {
                map.compute(t, (k, ov) -> {
                    if (ov == null) {
                        List<String> v = new ArrayList<>();
                        v.add(e.source());
                        return v;
                    } else {
                        ov.add(e.source());
                        return ov;
                    }
                });
            }
        }

        // Check if each target module has the right permissions to access
        // its qualified exports
        Policy policy = Policy.getPolicy();
        List<String> deployMods = Arrays.asList(deployModules);
        for (Map.Entry<String, List<String>> me : map.entrySet()) {
            String moduleName = me.getKey();

            // skip deploy modules since they are granted permissions in
            // deployment policy file
            if (deployMods.contains(moduleName)) {
                continue;
            }

            // is this a module loaded by the platform loader?
            Optional<Module> module = bootLayer.findModule(moduleName);
            if (!module.isPresent()) {
                continue;
            }
            Module mod = module.get();
            if (mod.getClassLoader() != ClassLoader.getPlatformClassLoader()) {
                continue;
            }

            // create ProtectionDomain simulating module
            URL url = new URL("jrt:/" + moduleName);
            CodeSource cs = new CodeSource(url, (CodeSigner[])null);
            ProtectionDomain pd = new ProtectionDomain(cs, null, null, null);

            List<String> pkgs = me.getValue();
            for (String p : pkgs) {
                RuntimePermission rp =
                    new RuntimePermission("accessClassInPackage." + p);
                if (!policy.implies(pd, rp)) {
                    throw new Exception("Module " + mod + " has not been " +
                                        "granted " + rp);
                }
            }
        }
    }

    /**
     * Returns true if the module's loader is the boot or platform loader.
     */
    private static boolean isBootOrPlatformMod(Module m) {
        return m.getClassLoader() == null ||
               m.getClassLoader() == ClassLoader.getPlatformClassLoader();
    }
}
