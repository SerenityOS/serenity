/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8073844
 * @summary If an error is produced by an annotation processor, the code should not be Attred, \
 *          unless requested
 * @modules jdk.compiler
 * @library /tools/javac/lib
 * @build StopAfterError JavacTestingAbstractProcessor
 * @compile/fail/ref=StopAfterError.out -XDrawDiagnostics -processor StopAfterError StopAfterErrorAux.java
 * @compile/fail/ref=StopAfterError.out -XDshould-stop.ifError=PROCESS -XDrawDiagnostics -processor StopAfterError StopAfterErrorAux.java
 * @compile/fail/ref=StopAfterErrorContinue.out -XDshould-stop.ifError=ATTR -XDrawDiagnostics -processor StopAfterError StopAfterErrorAux.java
 */

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

public class StopAfterError extends JavacTestingAbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Stop!");
        }
        return false;
    }
}
