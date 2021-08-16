/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;
import java.io.*;
import java.net.URL;
import java.util.List;

import com.sun.tools.classfile.*;
import java.util.ArrayList;

public class ClassfileTestHelper {
    int expected_tinvisibles = 0;
    int expected_tvisibles = 0;
    int expected_invisibles = 0;
    int expected_visibles = 0;
    List<String> extraOptions = List.of();

    //Makes debugging much easier. Set to 'false' for less output.
    public Boolean verbose = true;
    void println(String msg) { if (verbose) System.err.println(msg); }
    void print(String msg) { if (verbose) System.err.print(msg); }

    File writeTestFile(String fname, String source) throws IOException {
      File f = new File(fname);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.println(source);
        out.close();
        return f;
    }

    File compile(File f) {
        List<String> options = new ArrayList<>(List.of("-g", f.getPath()));
        options.addAll(extraOptions);
        int rc = com.sun.tools.javac.Main.compile(options.toArray(new String[0]));
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        String path = f.getPath();
        return new File(path.substring(0, path.length() - 5) + ".class");
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

    ClassFile getClassFile(URL url) throws IOException, ConstantPoolException {
        InputStream in = url.openStream();
        try {
            return ClassFile.read(in);
        } finally {
            in.close();
        }
    }

    /************ Helper annotations counting methods ******************/
    void test(ClassFile cf) {
        test("CLASS",cf, null, null, Attribute.RuntimeVisibleTypeAnnotations, true);
        test("CLASS",cf, null, null, Attribute.RuntimeInvisibleTypeAnnotations, false);
        //RuntimeAnnotations since one annotation can result in two attributes.
        test("CLASS",cf, null, null, Attribute.RuntimeVisibleAnnotations, true);
        test("CLASS",cf, null, null, Attribute.RuntimeInvisibleAnnotations, false);
    }

    void test(ClassFile cf, Field f, Boolean local) {
        if (!local) {
            test("FIELD",cf, f, null, Attribute.RuntimeVisibleTypeAnnotations, true);
            test("FIELD",cf, f, null, Attribute.RuntimeInvisibleTypeAnnotations, false);
            test("FIELD",cf, f, null, Attribute.RuntimeVisibleAnnotations, true);
            test("FIELD",cf, f, null, Attribute.RuntimeInvisibleAnnotations, false);
        } else {
            test("CODE",cf, f, null, Attribute.RuntimeVisibleTypeAnnotations, true);
            test("CODE",cf, f, null, Attribute.RuntimeInvisibleTypeAnnotations, false);
            test("CODE",cf, f, null, Attribute.RuntimeVisibleAnnotations, true);
            test("CODE",cf, f, null, Attribute.RuntimeInvisibleAnnotations, false);
        }
    }

    void test(ClassFile cf, Field f) {
        test(cf, f, false);
    }

    // 'local' determines whether to look for annotations in code attribute or not.
    void test(ClassFile cf, Method m, Boolean local) {
        if (!local) {
            test("METHOD",cf, null, m, Attribute.RuntimeVisibleTypeAnnotations, true);
            test("METHOD",cf, null, m, Attribute.RuntimeInvisibleTypeAnnotations, false);
            test("METHOD",cf, null, m, Attribute.RuntimeVisibleAnnotations, true);
            test("METHOD",cf, null, m, Attribute.RuntimeInvisibleAnnotations, false);
        } else  {
            test("MCODE",cf, null, m, Attribute.RuntimeVisibleTypeAnnotations, true);
            test("MCODE",cf, null, m, Attribute.RuntimeInvisibleTypeAnnotations, false);
            test("MCODE",cf, null, m, Attribute.RuntimeVisibleAnnotations, true);
            test("MCODE",cf, null, m, Attribute.RuntimeInvisibleAnnotations, false);
        }
    }

    // default to not looking in code attribute
    void test(ClassFile cf, Method m ) {
        test(cf, m, false);
    }

    // Test the result of Attributes.getIndex according to expectations
    // encoded in the class/field/method name; increment annotations counts.
    void test(String ttype, ClassFile cf, Field f, Method m, String annName, boolean visible) {
        String testtype = ttype;
        String name = null;
        int index = -1;
        Attribute attr = null;
        Code_attribute cAttr = null;
        boolean isTAattr = annName.contains("TypeAnnotations");
        try {
            switch(testtype) {
                case "FIELD":
                    name = f.getName(cf.constant_pool);
                    index = f.attributes.getIndex(cf.constant_pool, annName);
                    if(index!= -1)
                        attr = f.attributes.get(index);
                    break;
                case "CODE":
                    name = f.getName(cf.constant_pool);
                    //fetch index of and code attribute and annotations from code attribute.
                    index = cf.attributes.getIndex(cf.constant_pool, Attribute.Code);
                    if(index!= -1) {
                        attr = cf.attributes.get(index);
                        assert attr instanceof Code_attribute;
                        cAttr = (Code_attribute)attr;
                        index = cAttr.attributes.getIndex(cf.constant_pool, annName);
                        if(index!= -1)
                            attr = cAttr.attributes.get(index);
                    }
                    break;
                case "METHOD":
                    name = m.getName(cf.constant_pool);
                    index = m.attributes.getIndex(cf.constant_pool, annName);
                    if(index!= -1)
                        attr = m.attributes.get(index);
                    break;
                case "MCODE":
                    name = m.getName(cf.constant_pool);
                    //fetch index of and code attribute and annotations from code attribute.
                    index = m.attributes.getIndex(cf.constant_pool, Attribute.Code);
                    if(index!= -1) {
                        attr = m.attributes.get(index);
                        assert attr instanceof Code_attribute;
                        cAttr = (Code_attribute)attr;
                        index = cAttr.attributes.getIndex(cf.constant_pool, annName);
                        if(index!= -1)
                            attr = cAttr.attributes.get(index);
                    }
                    break;
                default:
                    name = cf.getName();
                    index = cf.attributes.getIndex(cf.constant_pool, annName);
                    if(index!= -1) attr = cf.attributes.get(index);
            }
        } catch(ConstantPoolException cpe) { cpe.printStackTrace(); }

        if (index != -1) {
            if(isTAattr) { //count RuntimeTypeAnnotations
                RuntimeTypeAnnotations_attribute tAttr =
                        (RuntimeTypeAnnotations_attribute)attr;
                println(testtype + ": " + name + ", " + annName + ": " +
                        tAttr.annotations.length );
                if (tAttr.annotations.length > 0) {
                    for (int i = 0; i < tAttr.annotations.length; i++) {
                        println("  types:" + tAttr.annotations[i].position.type);
                    }
                } else {
                    println("");
                }
                allt += tAttr.annotations.length;
                if (visible)
                    tvisibles += tAttr.annotations.length;
                else
                    tinvisibles += tAttr.annotations.length;
            } else {
                RuntimeAnnotations_attribute tAttr =
                        (RuntimeAnnotations_attribute)attr;
                println(testtype + ": " + name + ", " + annName + ": " +
                        tAttr.annotations.length );
                all += tAttr.annotations.length;
                if (visible)
                    visibles += tAttr.annotations.length;
                else
                    invisibles += tAttr.annotations.length;
            }
        }
    }

    void countAnnotations() {
        errors=0;
        int expected_allt = expected_tvisibles + expected_tinvisibles;
        int expected_all = expected_visibles + expected_invisibles;

        if (expected_allt != allt) {
            errors++;
            System.err.println("Failure: expected " + expected_allt +
                    " type annotations but found " + allt);
        }
        if (expected_all != all) {
            errors++;
            System.err.println("Failure: expected " + expected_all +
                    " annotations but found " + all);
        }
        if (expected_tvisibles != tvisibles) {
            errors++;
            System.err.println("Failure: expected " + expected_tvisibles +
                    " typevisible annotations but found " + tvisibles);
        }

        if (expected_tinvisibles != tinvisibles) {
            errors++;
            System.err.println("Failure: expected " + expected_tinvisibles +
                    " typeinvisible annotations but found " + tinvisibles);
        }
        allt=0;
        tvisibles=0;
        tinvisibles=0;
        all=0;
        visibles=0;
        invisibles=0;
    }

    int errors;
    int allt;
    int tvisibles;
    int tinvisibles;
    int all;
    int visibles;
    int invisibles;
}
