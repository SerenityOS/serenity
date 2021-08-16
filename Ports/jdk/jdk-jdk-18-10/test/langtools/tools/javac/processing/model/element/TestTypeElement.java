/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025118
 * @summary Verify that TypeElement for interfaces does not have
 *          Modifier.DEFAULT in getModifiers()
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestTypeElement
 * @compile -processor TestTypeElement -proc:only TestTypeElement.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.tools.Diagnostic.Kind.*;

/**
 * Verify that TypeElement for interfaces does not have Modifier.DEFAULT in getModifiers().
 */
public class TestTypeElement extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (Element element : roundEnv.getRootElements()) {
                if (element.getKind().isClass() || element.getKind().isInterface()) {
                    if (element.getModifiers().contains(Modifier.DEFAULT))
                        messager.printMessage(ERROR, "Modifier.DEFAULT not expected on classes/interfaces");
                }
            }
        }
        return true;
    }

}

/**
 * Test interface to provide a default method.
 */
interface InterfaceWithDefaultMethod {
    default void quux() {}
}
