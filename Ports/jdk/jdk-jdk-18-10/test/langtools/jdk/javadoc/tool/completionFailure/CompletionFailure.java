/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4670772 6328529
 * @summary Completion failures should be ignored in javadoc.
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */


import java.io.PrintWriter;
import java.util.*;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class CompletionFailure implements Doclet {
    public static void main(String[] args) {
        String[] toolargs = {
            "-doclet", "CompletionFailure",
            "-docletpath", System.getProperty("test.classes", "."),
            "pkg"
        };
        // run javadoc on package pkg
        if (jdk.javadoc.internal.tool.Main.execute(toolargs,
                new PrintWriter(System.err)) != 0)
            throw new Error();
    }

    public boolean run(DocletEnvironment root) {
        Set<TypeElement> classes = ElementFilter.typesIn(root.getIncludedElements());
        if (classes.size() != 1)
            throw new Error("1 " + Arrays.asList(classes));
        return true;
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        // do nothing
    }
}
