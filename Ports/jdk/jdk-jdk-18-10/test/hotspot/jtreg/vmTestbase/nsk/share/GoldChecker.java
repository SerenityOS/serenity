/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import jdk.test.lib.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GoldChecker extends AbstractGoldChecker {
    private final String goldOutput;

    public GoldChecker(String mainClassName) {
        goldOutput = readGoldStr(Path.of(Utils.TEST_SRC, mainClassName + ".gold"));
    }

    @Override
    protected String getGoldenString() {
        return goldOutput;
    }

    private String readGoldStr(Path goldenFile) {
        if (Files.notExists(goldenFile)) {
            throw new TestBug("Unable to open golden file '" + goldenFile + "' for reading");
        }
        byte[] data;
        try {
            data = Files.readAllBytes(goldenFile);
        } catch (IOException e) {
            throw new TestBug("Error reading from golden file '" + goldenFile + "'", e);
        }
        return new String(data, StandardCharsets.US_ASCII);
    }

}
