/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004727
 * @summary javac should generate method parameters correctly.
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.model
 *          jdk.compiler/com.sun.tools.javac.util
 */
// key: opt.arg.parameters
import com.sun.tools.classfile.*;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.io.*;
import javax.lang.model.element.*;
import java.util.*;

public class MethodParametersTest {

    static final String Foo_name = "Foo";
    static final String Foo_contents =
        "public class Foo {\n" +
        "  Foo() {}\n" +
        "  void foo0() {}\n" +
        "  void foo2(int j, int k) {}\n" +
        "}";
    static final String Bar_name = "Bar";
    static final String Bar_contents =
        "public class Bar {\n" +
        "  Bar(int i) {}" +
        "  Foo foo() { return new Foo(); }\n" +
        "}";
    static final String Baz_name = "Baz";
    static final String Baz_contents =
        "public class Baz {\n" +
        "  int baz;" +
        "  Baz(int i) {}" +
        "}";
    static final String Qux_name = "Qux";
    static final String Qux_contents =
        "public class Qux extends Baz {\n" +
        "  Qux(int i) { super(i); }" +
        "}";
    static final File classesdir = new File("methodparameters");

    public static void main(String... args) throws Exception {
        new MethodParametersTest().run();
    }

    void run() throws Exception {
        classesdir.mkdir();
        final File Foo_java =
            writeFile(classesdir, Foo_name + ".java", Foo_contents);
        final File Bar_java =
            writeFile(classesdir, Bar_name + ".java", Bar_contents);
        final File Baz_java =
            writeFile(classesdir, Baz_name + ".java", Baz_contents);
        System.err.println("Test compile with -parameter");
        compile("-parameters", "-d", classesdir.getPath(), Foo_java.getPath());
        // First test: make sure javac doesn't choke to death on
        // MethodParameter attributes
        System.err.println("Test compile with classfile containing MethodParameter attributes");
        compile("-parameters", "-d", classesdir.getPath(),
                "-cp", classesdir.getPath(), Bar_java.getPath());
        System.err.println("Examine class foo");
        checkFoo();
        checkBar();
        System.err.println("Test debug information conflict");
        compile("-g", "-parameters", "-d", classesdir.getPath(),
                "-cp", classesdir.getPath(), Baz_java.getPath());
        System.err.println("Introducing debug information conflict");
        Baz_java.delete();
        modifyBaz(false);
        System.err.println("Checking language model");
        inspectBaz();
        System.err.println("Permuting attributes");
        modifyBaz(true);
        System.err.println("Checking language model");
        inspectBaz();

        if(0 != errors)
            throw new Exception("MethodParameters test failed with " +
                                errors + " errors");
    }

    void inspectBaz() throws Exception {
        final File Qux_java =
            writeFile(classesdir, Qux_name + ".java", Qux_contents);
        final String[] args = { "-parameters", "-d",
                                classesdir.getPath(),
                                "-cp", classesdir.getPath(),
                                Qux_java.getPath() };
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        // We need to be able to crack open javac and look at its data
        // structures.  We'll rig up a compiler instance, but keep its
        // Context, thus allowing us to get at the ClassReader.
        Context context = new Context();
        Main comp =  new Main("javac", pw);
        JavacFileManager.preRegister(context);

        // Compile Qux, which uses Baz.
        comp.compile(args, context);
        pw.close();
        final String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);

        // Now get the class finder, construct a name for Baz, and load it.
        com.sun.tools.javac.code.ClassFinder cf =
            com.sun.tools.javac.code.ClassFinder.instance(context);
        Name name = Names.instance(context).fromString(Baz_name);
        Symtab syms = Symtab.instance(context);

