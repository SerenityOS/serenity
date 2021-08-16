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

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

// This processor generates the missing annotation types of the form AutoAnnotation_*
// thereby making sure annotation processing doesn't abort/crash on account of that.
@SupportedAnnotationTypes("*")
public class Processor extends JavacTestingAbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!roundEnv.processingOver()) {
      for (Element element : roundEnv.getRootElements()) {
        String elementName = element.getSimpleName().toString();
        if (elementName.startsWith("AutoAnnotation_")) {
          continue;
        }
        String name = "AutoAnnotation_" + elementName;
        JavaFileObject jfo;
        try {
          jfo = processingEnv.getFiler().createSourceFile(name, element);
        } catch (IOException e) {
          throw new IOError(e);
        }
        try (OutputStream os = jfo.openOutputStream()) {
          String output = String.format("public @interface %s {}", name);
          os.write(output.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
          throw new IOError(e);
        }
      }
    }
    return false;
  }
}