/*
 * Copyright (c) 2018, Google Inc. All rights reserved.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("*")
public class Processor extends JavacTestingAbstractProcessor {

    boolean first = true;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (first) {
            // Annotations are present on the initial package-info loaded from the classpath.
            PackageElement p = processingEnv.getElementUtils().getPackageElement("p");
            if (p.getAnnotationMirrors().isEmpty()) {
                throw new AssertionError(
                        "expected package annotations: " + p.getAnnotationMirrors());
            }
            // Overwrite the package-info with a new unannotated package-info.
            try (OutputStream os =
                    processingEnv
                            .getFiler()
                            .createSourceFile("p.package-info")
                            .openOutputStream()) {
                os.write("package p;".getBytes(UTF_8));
            } catch (IOException e) {
                throw new IOError(e);
            }
            first = false;
        }
        // The package-info's symbol should be reset between rounds, and when annotation
        // processing is over the package-info should be unannotated.
        PackageElement p = processingEnv.getElementUtils().getPackageElement("p");
        if (roundEnv.processingOver()) {
            if (!p.getAnnotationMirrors().isEmpty()) {
                throw new AssertionError(
                        "expected no package annotations: " + p.getAnnotationMirrors());
            }
        }
        return false;
    }
}
