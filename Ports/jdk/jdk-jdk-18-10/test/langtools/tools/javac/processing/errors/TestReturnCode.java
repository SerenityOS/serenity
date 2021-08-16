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
 * @bug 6403468
 * @summary Test that an erroneous return code results from raising an error.
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.main
 * @build JavacTestingAbstractProcessor CompileFail
 * @compile TestReturnCode.java
 *
 * @compile                     -processor TestReturnCode -proc:only                                                                   Foo.java
 * @run main CompileFail ERROR  -processor TestReturnCode -proc:only                                                    -AErrorOnFirst Foo.java
 * @run main CompileFail ERROR  -processor TestReturnCode -proc:only                                      -AErrorOnLast                Foo.java
 * @run main CompileFail ERROR  -processor TestReturnCode -proc:only                                      -AErrorOnLast -AErrorOnFirst Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only                   -AExceptionOnFirst                              Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only                   -AExceptionOnFirst               -AErrorOnFirst Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only                   -AExceptionOnFirst -AErrorOnLast                Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only                   -AExceptionOnFirst -AErrorOnLast -AErrorOnFirst Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast                                                 Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast                                  -AErrorOnFirst Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast                    -AErrorOnLast                Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast                    -AErrorOnLast -AErrorOnFirst Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast -AExceptionOnFirst                              Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast -AExceptionOnFirst               -AErrorOnFirst Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast -AExceptionOnFirst -AErrorOnLast                Foo.java
 * @run main CompileFail SYSERR -processor TestReturnCode -proc:only -AExceptionOnLast -AExceptionOnFirst -AErrorOnLast -AErrorOnFirst Foo.java
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;


/**
 * This processor raises errors or throws exceptions on different
 * rounds to allow the return code to be test.
 */
@SupportedOptions({"ErrorOnFirst",
                   "ErrorOnLast",
                   "ExceptionOnFirst",
                   "ExceptionOnLast"})
public class TestReturnCode extends JavacTestingAbstractProcessor {

    private boolean errorOnFirst;
    private boolean errorOnLast;
    private boolean exceptionOnFirst;
    private boolean exceptionOnLast;

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            System.out.format("Variables: %b\t%b\t%b\t%b%n",
                              errorOnFirst,
                              errorOnLast,
                              exceptionOnFirst,
                              exceptionOnLast);
            if (errorOnFirst)
                messager.printMessage(ERROR, "Error on first round.");
            if (exceptionOnFirst)
                throw new RuntimeException("Exception on first round.");
        } else {
            if (errorOnLast)
                messager.printMessage(ERROR, "Error on last round.");
            if (exceptionOnLast)
                throw new RuntimeException("Exception on last round.");
        }
        return true;
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Set<String> keySet = processingEnv.getOptions().keySet();
        errorOnFirst      = keySet.contains("ErrorOnFirst");
        errorOnLast     = keySet.contains("ErrorOnLast");
        exceptionOnFirst  = keySet.contains("ExceptionOnFirst");
        exceptionOnLast = keySet.contains("ExceptionOnLast");
    }
}
