/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic.Kind;

/*
 * @test
 * @bug 6499119
 * @summary Created package-info class file modeled improperly
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile ClassProcessor.java package-info.java
 * @compile/process -cp . -processor ClassProcessor -Akind=java  java.lang.Object
 * @compile/process -cp . -processor ClassProcessor -Akind=class java.lang.Object
 */

@SupportedOptions({ "gen", "expect" })
public class ClassProcessor extends JavacTestingAbstractProcessor {
    int round = 1;

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (round == 1) {
            System.out.println("-- Round 1 --");
            createPackageFile();
        } else if (round == 2) {
            boolean found_foo_A = false;
            System.out.println("-- Round 2 --");
            for(Element e: roundEnv.getRootElements()) {
                System.out.println("ElementKind: " + e.getKind());
                System.out.println("Modifiers:   " + e.getModifiers());
                System.out.println("Annotations: " + e.getAnnotationMirrors());
                if (e.getAnnotationMirrors().toString().equals("@foo.A")) {
                    found_foo_A = true;
                    checkEqual("ElementKind", e.getKind().toString(), "PACKAGE");
                    checkEqual("Modifiers",   e.getModifiers().toString(), "[]");
                }
            }
            if (!found_foo_A)
                error("did not find @foo.A");
        }
        round++;
        return true;
    }

    private void createPackageFile() {
        String kind = processingEnv.getOptions().get("kind");

        File pkgInfo;
        if (kind.equals("java"))
            pkgInfo = new File(System.getProperty("test.src"),     "package-info.java");
        else
            pkgInfo = new File(System.getProperty("test.classes"), "foo/package-info.class");

        byte[] bytes = new byte[(int) pkgInfo.length()];
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(pkgInfo));
            in.readFully(bytes);
        } catch (IOException ioe) {
            error("Couldn't read package info file: " + ioe);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    error("InputStream closing failed: " + e);
                }
            }
        }

        OutputStream out = null;
        try {
            if (kind.equals("java"))
                out = filer.createSourceFile("foo.package-info").openOutputStream();
            else
                out = filer.createClassFile("foo.package-info").openOutputStream();
            out.write(bytes, 0, bytes.length);
        } catch (IOException ioe) {
            error("Couldn't create package info file: " + ioe);
        } finally {
            if(out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    error("OutputStream closing failed: " + e);
                }
            }
        }
    }

    private void checkEqual(String label, String actual, String expect) {
        if (!actual.equals(expect)) {
            error("Unexpected value for " + label + "; actual=" + actual + ", expected=" + expect);
        }
    }

    private void error(String msg) {
        messager.printMessage(Kind.ERROR, msg);
    }
}

