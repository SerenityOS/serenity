/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class MyDoclet implements Doclet {
    private static final boolean OK = true;
    private boolean verbose;
    private Reporter reporter;

    Set<Option> options = Set.of(
            new Option("--alpha -a", false, "an example no-arg option") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    System.out.println("received option " + option + " " + arguments);
                    return OK;
                }
            },
            new Option("--beta -b", true, "an example 1-arg option") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    System.out.println("received option " + option + " " + arguments);
                    return OK;
                }
            },
            new Option("--verbose", false, "report progress") {
                @Override
                public boolean process(String option, List<String> arguments) {
                    verbose = true;
                    return OK;
                }
            }
    );

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "MyDoclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return options;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        MyScanner myScanner = new MyScanner();
        for (Element e : environment.getSpecifiedElements()) {
            myScanner.scan(e, 0);
        }

        return OK;
    }

    class MyScanner extends ElementScanner14<Void, Integer> {
        @Override
        public Void scan(Element e, Integer depth) {
            String msg = e.getKind() + " " + e;
            reporter.print(Diagnostic.Kind.NOTE, e, msg);
            return super.scan(e, depth + 1);
        }
    }


    abstract class Option implements Doclet.Option {
        final List<String> names;
        final boolean hasArg;
        final String description;

        Option(String names, boolean hasArg, String description) {
            this.names = List.of(names.split("\\s+"));
            this.hasArg = hasArg;
            this.description = description;
        }

        @Override
        public int getArgumentCount() {
            return hasArg ? 1 : 0;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return names;
        }

        @Override
        public String getParameters() {
            return hasArg ? "<arg>" : null;
        }
    }
}
