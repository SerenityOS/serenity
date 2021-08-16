/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222378
 * @summary Test that ProcessingEnvironment.isPreviewEnabled works properly
 * @library /tools/javac/lib
 * @modules java.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile TestPreviewEnabled.java
 * @compile -processor TestPreviewEnabled -proc:only -source ${jdk.version} -AExpectedPreview=false                  TestSourceVersion.java
 * @compile -processor TestPreviewEnabled -proc:only -source ${jdk.version} -AExpectedPreview=true  --enable-preview TestSourceVersion.java
 */

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.annotation.processing.*;
import javax.lang.model.util.*;

/**
 * This processor checks that ProcessingEnvironment.isPreviewEnabled
 * is consistent with the compiler options.
 */
@SupportedOptions("ExpectedPreview")
public class TestPreviewEnabled extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment) {
        if (!roundEnvironment.processingOver()) {
            boolean expectedPreview =
                Boolean.valueOf(processingEnv.getOptions().get("ExpectedPreview"));
            boolean actualPreview =  processingEnv.isPreviewEnabled();
            System.out.println("Expected PreviewEnabled: " + expectedPreview +
                               "\n  actual PreviewEnabled: "  + actualPreview);
            if (expectedPreview != actualPreview)
                throw new RuntimeException();

            if (expectedPreview) {
                // Create a ProcessingEnvironment that uses the
                // default implemention of isPreviewEnabled.
                ProcessingEnvironment testEnv = new ProcessingEnvironment() {
                        @Override public Elements getElementUtils() {return null;}
                        @Override public Filer getFiler() {return null;}
                        @Override public Locale getLocale() {return null;}
                        @Override public Messager getMessager() {return null;}
                        @Override public Map<String,String> getOptions() {return null;}
                        @Override public SourceVersion getSourceVersion() {return null;}
                        @Override public Types getTypeUtils() {return null;}
                    };
                if (testEnv.isPreviewEnabled()) {
                    throw new RuntimeException("Bad true return value from default " +
                                               "ProcessingEnvironment.isPreviewEnabled.");
                }
            }
        }
        return true;
    }
}
