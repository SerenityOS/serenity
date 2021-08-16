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
 * @bug 8161501
 * @summary JSR269 jigsaw update: javax.lang.model.element.ModuleElement.getEnclosedElements() on unnamed module with unnamed package
 * @compile UnnamedModuleUnnamedPackageTest.java
 * @compile -processor UnnamedModuleUnnamedPackageTest UnnamedModuleUnnamedPackageTest.java EmptyClass.java
 */

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;

import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("*")
public class UnnamedModuleUnnamedPackageTest extends AbstractProcessor {
    static final Set<String> expected = new HashSet<>(Arrays.asList("unnamed package", "pkg1"));

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e: roundEnv.getRootElements()) {
            Element m = e.getEnclosingElement();
            while (!(m instanceof ModuleElement)) {
                m = m.getEnclosingElement();
            }
            Set<String> found = m.getEnclosedElements().stream()
                .map(p -> ((PackageElement)p).isUnnamed() ?
                                        "unnamed package" :
                                        ((PackageElement)p).getQualifiedName().toString())
                .collect(Collectors.toSet());
            if (!Objects.equals(expected, found)) {
                System.err.println("expected: " + expected);
                System.err.println("found: " + found);
                throw new AssertionError("unexpected packages found");
            }
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
