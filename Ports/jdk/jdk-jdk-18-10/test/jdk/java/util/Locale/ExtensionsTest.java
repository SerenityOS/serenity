/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7168528
 * @summary Test Locale.hasExtensions() and Locale.stripExtensions().
 */

import java.util.*;

public class ExtensionsTest {
    public static void main(String[] args) {
        Locale jaJPJP = new Locale("ja", "JP", "JP");
        if (!jaJPJP.hasExtensions()) {
            error(jaJPJP + " should have an extension.");
        }
        Locale stripped = jaJPJP.stripExtensions();
        if (stripped.hasExtensions()) {
            error(stripped + " should NOT have an extension.");
        }
        if (jaJPJP.equals(stripped)) {
            throw new RuntimeException("jaJPJP equals stripped");
        }
        if (!"ja-JP-x-lvariant-JP".equals(stripped.toLanguageTag())) {
            error("stripped.toLanguageTag() isn't ja-JP-x-lvariant-JP");
        }

        Locale enUSja = Locale.forLanguageTag("en-US-u-ca-japanese");
        if (!enUSja.stripExtensions().equals(Locale.US)) {
            error("stripped enUSja not equals Locale.US");
        }

        // If a Locale has no extensions, stripExtensions() returns self.
        Locale enUS = Locale.US.stripExtensions();
        if (enUS != Locale.US) {
            error("stripped Locale.US != Locale.US");
        }
    }

    private static void error(String msg) {
        throw new RuntimeException(msg);
    }
}
