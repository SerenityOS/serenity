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
 * @bug 6380018 6449798
 * @summary Test Filer.getResource
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build  JavacTestingAbstractProcessor TestGetResource
 * @compile -processor TestGetResource -proc:only -Aphase=write TestGetResource.java
 * @compile -processor TestGetResource -proc:only -Aphase=read  TestGetResource.java
 */

import java.util.Set;
import java.util.Map;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Test basic functionality of the Filer.getResource method.  On the
 * first run of the annotation processor, write out a resource file
 * and on the second run read it in.
 */
@SupportedOptions("phase")
public class TestGetResource extends JavacTestingAbstractProcessor {
    private static String CONTENTS = "Hello World.";
    private static String PKG = "";
    private static String RESOURCE_NAME = "Resource1";

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                String phase = options.get("phase");

                if (phase.equals("write")) {
                    PrintWriter pw =
                        new PrintWriter(filer.createResource(CLASS_OUTPUT, PKG, RESOURCE_NAME).openWriter());
                    pw.print(CONTENTS);
                    pw.close();
                } else if (phase.equals("read")) {
                    String contents = filer.getResource(CLASS_OUTPUT,
                                                       PKG,
                                                       RESOURCE_NAME).getCharContent(false).toString();
                    if (!contents.equals(CONTENTS))
                        throw new RuntimeException("Expected \n\t" + CONTENTS +
                                                   "\nbut instead got \n\t" +
                                                   contents);
                    // Now try to open the file for writing
                    filer.createResource(CLASS_OUTPUT,
                                         PKG,
                                         RESOURCE_NAME);
                } else {
                    throw new RuntimeException("Unexpected phase: " + phase);
                }
            }
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return false;
    }
}
