/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160454
 * @summary JSR269 jigsaw update: javax.lang.model.element.ModuleElement.getDirectives() causes NPE on unnamed modules
 * @modules
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @compile NPEGetDirectivesTest.java
 * @compile -processor NPEGetDirectivesTest NPEGetDirectivesTest.java
 */

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;

import java.util.Set;

import com.sun.tools.javac.code.Directive.RequiresDirective;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.util.Assert;

import static com.sun.tools.javac.code.Directive.RequiresFlag.MANDATED;

@SupportedAnnotationTypes("*")
public class NPEGetDirectivesTest extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e: roundEnv.getRootElements()) {
            Element m = e.getEnclosingElement();
            while (!(m instanceof ModuleElement)) {
                m = m.getEnclosingElement();
            }
            ((ModuleSymbol)m).getDirectives();
            RequiresDirective requiresDirective = ((ModuleSymbol)m).requires.head;
            Assert.check(requiresDirective.getDependency().getQualifiedName().toString().equals("java.base"));
            Assert.check(requiresDirective.flags.contains(MANDATED));
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
