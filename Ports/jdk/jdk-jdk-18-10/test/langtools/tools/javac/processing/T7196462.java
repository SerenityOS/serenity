/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7196462
 * @summary JavacProcessingEnvironment should tolerate BasicJavacTask
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor T7196462
 * @compile/process -processor T7196462 T7196462.java
 */

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import com.sun.source.util.*;

public class T7196462 extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annos,RoundEnvironment rEnv) {
        JavacTask t = JavacTask.instance(processingEnv);
        System.err.println(t);
        return true;
    }
}
