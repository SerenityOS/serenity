/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6380016
 * @summary Test that the constraints guaranteed by the Filer and maintained
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestNames
 * @compile -processor TestNames -proc:only TestNames.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;

import java.io.*;

/**
 * Basic tests of semantics of javax.lang.model.element.Name
 */
public class TestNames extends JavacTestingAbstractProcessor {
    private int round = 0;

    String stringStringName = "java.lang.String";
    Name stringName = null;

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        round++;
        if (!roundEnv.processingOver()) {
            boolean failed = false;

            switch(round) {
            case 1:

            TypeElement stringMirror = eltUtils.getTypeElement(stringStringName);
            stringName = stringMirror.getQualifiedName();
            Name stringPseudoName = Pseudonym.getName(stringName.toString());


            if (stringName.equals(stringPseudoName))
                failed = true;
            if (!stringName.contentEquals(stringStringName))
                failed = true;
            if (!stringName.contentEquals(stringPseudoName))
                failed = true;


            try {
                // Force another round with a new context
                PrintWriter pw = new PrintWriter(filer.createSourceFile("Foo").openWriter());
                pw.println("public class Foo {}");
                pw.close();
            } catch (IOException ioe) {
                throw new RuntimeException();
            }
            break;

            case 2:
                Name stringStringAsName = eltUtils.getName(stringStringName);
                TypeElement stringMirror2 = eltUtils.getTypeElement(stringStringName);
                Name stringName2 = stringMirror2.getQualifiedName();

                if (stringStringAsName != stringName ||
                    stringName != stringName2)
                    failed = true;
                break;

            default:
                throw new RuntimeException("Unexpected round " + round);
            }

            if (failed)
                throw new RuntimeException("Invalid name equality checks.");
        }
        return true;
    }

    private static class Pseudonym implements Name {
        private String name;

        private Pseudonym(String name) {
            this.name = name;
        }

        public static Pseudonym getName(String name) {
            return new Pseudonym(name);
        }

        public boolean contentEquals(CharSequence cs) {
            return name.contentEquals(cs);
        }

        public char charAt(int index) {
            return name.charAt(index);
        }

        public int length() {
            return name.length();
        }

        public CharSequence subSequence(int start, int end) {
            return name.subSequence(start, end);
        }

        public String toString() {
            return name;
        }
    }
}