        // Now walk down the language model and check the name of the
        // parameter.
        final Element baz = cf.loadClass(syms.unnamedModule, name);
        for (Element e : baz.getEnclosedElements()) {
            if (e instanceof ExecutableElement) {
                final ExecutableElement ee = (ExecutableElement) e;
                final List<? extends VariableElement> params =
                    ee.getParameters();
                if (1 != params.size())
                    throw new Exception("Classfile Baz badly formed: wrong number of methods");
                final VariableElement param = params.get(0);
                if (!param.getSimpleName().contentEquals("baz")) {
                    errors++;
                    System.err.println("javac did not correctly resolve the metadata conflict, parameter's name reads as " + param.getSimpleName());
                } else
                    System.err.println("javac did correctly resolve the metadata conflict");
            }
        }
    }

    void modifyBaz(boolean flip) throws Exception {
        final File Baz_class = new File(classesdir, Baz_name + ".class");
        final ClassFile baz = ClassFile.read(Baz_class);
        final int ind = baz.constant_pool.getUTF8Index("baz");
        MethodParameters_attribute mpattr = null;
        int mpind = 0;
        Code_attribute cattr = null;
        int cind = 0;

        // Find the indexes of the MethodParameters and the Code attributes
        if (baz.methods.length != 1)
            throw new Exception("Classfile Baz badly formed: wrong number of methods");
        if (!baz.methods[0].getName(baz.constant_pool).equals("<init>"))
            throw new Exception("Classfile Baz badly formed: method has name " +
                                baz.methods[0].getName(baz.constant_pool));
        for (int i = 0; i < baz.methods[0].attributes.attrs.length; i++) {
            if (baz.methods[0].attributes.attrs[i] instanceof
                MethodParameters_attribute) {
                mpattr = (MethodParameters_attribute)
                    baz.methods[0].attributes.attrs[i];
                mpind = i;
            } else if (baz.methods[0].attributes.attrs[i] instanceof
                       Code_attribute) {
                cattr = (Code_attribute) baz.methods[0].attributes.attrs[i];
                cind = i;
            }
        }
        if (null == mpattr)
            throw new Exception("Classfile Baz badly formed: no method parameters info");
        if (null == cattr)
            throw new Exception("Classfile Baz badly formed: no local variable table");

        int flags = mpattr.method_parameter_table[0].flags;

        // Alter the MethodParameters attribute, changing the name of
        // the parameter from i to baz.  This requires Black Magic...
        //
        // The (well-designed) classfile library (correctly) does not
        // allow us to mess around with the attribute data structures,
        // or arbitrarily generate new ones.
        //
        // Instead, we install a new subclass of Attribute that
        // hijacks the Visitor pattern and outputs the sequence of
        // bytes that we want.  This only works in this particular
        // instance, because we know we'll only every see one kind of
        // visitor.
        //
        // If anyone ever changes the makeup of the Baz class, or
        // tries to install some kind of visitor that gets run prior
        // to serialization, this will break.
        baz.methods[0].attributes.attrs[mpind] =
            new Attribute(mpattr.attribute_name_index,
                          mpattr.attribute_length) {
                public <R, D> R accept(Visitor<R, D> visitor, D data) {
                    if (data instanceof ByteArrayOutputStream) {
                        ByteArrayOutputStream out =
                            (ByteArrayOutputStream) data;
                        out.write(1);
                        out.write((ind >> 8) & 0xff);
                        out.write(ind & 0xff);
                        out.write((flags >> 24) & 0xff);
                        out.write((flags >> 16) & 0xff);
                        out.write((flags >> 8) & 0xff);
                        out.write(flags & 0xff);
                    } else
                        throw new RuntimeException("Output stream is of type " + data.getClass() + ", which is not handled by this test.  Update the test and it should work.");
                    return null;
                }
            };

        // Flip the code and method attributes.  This is for checking
        // that order doesn't matter.
        if (flip) {
            baz.methods[0].attributes.attrs[mpind] = cattr;
            baz.methods[0].attributes.attrs[cind] = mpattr;
        }

        new ClassWriter().write(baz, Baz_class);
    }

    // Run a bunch of structural tests on foo to make sure it looks right.
    void checkFoo() throws Exception {
        final File Foo_class = new File(classesdir, Foo_name + ".class");
        final ClassFile foo = ClassFile.read(Foo_class);
        for (int i = 0; i < foo.methods.length; i++) {
            System.err.println("Examine method Foo." + foo.methods[i].getName(foo.constant_pool));
            if (foo.methods[i].getName(foo.constant_pool).equals("foo2")) {
                for (int j = 0; j < foo.methods[i].attributes.attrs.length; j++)
                    if (foo.methods[i].attributes.attrs[j] instanceof
                        MethodParameters_attribute) {
                        MethodParameters_attribute mp =
                            (MethodParameters_attribute)
                            foo.methods[i].attributes.attrs[j];
                        System.err.println("Foo.foo2 should have 2 parameters: j and k");
                        if (2 != mp.method_parameter_table_length)
                            error("expected 2 method parameter entries in foo2, got " +
                                  mp.method_parameter_table_length);
                        else if (!foo.constant_pool.getUTF8Value(mp.method_parameter_table[0].name_index).equals("j"))
                            error("expected first parameter to foo2 to be \"j\", got \"" +
                                  foo.constant_pool.getUTF8Value(mp.method_parameter_table[0].name_index) +
                                  "\" instead");
                        else if  (!foo.constant_pool.getUTF8Value(mp.method_parameter_table[1].name_index).equals("k"))
                            error("expected first parameter to foo2 to be \"k\", got \"" +
                                  foo.constant_pool.getUTF8Value(mp.method_parameter_table[1].name_index) +
                                  "\" instead");
                    }
            }
            else if (foo.methods[i].getName(foo.constant_pool).equals("<init>")) {
                for (int j = 0; j < foo.methods[i].attributes.attrs.length; j++) {
                    if (foo.methods[i].attributes.attrs[j] instanceof
                        MethodParameters_attribute)
                        error("Zero-argument constructor shouldn't have MethodParameters");
                }
            }
            else if (foo.methods[i].getName(foo.constant_pool).equals("foo0")) {
                for (int j = 0; j < foo.methods[i].attributes.attrs.length; j++)
                    if (foo.methods[i].attributes.attrs[j] instanceof
                        MethodParameters_attribute)
                        error("Zero-argument method shouldn't have MethodParameters");
            }
            else
                error("Unknown method " + foo.methods[i].getName(foo.constant_pool) + " showed up in class Foo");
        }
    }

    // Run a bunch of structural tests on Bar to make sure it looks right.
    void checkBar() throws Exception {
        final File Bar_class = new File(classesdir, Bar_name + ".class");
        final ClassFile bar = ClassFile.read(Bar_class);
        for (int i = 0; i < bar.methods.length; i++) {
            System.err.println("Examine method Bar." + bar.methods[i].getName(bar.constant_pool));
            if (bar.methods[i].getName(bar.constant_pool).equals("<init>")) {
                for (int j = 0; j < bar.methods[i].attributes.attrs.length; j++)
                    if (bar.methods[i].attributes.attrs[j] instanceof
                        MethodParameters_attribute) {
                        MethodParameters_attribute mp =
                            (MethodParameters_attribute)
                            bar.methods[i].attributes.attrs[j];
                        System.err.println("Bar constructor should have 1 parameter: i");
                        if (1 != mp.method_parameter_table_length)
                            error("expected 1 method parameter entries in constructor, got " +
                                  mp.method_parameter_table_length);
                        else if (!bar.constant_pool.getUTF8Value(mp.method_parameter_table[0].name_index).equals("i"))
                            error("expected first parameter to foo2 to be \"i\", got \"" +
                                  bar.constant_pool.getUTF8Value(mp.method_parameter_table[0].name_index) +
                                  "\" instead");
                    }
            }
            else if (bar.methods[i].getName(bar.constant_pool).equals("foo")) {
                for (int j = 0; j < bar.methods[i].attributes.attrs.length; j++) {
                    if (bar.methods[i].attributes.attrs[j] instanceof
                        MethodParameters_attribute)
                        error("Zero-argument constructor shouldn't have MethodParameters");
                }
            }
        }
    }

    String compile(String... args) throws Exception {
        System.err.println("compile: " + Arrays.asList(args));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);
        if (rc != 0)
            error("compilation failed, rc=" + rc);
        return out;
    }

    File writeFile(File dir, String path, String body) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        FileWriter out = new FileWriter(f);
        out.write(body);
        out.close();
        return f;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
