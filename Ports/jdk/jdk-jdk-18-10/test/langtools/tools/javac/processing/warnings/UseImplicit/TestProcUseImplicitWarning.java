/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6986892
 * @summary confusing warning given after errors in annotation processing
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor TestProcUseImplicitWarning
 * @clean C1 p.C2
 * @compile/fail/ref=err.out -XDrawDiagnostics -processor TestProcUseImplicitWarning -Aerror C1.java
 * @clean C1 p.C2
 * @compile/ref=warn.out     -XDrawDiagnostics -processor TestProcUseImplicitWarning         C1.java
 */

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.tools.Diagnostic.Kind.*;

@SupportedOptions("error")
public class TestProcUseImplicitWarning extends JavacTestingAbstractProcessor {

    int round = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;

        if (round == 1) {
            boolean error = options.containsKey("error");
            if (error)
                messager.printMessage(ERROR, "error generated per option");
        }

        return false;
    }

}
