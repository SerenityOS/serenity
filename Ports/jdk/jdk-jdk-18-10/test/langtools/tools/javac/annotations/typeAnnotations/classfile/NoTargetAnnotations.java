/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.List;

import com.sun.tools.classfile.*;

/*
 * @test NoTargetAnnotations
 * @summary test that annotations with no Target meta type is emitted
 *          only once as declaration annotation
 * @modules jdk.jdeps/com.sun.tools.classfile
 */
public class NoTargetAnnotations {

    public static void main(String[] args) throws Exception {
        new NoTargetAnnotations().run();
    }

    public void run() throws Exception {
        ClassFile cf = getClassFile("NoTargetAnnotations$Test.class");
        for (Field f : cf.fields) {
            test(cf, f);
            testDeclaration(cf, f);
        }
        for (Method m: cf.methods) {
            test(cf, m);
            testDeclaration(cf, m);
        }

        countAnnotations();

        if (errors > 0)
            throw new Exception(errors + " errors found");
        System.out.println("PASSED");
    }

    ClassFile getClassFile(String name) throws IOException, ConstantPoolException {
        URL url = getClass().getResource(name);
        InputStream in = url.openStream();
        try {
            return ClassFile.read(in);
        } finally {
            in.close();
        }
    }

    /************ Helper annotations counting methods ******************/
    void test(ClassFile cf, Method m) {
        test(cf, m, Attribute.RuntimeVisibleTypeAnnotations, true);
        test(cf, m, Attribute.RuntimeInvisibleTypeAnnotations, false);
    }

    void test(ClassFile cf, Field m) {
        test(cf, m, Attribute.RuntimeVisibleTypeAnnotations, true);
        test(cf, m, Attribute.RuntimeInvisibleTypeAnnotations, false);
    }

    void testDeclaration(ClassFile cf, Method m) {
        testDecl(cf, m, Attribute.RuntimeVisibleAnnotations, true);
        testDecl(cf, m, Attribute.RuntimeInvisibleAnnotations, false);
    }

    void testDeclaration(ClassFile cf, Field m) {
        testDecl(cf, m, Attribute.RuntimeVisibleAnnotations, true);
        testDecl(cf, m, Attribute.RuntimeInvisibleAnnotations, false);
    }

    // test the result of Attributes.getIndex according to expectations
    // encoded in the method's name
    void test(ClassFile cf, Method m, String name, boolean visible) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeTypeAnnotations_attribute;
            RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute)attr;
            all += tAttr.annotations.length;
            if (visible)
                visibles += tAttr.annotations.length;
            else
                invisibles += tAttr.annotations.length;
        }
    }

    // test the result of Attributes.getIndex according to expectations
    // encoded in the method's name
    void test(ClassFile cf, Field m, String name, boolean visible) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeTypeAnnotations_attribute;
            RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute)attr;
            all += tAttr.annotations.length;
            if (visible)
                visibles += tAttr.annotations.length;
            else
                invisibles += tAttr.annotations.length;
        }
    }

    // test the result of Attributes.getIndex according to expectations
    // encoded in the method's name
    void testDecl(ClassFile cf, Method m, String name, boolean visible) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeAnnotations_attribute;
            RuntimeAnnotations_attribute tAttr = (RuntimeAnnotations_attribute)attr;
            this.declAnnotations += tAttr.annotations.length;
        }
    }

    // test the result of Attributes.getIndex according to expectations
    // encoded in the method's name
    void testDecl(ClassFile cf, Field m, String name, boolean visible) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeAnnotations_attribute;
            RuntimeAnnotations_attribute tAttr = (RuntimeAnnotations_attribute)attr;
            this.declAnnotations += tAttr.annotations.length;
        }
    }

    File compileTestFile(File f) {
        int rc = com.sun.tools.javac.Main.compile(new String[] { "-XDTA:writer", "-g", f.getPath() });
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        String path = f.getPath();
        return new File(path.substring(0, path.length() - 5) + ".class");
    }

    void countAnnotations() {
        int expected_all = expected_visibles + expected_invisibles;

        if (expected_all != all) {
            errors++;
            System.err.println("expected " + expected_all
                    + " annotations but found " + all);
        }

        if (expected_visibles != visibles) {
            errors++;
            System.err.println("expected " + expected_visibles
                    + " visibles annotations but found " + visibles);
        }

        if (expected_invisibles != invisibles) {
            errors++;
            System.err.println("expected " + expected_invisibles
                    + " invisibles annotations but found " + invisibles);
        }

        if (expected_decl != declAnnotations) {
            errors++;
            System.err.println("expected " + expected_decl
                    + " declaration annotations but found " + declAnnotations);
        }
    }

    int errors;
    int all;
    int visibles;
    int invisibles;

    int declAnnotations;

    /*********************** Test class *************************/
    static int expected_invisibles = 0;
    static int expected_visibles = 0;
    static int expected_decl = 1;

    static class Test {
        @Retention(RetentionPolicy.RUNTIME)
        @interface A {}

        @A String method() {
            return null;
        }
    }
}
