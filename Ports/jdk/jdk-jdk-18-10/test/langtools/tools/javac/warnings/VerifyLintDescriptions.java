/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8033961
 * @summary Verify that all LintCategories have their descriptions filled.
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.resources:open
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.util.Log.PrefixKind;

public class VerifyLintDescriptions {
    public static void main(String... args) {
        ModuleLayer boot = ModuleLayer.boot();
        Module jdk_compiler = boot.findModule("jdk.compiler").get();
        ResourceBundle b = ResourceBundle.getBundle("com.sun.tools.javac.resources.javac",
                                                    Locale.US,
                                                    jdk_compiler);

        List<String> missing = new ArrayList<>();

        for (LintCategory lc : LintCategory.values()) {
            try {
                b.getString(PrefixKind.JAVAC.key("opt.Xlint.desc." + lc.option));
            } catch (MissingResourceException ex) {
                missing.add(lc.option);
            }
        }

        if (!missing.isEmpty()) {
            throw new UnsupportedOperationException("Lints that are missing description: " + missing);
        }
    }

}
