/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @summary Test that type annotations are retained after a retransform
 * @requires vm.jvmti
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineAnnotations buildagent
 * @run main/othervm -javaagent:redefineagent.jar RedefineAnnotations
 */

import static jdk.test.lib.Asserts.assertTrue;
import jdk.test.lib.helpers.ClassFileInstaller;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.NoSuchFieldException;
import java.lang.NoSuchMethodException;
import java.lang.RuntimeException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.TypeVariable;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.ASM7;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface TestAnn {
    String site();
}

public class RedefineAnnotations {
    static Instrumentation inst;
    public static void premain(String agentArgs, Instrumentation inst) {
        RedefineAnnotations.inst = inst;
    }

    static class Transformer implements ClassFileTransformer {

        public byte[] asm(ClassLoader loader, String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

            ClassWriter cw = new ClassWriter(0);
            ClassVisitor cv = new ReAddDummyFieldsClassVisitor(ASM7, cw) { };
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(cv, 0);
            return cw.toByteArray();
        }

        public class ReAddDummyFieldsClassVisitor extends ClassVisitor {

            LinkedList<F> fields = new LinkedList<>();

            public ReAddDummyFieldsClassVisitor(int api, ClassVisitor cv) {
                super(api, cv);
            }

            @Override public FieldVisitor visitField(int access, String name,
                    String desc, String signature, Object value) {
                if (name.startsWith("dummy")) {
                    // Remove dummy field
                    fields.addLast(new F(access, name, desc, signature, value));
                    return null;
                }
                return cv.visitField(access, name, desc, signature, value);
            }

            @Override public void visitEnd() {
                F f;
                while ((f = fields.pollFirst()) != null) {
                    // Re-add dummy fields
                    cv.visitField(f.access, f.name, f.desc, f.signature, f.value);
                }
            }

            private class F {
                private int access;
                private String name;
                private String desc;
                private String signature;
                private Object value;
                F(int access, String name, String desc, String signature, Object value) {
                    this.access = access;
                    this.name = name;
                    this.desc = desc;
                    this.signature = signature;
                    this.value = value;
                }
            }
        }

