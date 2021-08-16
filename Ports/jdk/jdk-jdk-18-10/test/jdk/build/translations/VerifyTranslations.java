/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @test
 * @requires release.implementor == "Oracle Corporation"
 * @modules jdk.jlink/jdk.tools.jimage
 * @summary Oracle builds of OpenJDK should only contain english, chinese and
 *          japanese translations
 */
public class VerifyTranslations {

    /**
     * The set of translations we want to see in an Oracle built image
     */
    private static final Set<String> VALID_TRANSLATION_SUFFIXES = Set.of(
            "_en", "_en_US", "_en_US_POSIX", "_ja", "_zh_CN"
    );

    /**
     * This regexp will not match locales with 3 letter lang strings because
     * doing so would trigger a ton of false positives all over the source
     * tree. This is ok for now but is a potential future flaw in the test.
     */
    private static final String BASE_LOCALE_REGEXP
            = "(_[a-z]{2}(_[A-Z][a-z]{3})?(_([A-Z]{2})|([0-9]{3}))?(_[a-zA-Z]+)?)";

    public static void main(String[] args) {
        String jdkPath = System.getProperty("test.jdk");
        String modulesFile = jdkPath + "/lib/modules";

        // Run jimage tool to extract list of all classes and resources in the jdk
        StringWriter output = new StringWriter();
        jdk.tools.jimage.Main.run(new String[] { "list", modulesFile }, new PrintWriter(output));

        Pattern classesLocalePattern = Pattern.compile(BASE_LOCALE_REGEXP + "\\.(class|properties)");

        boolean failed = false;
        String module = "";
        for (String line : output.toString().split("\n")) {
            if (line.startsWith("Module: ")) {
                module = line.substring(8).trim();
            }
            // We do not filter resources in jdk.localedata
            if (!module.equals("jdk.localedata")) {
                Matcher matcher = classesLocalePattern.matcher(line);
                if (matcher.find()) {
                    if (!VALID_TRANSLATION_SUFFIXES.contains(matcher.group(1))) {
                        System.out.println("Unsupported translation found in lib/modules: "
                                + module + "/" + line.trim());
                        failed = true;
                    }
                }
            }
        }

        // Check all files in src.zip
        Pattern sourceLocalePattern = Pattern.compile(BASE_LOCALE_REGEXP + "\\.java");
        String srcZip = jdkPath + "/lib/src.zip";
        try (ZipInputStream srcZipInput = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(srcZip)))) {
            ZipEntry entry;
            while ((entry = srcZipInput.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith("jdk.localedata")) {
                    Matcher matcher = sourceLocalePattern.matcher(name);
                    if (matcher.find()) {
                        if (!VALID_TRANSLATION_SUFFIXES.contains(matcher.group(1))) {
                            System.out.println("Unsupported translation found in lib/src.zip: " + name);
                            failed = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (failed) {
            throw new RuntimeException("lib/modules contains unsupported translations");
        }
    }
}
