/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

public class Helper {

    enum ContentVars {

        IMPORTCONTAINERSTMTS("\nimport java.lang.annotation.Repeatable;\n"),
        IMPORTDEPRECATED("import java.lang.Deprecated;\n"),
        IMPORTDOCUMENTED("import java.lang.annotation.Documented;\n"),
        IMPORTINHERITED("import java.lang.annotation.Inherited;\n"),
        IMPORTRETENTION("import java.lang.annotation.Retention;\n"
        + "\nimport java.lang.annotation.RetentionPolicy;\n"),
        IMPORTSTMTS("import java.lang.annotation.*;\n"),
        IMPORTEXPECTED("import expectedFiles.*;\n"),
        REPEATABLE("\n@Repeatable(FooContainer.class)\n"),
        CONTAINER("@interface FooContainer {\n" + "  Foo[] value();\n}\n"),
        BASE("@interface Foo {int value() default Integer.MAX_VALUE;}\n"),
        BASEANNO("@Foo(0)"),
        LEGACYCONTAINER("@FooContainer(value = {@Foo(1), @Foo(2)})\n"),
        REPEATABLEANNO("\n@Foo(1) @Foo(2)"),
        DEPRECATED("\n@Deprecated"),
        DOCUMENTED("\n@Documented"),
        INHERITED("\n@Inherited"),
        TARGET("\n@Target(#VAL)\n"),
        RETENTION("@Retention(RetentionPolicy.#VAL)\n"),
        RETENTIONRUNTIME("@Retention(RetentionPolicy.RUNTIME)\n");
        private String val;

        private ContentVars(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }

    // Create and compile FileObject using values for className and contents
    public static boolean compileCode(String className, String contents,
            DiagnosticCollector<JavaFileObject> diagnostics) {
        boolean ok = false;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("can't get javax.tools.JavaCompiler!");
        }

        JavaFileObject file = getFile(className, contents);
        Iterable<? extends JavaFileObject> compilationUnit = Arrays.asList(file);

        CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnit);
        ok = task.call();
        return ok;
    }
    // Compile a list of FileObjects
    // Used when packages are needed and classes need to be loaded at runtime
    static File destDir = new File(System.getProperty("user.dir"));

    public static boolean compileCode(DiagnosticCollector<JavaFileObject> diagnostics, Iterable<? extends JavaFileObject> files) {
        return compileCode(diagnostics, files, null);
    }

    public static boolean compileCode(DiagnosticCollector<JavaFileObject> diagnostics, Iterable<? extends JavaFileObject> files, Iterable<String> options) {
        boolean ok = false;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("can't get javax.tools.JavaCompiler!");
        }

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            // Assuming filesCount can maximum be 2 and if true, one file is package-info.java
            if (isPkgInfoPresent(files)) {
                JavacTask task = (JavacTask) compiler.getTask(null, fm, diagnostics, options, null, files);
                try {
                    fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(destDir));
                    task.generate();
                } catch (IOException ioe) {
                    throw new RuntimeException("Compilation failed for package level tests", ioe);
                }
                int err = 0;
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    if(d.getKind() == Diagnostic.Kind.ERROR) {
                      err++;
                    }
                }
                ok = (err == 0);
            } else {
                CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, files);
                ok = task.call();
            }
            return ok;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    static private boolean isPkgInfoPresent(Iterable<? extends JavaFileObject> files) {
        Iterator<? extends JavaFileObject> itr = files.iterator();
        while (itr.hasNext()) {
            String name = itr.next().getName();
            if (name.contains("package-info")) {
                return true;
            }
        }
        return false;
    }
    /* String template where /*<TYPE>*/ /*gets replaced by repeating anno
     * Used to generate test src for combo tests
     *   - BasicSyntaxCombo.java
     *   - TargetAnnoCombo.java
     */

    public static final String template =
            "/*PACKAGE*/\n"
            + "//pkg test;\n\n"
            + "/*ANNODATA*/\n" // import statements, declaration of Foo/FooContainer
            + "/*TYPE*/ //class\n"
            + "class #ClassName {\n"
            + "  /*FIELD*/ //instance var\n"
            + "  public int x = 0;\n\n"
            + "  /*FIELD*/ //Enum constants\n"
            + "  TestEnum testEnum;\n\n"
            + "  /*FIELD*/ // Static field\n"
            + "  public static int num;\n\n"
            + "  /*STATIC_INI*/\n"
            + "  static { \n" + "num = 10; \n  }\n\n"
            + "  /*CONSTRUCTOR*/\n"
            + "  #ClassName() {}\n\n"
            + "  /*INSTANCE_INI*/\n"
            + "  { \n x = 10; \n }"
            + "  /*INNER_CLASS*/\n"
            + "  class innerClass {}\n"
            + "  /*METHOD*/\n"
            + "  void bar(/*PARAMETER*/ int baz) {\n"
            + "    /*LOCAL_VARIABLE*/\n"
            + "    int y = 0;\n"
            + "  }\n"
            + "}\n\n"
            + "/*TYPE*/ //Enum\n"
            + "enum TestEnum {}\n\n"
            + "/*TYPE*/ //Interface\n"
            + "interface TestInterface {}\n\n"
            + "/*TYPE*/\n"
            + "/*ANNOTATION_TYPE*/\n"
            + "@interface TestAnnotationType{}\n"
            + "class TestPkg {}\n"
            + "class TestTypeAnno </*TYPE_PARAMETER*/ T extends Object> {\n"
            + "  String /*TYPE_USE*/[] arr;\n"
            + "}";

    static JavaFileObject getFile(String name, String code) {
        JavaFileObject o = null;
        try {
            o = new JavaStringFileObject(name, code);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return o;
    }

    static class JavaStringFileObject extends SimpleJavaFileObject {

        final String theCode;

        public JavaStringFileObject(String fileName, String theCode) throws URISyntaxException {
            super(new URI("string:///" + fileName.replace('.', '/') + ".java"), Kind.SOURCE);
            this.theCode = theCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return theCode;
        }
    }
}