        @Override public byte[] transform(ClassLoader loader, String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

            if (className.contains("TypeAnnotatedTestClass")) {
                try {
                    // Here we remove and re-add the dummy fields. This shuffles the constant pool
                    return asm(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                } catch (Throwable e) {
                    // The retransform native code that called this method does not propagate
                    // exceptions. Instead of getting an uninformative generic error, catch
                    // problems here and print it, then exit.
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            return null;
        }
    }

    private static void buildAgent() {
        try {
            ClassFileInstaller.main("RedefineAnnotations");
        } catch (Exception e) {
            throw new RuntimeException("Could not write agent classfile", e);
        }

        try {
            PrintWriter pw = new PrintWriter("MANIFEST.MF");
            pw.println("Premain-Class: RedefineAnnotations");
            pw.println("Agent-Class: RedefineAnnotations");
            pw.println("Can-Retransform-Classes: true");
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write manifest file for the agent", e);
        }

        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jarTool.run(new String[] { "-cmf", "MANIFEST.MF", "redefineagent.jar", "RedefineAnnotations.class" })) {
            throw new RuntimeException("Could not write the agent jar file");
        }
    }

    public static void main(String argv[]) throws NoSuchFieldException, NoSuchMethodException {
        if (argv.length == 1 && argv[0].equals("buildagent")) {
            buildAgent();
            return;
        }

        if (inst == null) {
            throw new RuntimeException("Instrumentation object was null");
        }

        RedefineAnnotations test = new RedefineAnnotations();
        test.testTransformAndVerify();
    }

    // Class type annotations
    private Annotation classTypeParameterTA;
    private Annotation extendsTA;
    private Annotation implementsTA;

    // Field type annotations
    private Annotation fieldTA;
    private Annotation innerTA;
    private Annotation[] arrayTA = new Annotation[4];
    private Annotation[] mapTA = new Annotation[5];

    // Method type annotations
    private Annotation returnTA, methodTypeParameterTA, formalParameterTA, throwsTA;

    private void testTransformAndVerify()
        throws NoSuchFieldException, NoSuchMethodException {

        Class<TypeAnnotatedTestClass> c = TypeAnnotatedTestClass.class;
        Class<?> myClass = c;

        /*
         * Verify that the expected annotations are where they should be before transform.
         */
        verifyClassTypeAnnotations(c);
        verifyFieldTypeAnnotations(c);
        verifyMethodTypeAnnotations(c);

        try {
            inst.addTransformer(new Transformer(), true);
            inst.retransformClasses(myClass);
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }

        /*
         * Verify that the expected annotations are where they should be after transform.
         * Also verify that before and after are equal.
         */
        verifyClassTypeAnnotations(c);
        verifyFieldTypeAnnotations(c);
        verifyMethodTypeAnnotations(c);
    }

    private void verifyClassTypeAnnotations(Class c) {
        Annotation anno;

        anno = c.getTypeParameters()[0].getAnnotations()[0];
        verifyTestAnn(classTypeParameterTA, anno, "classTypeParameter");
        classTypeParameterTA = anno;

        anno = c.getAnnotatedSuperclass().getAnnotations()[0];
        verifyTestAnn(extendsTA, anno, "extends");
        extendsTA = anno;

        anno = c.getAnnotatedInterfaces()[0].getAnnotations()[0];
        verifyTestAnn(implementsTA, anno, "implements");
        implementsTA = anno;
    }

    private void verifyFieldTypeAnnotations(Class c)
        throws NoSuchFieldException, NoSuchMethodException {

        verifyBasicFieldTypeAnnotations(c);
        verifyInnerFieldTypeAnnotations(c);
        verifyArrayFieldTypeAnnotations(c);
        verifyMapFieldTypeAnnotations(c);
    }

    private void verifyBasicFieldTypeAnnotations(Class c)
        throws NoSuchFieldException, NoSuchMethodException {

        Annotation anno = c.getDeclaredField("typeAnnotatedBoolean").getAnnotatedType().getAnnotations()[0];
        verifyTestAnn(fieldTA, anno, "field");
        fieldTA = anno;
    }

    private void verifyInnerFieldTypeAnnotations(Class c)
        throws NoSuchFieldException, NoSuchMethodException {

        AnnotatedType at = c.getDeclaredField("typeAnnotatedInner").getAnnotatedType();
        Annotation anno = at.getAnnotations()[0];
        verifyTestAnn(innerTA, anno, "inner");
        innerTA = anno;
    }

    private void verifyArrayFieldTypeAnnotations(Class c)
        throws NoSuchFieldException, NoSuchMethodException {

        Annotation anno;
        AnnotatedType at;

        at = c.getDeclaredField("typeAnnotatedArray").getAnnotatedType();
        anno = at.getAnnotations()[0];
        verifyTestAnn(arrayTA[0], anno, "array1");
        arrayTA[0] = anno;

        for (int i = 1; i <= 3; i++) {
            at = ((AnnotatedArrayType) at).getAnnotatedGenericComponentType();
            anno = at.getAnnotations()[0];
            verifyTestAnn(arrayTA[i], anno, "array" + (i + 1));
            arrayTA[i] = anno;
        }
    }

    private void verifyMapFieldTypeAnnotations(Class c)
        throws NoSuchFieldException, NoSuchMethodException {

        Annotation anno;
        AnnotatedType atBase;
        AnnotatedType atParameter;
        atBase = c.getDeclaredField("typeAnnotatedMap").getAnnotatedType();

        anno = atBase.getAnnotations()[0];
        verifyTestAnn(mapTA[0], anno, "map1");
        mapTA[0] = anno;

        atParameter =
            ((AnnotatedParameterizedType) atBase).
            getAnnotatedActualTypeArguments()[0];
        anno = ((AnnotatedWildcardType) atParameter).getAnnotations()[0];
        verifyTestAnn(mapTA[1], anno, "map2");
        mapTA[1] = anno;

        anno =
            ((AnnotatedWildcardType) atParameter).
            getAnnotatedUpperBounds()[0].getAnnotations()[0];
        verifyTestAnn(mapTA[2], anno, "map3");
        mapTA[2] = anno;

        atParameter =
            ((AnnotatedParameterizedType) atBase).
            getAnnotatedActualTypeArguments()[1];
        anno = ((AnnotatedParameterizedType) atParameter).getAnnotations()[0];
        verifyTestAnn(mapTA[3], anno, "map4");
        mapTA[3] = anno;

        anno =
            ((AnnotatedParameterizedType) atParameter).
            getAnnotatedActualTypeArguments()[0].getAnnotations()[0];
        verifyTestAnn(mapTA[4], anno, "map5");
        mapTA[4] = anno;
    }

    private void verifyMethodTypeAnnotations(Class c)
        throws NoSuchFieldException, NoSuchMethodException {
        Annotation anno;
        Executable typeAnnotatedMethod =
            c.getDeclaredMethod("typeAnnotatedMethod", TypeAnnotatedTestClass.class);

        anno = typeAnnotatedMethod.getAnnotatedReturnType().getAnnotations()[0];
        verifyTestAnn(returnTA, anno, "return");
        returnTA = anno;

        anno = typeAnnotatedMethod.getTypeParameters()[0].getAnnotations()[0];
        verifyTestAnn(methodTypeParameterTA, anno, "methodTypeParameter");
        methodTypeParameterTA = anno;

        anno = typeAnnotatedMethod.getAnnotatedParameterTypes()[0].getAnnotations()[0];
        verifyTestAnn(formalParameterTA, anno, "formalParameter");
        formalParameterTA = anno;

        anno = typeAnnotatedMethod.getAnnotatedExceptionTypes()[0].getAnnotations()[0];
        verifyTestAnn(throwsTA, anno, "throws");
        throwsTA = anno;
    }

    private static void verifyTestAnn(Annotation verifyAgainst, Annotation anno, String expectedSite) {
        verifyTestAnnSite(anno, expectedSite);

        // When called before transform verifyAgainst will be null, when called
        // after transform it will be the annotation from before the transform
        if (verifyAgainst != null) {
            assertTrue(anno.equals(verifyAgainst),
                       "Annotations do not match before and after." +
                       " Before: \"" + verifyAgainst + "\", After: \"" + anno + "\"");
        }
    }

    private static void verifyTestAnnSite(Annotation testAnn, String expectedSite) {
        String expectedAnn = "@TestAnn(site=\"" + expectedSite + "\")";
        assertTrue(testAnn.toString().equals(expectedAnn),
                   "Expected \"" + expectedAnn + "\", got \"" + testAnn + "\"");
    }

    public static class TypeAnnotatedTestClass <@TestAnn(site="classTypeParameter") S,T>
            extends @TestAnn(site="extends") Thread
            implements @TestAnn(site="implements") Runnable {

        public @TestAnn(site="field") boolean typeAnnotatedBoolean;

        public
            RedefineAnnotations.
            @TestAnn(site="inner") TypeAnnotatedTestClass
            typeAnnotatedInner;

        public
            @TestAnn(site="array4") boolean
            @TestAnn(site="array1") []
            @TestAnn(site="array2") []
            @TestAnn(site="array3") []
            typeAnnotatedArray;

        public @TestAnn(site="map1") Map
            <@TestAnn(site="map2") ? extends @TestAnn(site="map3") String,
            @TestAnn(site="map4")  List<@TestAnn(site="map5")  Object>> typeAnnotatedMap;

        public int dummy1;
        public int dummy2;
        public int dummy3;

        @TestAnn(site="return") <@TestAnn(site="methodTypeParameter") U,V> Class
            typeAnnotatedMethod(@TestAnn(site="formalParameter") TypeAnnotatedTestClass arg)
            throws @TestAnn(site="throws") ClassNotFoundException {

            @TestAnn(site="local_variable_type") int foo = 0;
            throw new ClassNotFoundException();
        }

        public void run() {}
    }
}
