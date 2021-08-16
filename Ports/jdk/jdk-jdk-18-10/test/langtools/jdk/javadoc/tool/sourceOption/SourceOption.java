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
 * @bug     6507179
 * @summary Ensure that "-source" option isn't ignored.
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @run main/fail SourceOption 7
 * @run main      SourceOption 9
 * @run main      SourceOption
 */

/*
 * In order to test whether or not the -source option is working
 * correctly, this test tries to parse source code that contains
 * a feature that is not available in at least one of the currently
 * supported previous versions.
 *
 * Parsing such code should be expected to fail; if the action
 * passes, that means the -source option is (incorrectly) ineffective.
 *
 * Additional actions are performed to ensure that the source
 * provided is valid for the current release of the JDK.
 *
 * As support for older versions of the platform are dropped, the
 * source code (currently p/LambdaConstructTest.java) will need to
 * be updated with a more recent feature.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Doclet.Option;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class SourceOption implements Doclet {

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();
        params.add("-sourcepath");
        params.add(System.getProperty("test.src"));
        params.add("-docletpath");
        params.add(System.getProperty("test.classes"));
        params.add("-doclet");
        params.add("SourceOption");
        if ((args == null) || (args.length==0)) {
            System.out.println("NOTE : -source not provided, default taken");
        } else {
            params.add("-source");
            params.add(args[0]);
            System.out.println("NOTE : -source will be: " + args[0]);
        }
        params.add("p");
        System.out.println("arguments: " + params);
        if (jdk.javadoc.internal.tool.Main.execute(params.toArray(new String[params.size()])) != 0)
            throw new Error("Javadoc encountered warnings or errors.");
    }

    public boolean run(DocletEnvironment root) {
        ElementFilter.typesIn(root.getIncludedElements());
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
        reporter.print(Kind.NOTE, "init");
    }
}
