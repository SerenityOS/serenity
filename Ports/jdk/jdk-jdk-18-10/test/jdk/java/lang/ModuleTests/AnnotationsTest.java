/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jdk.internal.module.ModuleInfoWriter;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.commons.ModuleTargetAttribute;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.commons
 *          java.base/jdk.internal.module
 * @run testng AnnotationsTest
 * @summary Basic test of annotations on modules
 */

public class AnnotationsTest {

    /**
     * Test that there are no annotations on an unnamed module.
     */
    @Test
    public void testUnnamedModule() {
        Module module = this.getClass().getModule();
        assertTrue(module.getAnnotations().length == 0);
        assertTrue(module.getDeclaredAnnotations().length == 0);
    }

    /**
     * Test reflectively reading the annotations on a named module.
     */
    @Test
    public void testNamedModule() throws IOException {
        Path mods = Files.createTempDirectory(Path.of(""), "mods");

        // @Deprecated(since="9", forRemoval=true) module foo { }
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo").build();
        byte[] classBytes = ModuleInfoWriter.toBytes(descriptor);
        classBytes = addDeprecated(classBytes, true, "9");
        Files.write(mods.resolve("module-info.class"), classBytes);

        // create module layer with module foo
        Module module = loadModule(mods, "foo");

        // check the annotation is present
        assertTrue(module.isAnnotationPresent(Deprecated.class));
        Deprecated d = module.getAnnotation(Deprecated.class);
        assertNotNull(d, "@Deprecated not found");
        assertTrue(d.forRemoval());
        assertEquals(d.since(), "9");
        Annotation[] a = module.getAnnotations();
        assertTrue(a.length == 1);
        assertTrue(a[0] instanceof Deprecated);
        assertEquals(module.getDeclaredAnnotations(), a);
    }

    /**
     * Test reflectively reading annotations on a named module where the module
     * is mapped to a class loader that can locate a module-info.class.
     */
    @Test
    public void testWithModuleInfoResourceXXXX() throws IOException {
        Path mods = Files.createTempDirectory(Path.of(""), "mods");

        // classes directory with module-info.class
        Path classes = Files.createTempDirectory(Path.of("."), "classes");
        Path mi = classes.resolve("module-info.class");
        try (OutputStream out = Files.newOutputStream(mi)) {
            ModuleDescriptor descriptor = ModuleDescriptor.newModule("lurker").build();
            ModuleInfoWriter.write(descriptor, out);
        }

        // URLClassLoader that can locate a module-info.class resource
        URL url = classes.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[] { url });
        assertTrue(loader.findResource("module-info.class") != null);

        // module foo { }
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo").build();
        byte[] classBytes = ModuleInfoWriter.toBytes(descriptor);
        Files.write(mods.resolve("module-info.class"), classBytes);

        // create module layer with module foo
        Module foo = loadModule(mods, "foo", loader);

        // check the annotation is not present
        assertFalse(foo.isAnnotationPresent(Deprecated.class));

        // @Deprecated(since="11", forRemoval=true) module bar { }
        descriptor = ModuleDescriptor.newModule("bar").build();
        classBytes = ModuleInfoWriter.toBytes(descriptor);
        classBytes = addDeprecated(classBytes, true, "11");
        Files.write(mods.resolve("module-info.class"), classBytes);

        // create module layer with module bar
        Module bar = loadModule(mods, "bar", loader);

        // check the annotation is present
        assertTrue(bar.isAnnotationPresent(Deprecated.class));
    }

    /**
     * Adds the Deprecated annotation to the given module-info class file.
     */
    static byte[] addDeprecated(byte[] bytes, boolean forRemoval, String since) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, cw) { };

        ClassReader cr = new ClassReader(bytes);
        List<Attribute> attrs = new ArrayList<>();
        attrs.add(new ModuleTargetAttribute());
        cr.accept(cv, attrs.toArray(new Attribute[0]), 0);

        AnnotationVisitor annotationVisitor
            = cv.visitAnnotation("Ljava/lang/Deprecated;", true);
        annotationVisitor.visit("forRemoval", forRemoval);
        annotationVisitor.visit("since", since);
        annotationVisitor.visitEnd();

        return cw.toByteArray();
    }

    /**
     * Load the module of the given name in the given directory into a
     * child layer with the given class loader as the parent class loader.
     */
    static Module loadModule(Path dir, String name, ClassLoader parent)
        throws IOException
    {
        ModuleFinder finder = ModuleFinder.of(dir);

        ModuleLayer bootLayer = ModuleLayer.boot();

        Configuration cf = bootLayer.configuration()
                .resolve(finder, ModuleFinder.of(), Set.of(name));

        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, parent);

        Module module = layer.findModule(name).orElse(null);
        assertNotNull(module, name + " not loaded");
        return module;
    }

    static Module loadModule(Path dir, String name) throws IOException {
        return loadModule(dir, name, ClassLoader.getSystemClassLoader());
    }
}
