/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035473 8149565
 * @summary Verify that init method works correctly.
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic.Kind;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;

public class VerifyLocale implements Doclet {
    static String language;
    static String country;
    static String variant;

    Locale locale;
    Reporter reporter;

    public static void main(String[] args) {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        Path thisFile =
            Paths.get(System.getProperty("test.src", ".")).resolve("VerifyLocale.java");
        JavaFileObject fo = tool.getStandardFileManager(null, null, null)
                .getJavaFileObjects(thisFile).iterator().next();

        int skipCount = 0;
        int testCount = 0;

        for (Locale loc : Locale.getAvailableLocales()) {

            language = loc.getLanguage();
            country = loc.getCountry();
            variant = loc.getVariant();
            System.err.printf("test locale: %s [%s,%s,%s] %s%n",
                loc, language, country, variant, loc.toLanguageTag());

            // skip locales for which the round-trip fails
            if (!loc.equals(Locale.forLanguageTag(loc.toLanguageTag()))) {
                System.err.println("skipped " + loc + "!");
                System.err.println();
                skipCount++;
                continue;
            }

            if (!language.equals("")) {
                List<String> options = List.of("-locale", loc.toLanguageTag());
                System.err.println("test options: " + options);
                DocumentationTask t = tool.getTask(null, null, null,
                        VerifyLocale.class, options, List.of(fo));
                if (!t.call())
                    throw new Error("Javadoc encountered warnings or errors.");
                testCount++;
            }
            System.err.println();
        }
        System.err.println("Skipped " + skipCount + " locales");
        System.err.println("Tested " + testCount + " locales");
    }

    public boolean run(DocletEnvironment root) {
        reporter.print(Kind.NOTE, String.format("doclet locale is: %s [%s,%s,%s] %s (%s)",
                locale,
                locale.getLanguage(),
                locale.getCountry(),
                locale.getVariant(),
                locale.toLanguageTag(),
                locale.getDisplayName()));
        return language.equals(locale.getLanguage())
               && country.equals(locale.getCountry())
               && variant.equals(locale.getVariant());
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

    public void init(Locale locale, Reporter reporter) {
        this.locale = locale;
        this.reporter = reporter;
    }
}
