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

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Module_attribute;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class ModuleTestBase {
    protected final ToolBox tb = new ToolBox();
    private final TestResult tr = new TestResult();


    protected void run() throws Exception {
        boolean noTests = true;
        for (Method method : this.getClass().getMethods()) {
            if (method.isAnnotationPresent(Test.class)) {
                noTests = false;
                try {
                    tr.addTestCase(method.getName());
                    method.invoke(this, Paths.get(method.getName()));
                } catch (Throwable th) {
                    tr.addFailure(th);
                }
            }
        }
        if (noTests) throw new AssertionError("Tests are not found.");
        tr.checkStatus();
    }

    protected void testModuleAttribute(Path modulePath, ModuleDescriptor moduleDescriptor) throws Exception {
        ClassFile classFile = ClassFile.read(modulePath.resolve("module-info.class"));
        Module_attribute moduleAttribute = (Module_attribute) classFile.getAttribute("Module");
        ConstantPool constantPool = classFile.constant_pool;
        testModuleName(moduleDescriptor, moduleAttribute, constantPool);
        testModuleFlags(moduleDescriptor, moduleAttribute);
        testRequires(moduleDescriptor, moduleAttribute, constantPool);
        testExports(moduleDescriptor, moduleAttribute, constantPool);
        testOpens(moduleDescriptor, moduleAttribute, constantPool);
        testProvides(moduleDescriptor, moduleAttribute, constantPool);
        testUses(moduleDescriptor, moduleAttribute, constantPool);
    }

    private void testModuleName(ModuleDescriptor moduleDescriptor, Module_attribute module, ConstantPool constantPool) throws ConstantPoolException {
        tr.checkEquals(constantPool.getModuleInfo(module.module_name).getName(), moduleDescriptor.name, "Unexpected module name");
    }

    private void testModuleFlags(ModuleDescriptor moduleDescriptor, Module_attribute module) {
        tr.checkEquals(module.module_flags, moduleDescriptor.flags, "Unexpected module flags");
    }

    private void testRequires(ModuleDescriptor moduleDescriptor, Module_attribute module, ConstantPool constantPool) throws ConstantPoolException {
        tr.checkEquals(module.requires_count, moduleDescriptor.requires.size(), "Wrong amount of requires.");

        List<Requires> actualRequires = new ArrayList<>();
        for (Module_attribute.RequiresEntry require : module.requires) {
            actualRequires.add(new Requires(
                    require.getRequires(constantPool),
                    require.requires_flags));
        }
        tr.checkContains(actualRequires, moduleDescriptor.requires, "Lists of requires don't match");
    }

    private void testExports(ModuleDescriptor moduleDescriptor, Module_attribute module, ConstantPool constantPool) throws ConstantPoolException {
        tr.checkEquals(module.exports_count, moduleDescriptor.exports.size(), "Wrong amount of exports.");
        for (Module_attribute.ExportsEntry export : module.exports) {
            String pkg = constantPool.getPackageInfo(export.exports_index).getName();
            if (tr.checkTrue(moduleDescriptor.exports.containsKey(pkg), "Unexpected export " + pkg)) {
                Export expectedExport = moduleDescriptor.exports.get(pkg);
                tr.checkEquals(expectedExport.mask, export.exports_flags, "Wrong export flags");
                List<String> expectedTo = expectedExport.to;
                tr.checkEquals(export.exports_to_count, expectedTo.size(), "Wrong amount of exports to");
                List<String> actualTo = new ArrayList<>();
                for (int toIdx : export.exports_to_index) {
                    actualTo.add(constantPool.getModuleInfo(toIdx).getName());
                }
                tr.checkContains(actualTo, expectedTo, "Lists of \"exports to\" don't match.");
            }
        }
    }

    private void testOpens(ModuleDescriptor moduleDescriptor, Module_attribute module, ConstantPool constantPool) throws ConstantPoolException {
        tr.checkEquals(module.opens_count, moduleDescriptor.opens.size(), "Wrong amount of opens.");
        for (Module_attribute.OpensEntry open : module.opens) {
            String pkg = constantPool.getPackageInfo(open.opens_index).getName();
            if (tr.checkTrue(moduleDescriptor.opens.containsKey(pkg), "Unexpected open " + pkg)) {
                Open expectedOpen = moduleDescriptor.opens.get(pkg);
                tr.checkEquals(expectedOpen.mask, open.opens_flags, "Wrong open flags");
                List<String> expectedTo = expectedOpen.to;
                tr.checkEquals(open.opens_to_count, expectedTo.size(), "Wrong amount of opens to");
                List<String> actualTo = new ArrayList<>();
                for (int toIdx : open.opens_to_index) {
                    actualTo.add(constantPool.getModuleInfo(toIdx).getName());
                }
                tr.checkContains(actualTo, expectedTo, "Lists of \"opens to\" don't match.");
            }
        }
    }

    private void testUses(ModuleDescriptor moduleDescriptor, Module_attribute module, ConstantPool constantPool) throws ConstantPoolException {
        tr.checkEquals(module.uses_count, moduleDescriptor.uses.size(), "Wrong amount of uses.");
        List<String> actualUses = new ArrayList<>();
        for (int usesIdx : module.uses_index) {
            String uses = constantPool.getClassInfo(usesIdx).getBaseName();
            actualUses.add(uses);
        }
        tr.checkContains(actualUses, moduleDescriptor.uses, "Lists of uses don't match");
    }

    private void testProvides(ModuleDescriptor moduleDescriptor, Module_attribute module, ConstantPool constantPool) throws ConstantPoolException {
        int moduleProvidesCount = Arrays.asList(module.provides).stream()
                .mapToInt(e -> e.with_index.length)
                .sum();
        int moduleDescriptorProvidesCount = moduleDescriptor.provides.values().stream()
                .mapToInt(impls -> impls.size())
                .sum();
        tr.checkEquals(moduleProvidesCount, moduleDescriptorProvidesCount, "Wrong amount of provides.");
        Map<String, List<String>> actualProvides = new HashMap<>();
        for (Module_attribute.ProvidesEntry provide : module.provides) {
            String provides = constantPool.getClassInfo(provide.provides_index).getBaseName();
            List<String> impls = new ArrayList<>();
            for (int i = 0; i < provide.with_count; i++) {
                String with = constantPool.getClassInfo(provide.with_index[i]).getBaseName();
                impls.add(with);
            }
            actualProvides.put(provides, impls);
        }
        tr.checkContains(actualProvides.entrySet(), moduleDescriptor.provides.entrySet(), "Lists of provides don't match");
    }

    protected void compile(Path base, String... options) throws IOException {
        new JavacTask(tb)
                .options(options)
                .files(findJavaFiles(base))
                .run(Task.Expect.SUCCESS)
                .writeAll();
    }

    private static Path[] findJavaFiles(Path src) throws IOException {
        return Files.find(src, Integer.MAX_VALUE, (path, attr) -> path.toString().endsWith(".java"))
                .toArray(Path[]::new);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Test {
    }

    interface Mask {
        int getMask();
    }

    public enum ModuleFlag implements Mask {
        OPEN("open", Module_attribute.ACC_OPEN);

        private final String token;
        private final int mask;

        ModuleFlag(String token, int mask) {
            this.token = token;
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    public enum RequiresFlag implements Mask {
        TRANSITIVE("transitive", Module_attribute.ACC_TRANSITIVE),
        STATIC("static", Module_attribute.ACC_STATIC_PHASE),
        MANDATED("", Module_attribute.ACC_MANDATED);

        private final String token;
        private final int mask;

        RequiresFlag(String token, int mask) {
            this.token = token;
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    public enum ExportsFlag implements Mask {
        SYNTHETIC("", Module_attribute.ACC_SYNTHETIC);

        private final String token;
        private final int mask;

        ExportsFlag(String token, int mask) {
            this.token = token;
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    public enum OpensFlag implements Mask {
        SYNTHETIC("", Module_attribute.ACC_SYNTHETIC);

        private final String token;
        private final int mask;

        OpensFlag(String token, int mask) {
            this.token = token;
            this.mask = mask;
        }

        @Override
        public int getMask() {
            return mask;
        }
    }

    private class Export {
        private final String pkg;
        private final int mask;
        private final List<String> to = new ArrayList<>();

        Export(String pkg, int mask) {
            this.pkg = pkg;
            this.mask = mask;
        }
    }

    private class Open {
        private final String pkg;
        private final int mask;
        private final List<String> to = new ArrayList<>();

        Open(String pkg, int mask) {
            this.pkg = pkg;
            this.mask = mask;
        }
    }

    private class Requires {
        private final String module;
        private final int mask;

        Requires(String module, int mask) {
            this.module = module;
            this.mask = mask;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Requires requires = (Requires) o;
            return mask == requires.mask &&
                    Objects.equals(module, requires.module);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, mask);
        }
    }

    protected class ModuleDescriptor {

        private final String name;
        private final int flags;

        private final List<Requires> requires = new ArrayList<>();

        {
            requires.add(new Requires("java.base", computeMask(RequiresFlag.MANDATED)));
        }

        private final Map<String, Export> exports = new HashMap<>();
        private final Map<String, Open> opens = new HashMap<>();

        //List of service and implementation
        private final Map<String, List<String>> provides = new LinkedHashMap<>();
        private final List<String> uses = new ArrayList<>();

        private static final String LINE_END = ";\n";

        StringBuilder content = new StringBuilder("");

        public ModuleDescriptor(String moduleName, ModuleFlag... flags) {
            this.name = moduleName;
            this.flags = computeMask(flags);
            for (ModuleFlag flag : flags) {
                content.append(flag.token).append(" ");
            }
            content.append("module ").append(moduleName).append('{').append('\n');
        }

        public ModuleDescriptor requires(String module) {
            this.requires.add(new Requires(module, 0));
            content.append("    requires ").append(module).append(LINE_END);

            return this;
        }

        public ModuleDescriptor requires(String module, RequiresFlag... flags) {
            this.requires.add(new Requires(module, computeMask(flags)));

            content.append("    requires ");
            for (RequiresFlag flag : flags) {
                content.append(flag.token).append(" ");
            }
            content.append(module).append(LINE_END);

            return this;
        }

        public ModuleDescriptor exports(String pkg, ExportsFlag... flags) {
            this.exports.put(toInternalForm(pkg), new Export(toInternalForm(pkg), computeMask(flags)));
            content.append("    exports ");
            for (ExportsFlag flag : flags) {
                content.append(flag.token).append(" ");
            }
            content.append(pkg).append(LINE_END);
            return this;
        }

        public ModuleDescriptor exportsTo(String pkg, String to, ExportsFlag... flags) {
            List<String> tos = Pattern.compile(",")
                    .splitAsStream(to)
                    .map(String::trim)
                    .collect(Collectors.toList());
            this.exports.compute(toInternalForm(pkg), (k,v) -> new Export(k, computeMask(flags)))
                    .to.addAll(tos);

            content.append("    exports ");
            for (ExportsFlag flag : flags) {
                content.append(flag.token).append(" ");
            }
            content.append(pkg).append(" to ").append(to).append(LINE_END);
            return this;
        }

        public ModuleDescriptor opens(String pkg, OpensFlag... flags) {
            this.opens.put(toInternalForm(pkg), new Open(toInternalForm(pkg), computeMask(flags)));
            content.append("    opens ");
            for (OpensFlag flag : flags) {
                content.append(flag.token).append(" ");
            }
            content.append(pkg).append(LINE_END);
            return this;
        }

        public ModuleDescriptor opensTo(String pkg, String to, OpensFlag... flags) {
            List<String> tos = Pattern.compile(",")
                    .splitAsStream(to)
                    .map(String::trim)
                    .collect(Collectors.toList());
            this.opens.compute(toInternalForm(pkg), (k,v) -> new Open(toInternalForm(k), computeMask(flags)))
                    .to.addAll(tos);

            content.append("    opens ");
            for (OpensFlag flag : flags) {
                content.append(flag.token).append(" ");
            }
            content.append(pkg).append(" to ").append(to).append(LINE_END);
            return this;
        }

        public ModuleDescriptor provides(String provides, String... with) {
            List<String> impls = Arrays.stream(with)
                    .map(this::toInternalForm)
                    .collect(Collectors.toList());
            this.provides.put(toInternalForm(provides), impls);
            content.append("    provides ")
                    .append(provides)
                    .append(" with ")
                    .append(String.join(",", with))
                    .append(LINE_END);
            return this;
        }

        public ModuleDescriptor uses(String... uses) {
            for (String use : uses) {
                this.uses.add(toInternalForm(use));
                content.append("    uses ").append(use).append(LINE_END);
            }
            return this;
        }

        public ModuleDescriptor write(Path path) throws IOException {
            String src = content.append('}').toString();

            tb.createDirectories(path);
            tb.writeJavaFiles(path, src);
            return this;
        }

        private String toInternalForm(String name) {
            return name.replace('.', '/');
        }

        private int computeMask(Mask... masks) {
            return Arrays.stream(masks)
                    .map(Mask::getMask)
                    .reduce((a, b) -> a | b)
                    .orElseGet(() -> 0);
        }
    }
}
