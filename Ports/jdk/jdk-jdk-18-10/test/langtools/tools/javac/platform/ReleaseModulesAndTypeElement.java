/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8209865
 * @summary Verify that when reading from ct.sym, classes are only visible from modules from which
 *          they are exported.
 * @modules jdk.compiler
 * @build ReleaseModulesAndTypeElement
 * @compile -processor ReleaseModulesAndTypeElement --release 11 ReleaseModulesAndTypeElement.java
 */

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@SupportedAnnotationTypes("*")
public class ReleaseModulesAndTypeElement extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> roots, RoundEnvironment roundEnv) {
        Elements elements = processingEnv.getElementUtils();
        if (elements.getTypeElement(JX_A_P_GENERATED) == null) {
            throw new AssertionError("jx.a.p.Generated not found by unqualified search!");
        }
        ModuleElement javaBase = elements.getModuleElement("java.base");
        if (elements.getTypeElement(javaBase, JX_A_P_GENERATED) != null) {
            throw new AssertionError("jx.a.p.Generated found in java.base!");
        }
        ModuleElement javaCompiler = elements.getModuleElement("java.compiler");
        if (elements.getTypeElement(javaCompiler, JX_A_P_GENERATED) == null) {
            throw new AssertionError("jx.a.p.Generated not found in java.compiler!");
        }
        return false;
    }
    //where:
        private static final String JX_A_P_GENERATED = "javax.annotation.processing.Generated";

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}
