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
 * @bug 6411379
 * @summary NPE from JavacTrees.getPath
 * @modules jdk.compiler
 * @build T6411379
 * @compile -processor T6411379 -proc:only T6411379 T6411379.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

@SupportedAnnotationTypes("*")
public class T6411379 extends AbstractProcessor {

    public boolean process(Set<? extends TypeElement> annoElems,
                                    RoundEnvironment renv) {
        Trees trees = Trees.instance(processingEnv);
        for (TypeElement annoElem: annoElems) {
            for (Element te: renv.getRootElements()) {
                System.err.println("te: " + te);
                for (AnnotationMirror anno: te.getAnnotationMirrors()) {
                    // anno is an annotation on te, not on annoElem,
                    // so we expect the following to return null
                    // (and not give NPE)
                    checkNull(trees.getPath(annoElem, anno));
                    checkNull(trees.getTree(annoElem, anno));
                }
            }
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    public void checkNull(Object o) {
        if (o != null)
            throw new AssertionError("expected null");
    }
}
