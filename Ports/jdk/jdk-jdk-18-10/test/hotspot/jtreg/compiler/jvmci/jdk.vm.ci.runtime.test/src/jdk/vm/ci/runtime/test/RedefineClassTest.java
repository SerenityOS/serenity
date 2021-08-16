/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.jvmci
 * @requires vm.jvmti
 * @library ../../../../../
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.attach
 *          java.base/jdk.internal.misc
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler -Djdk.attach.allowAttachSelf jdk.vm.ci.runtime.test.RedefineClassTest
 */

package jdk.vm.ci.runtime.test;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.Assume.assumeTrue;

/**
 * Tests that {@link ResolvedJavaMethod}s are safe in the context of class redefinition being used
 * to redefine the method to which they refer.
 */
public class RedefineClassTest extends TypeUniverse {

    static class Foo {
        public static Object getName() {
            return "foo";
        }
    }

    @Test
    public void test() throws Throwable {

        Method fooMethod = Foo.class.getDeclaredMethod("getName");

        ResolvedJavaMethod foo1 = metaAccess.lookupJavaMethod(fooMethod);
        ResolvedJavaMethod foo2 = metaAccess.lookupJavaMethod(fooMethod);

        String foo1Code = Arrays.toString(foo1.getCode());
        String foo2Code = Arrays.toString(foo2.getCode());

        Assert.assertEquals("foo", Foo.getName());

        redefineFoo();
        System.gc();

        // Make sure the transformation happened
        Assert.assertEquals("bar", Foo.getName());

        Assert.assertEquals(foo1Code, Arrays.toString(foo1.getCode()));
        Assert.assertEquals(foo2Code, Arrays.toString(foo1.getCode()));
    }

    /**
     * Adds the class file bytes for a given class to a JAR stream.
     */
    static void add(JarOutputStream jar, Class<?> c) throws IOException {
        String name = c.getName();
        String classAsPath = name.replace('.', '/') + ".class";
        jar.putNextEntry(new JarEntry(classAsPath));

        InputStream stream = c.getClassLoader().getResourceAsStream(classAsPath);

        int nRead;
        byte[] buf = new byte[1024];
        while ((nRead = stream.read(buf, 0, buf.length)) != -1) {
            jar.write(buf, 0, nRead);
        }

        jar.closeEntry();
    }

    protected void redefineFoo() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Attributes mainAttrs = manifest.getMainAttributes();
        mainAttrs.putValue("Agent-Class", FooAgent.class.getName());
        mainAttrs.putValue("Can-Redefine-Classes", "true");
        mainAttrs.putValue("Can-Retransform-Classes", "true");

        Path jar = Files.createTempFile("myagent", ".jar");
        try {
            JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(jar.toFile()), manifest);
            add(jarStream, FooAgent.class);
            add(jarStream, FooTransformer.class);
            jarStream.close();

            loadAgent(jar);
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    public static void loadAgent(Path agent) throws Exception {
        String pid = Long.toString(ProcessHandle.current().pid());
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Class<?> c = Class.forName("com.sun.tools.attach.VirtualMachine", true, cl);
        Method attach = c.getDeclaredMethod("attach", String.class);
        Method loadAgent = c.getDeclaredMethod("loadAgent", String.class, String.class);
        Method detach = c.getDeclaredMethod("detach");
        Object vm = attach.invoke(null, pid);
        loadAgent.invoke(vm, agent.toString(), "");
        detach.invoke(vm);
    }

    public static class FooAgent {

        public static void agentmain(@SuppressWarnings("unused") String args, Instrumentation inst) throws Exception {
            if (inst.isRedefineClassesSupported() && inst.isRetransformClassesSupported()) {
                inst.addTransformer(new FooTransformer(), true);
                Class<?>[] allClasses = inst.getAllLoadedClasses();
                for (int i = 0; i < allClasses.length; i++) {
                    Class<?> c = allClasses[i];
                    if (c == Foo.class) {
                        inst.retransformClasses(new Class<?>[]{c});
                    }
                }
            }
        }
    }

    /**
     * This transformer replaces the first instance of the constant "foo" in the class file for
     * {@link Foo} with "bar".
     */
    static class FooTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader cl, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (Foo.class.equals(classBeingRedefined)) {
                String cf = new String(classfileBuffer);
                int i = cf.indexOf("foo");
                Assert.assertTrue("cannot find \"foo\" constant in " + Foo.class.getSimpleName() + "'s class file", i > 0);
                classfileBuffer[i] = 'b';
                classfileBuffer[i + 1] = 'a';
                classfileBuffer[i + 2] = 'r';
            }
            return classfileBuffer;
        }
    }
}
