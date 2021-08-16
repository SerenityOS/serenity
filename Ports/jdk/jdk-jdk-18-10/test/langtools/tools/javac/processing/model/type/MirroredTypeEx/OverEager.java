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
 * @bug     6362178
 * @summary MirroredType[s]Exception shouldn't be created too eagerly
 * @author  Scott Seligman
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor
 * @compile -g OverEager.java
 * @compile -processor OverEager -proc:only OverEager.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;

@SupportedAnnotationTypes("IAm")
@IAm(OverEager.class)
public class OverEager extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annoTypes,
                           RoundEnvironment round) {
        if (!round.processingOver())
            doit(annoTypes, round);
        return true;
    }

    private void doit(Set<? extends TypeElement> annoTypes,
                      RoundEnvironment round) {
        for (TypeElement t : typesIn(round.getRootElements())) {
            IAm anno = t.getAnnotation(IAm.class);
            if (anno != null)
                checkAnno(anno);
        }
    }

    private void checkAnno(IAm anno) {
        try {
            anno.value();
            throw new AssertionError();
        } catch (MirroredTypeException e) {
            System.out.println("Looking for checkAnno in this stack trace:");
            e.printStackTrace();
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getMethodName() == "checkAnno")
                    return;
            }
            throw new AssertionError();
        }
    }
}

@interface IAm {
    Class<?> value();
}
