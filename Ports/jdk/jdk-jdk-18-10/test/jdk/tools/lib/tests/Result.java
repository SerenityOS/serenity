/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package tests;

import java.nio.file.Path;

public class Result {
    private final int exitCode;
    private final String message;
    private final Path imageFile;

    public Result(int exitCode, String message, Path imageFile) {
        this.exitCode = exitCode;
        this.message = message;
        this.imageFile = imageFile;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getMessage() {
        return message;
    }

    public Path getFile() {
        return imageFile;
    }

    public void assertFailure() {
        assertFailure(null);
    }

    public void assertFailure(String expected) {
        if (getExitCode() == 0) {
            System.err.println(getMessage());
            throw new AssertionError("Failure expected: " + getFile());
        }
        if (getExitCode() != 1 && getExitCode() != 2) {
            System.err.println(getMessage());
            throw new AssertionError("Abnormal exit: " + getFile());
        }
        if (expected != null) {
            if (expected.isEmpty()) {
                throw new AssertionError("Expected error is empty");
            }
            if (!getMessage().matches(expected) && !getMessage().contains(expected)) {
                System.err.println(getMessage());
                throw new AssertionError("Output does not fit regexp: " + expected);
            }
        }
        System.err.println("Failed as expected. " + (expected != null ? expected : ""));
        System.err.println(getMessage());
    }

    public Path assertSuccess() {
        if (getExitCode() != 0) {
            System.err.println(getMessage());
            throw new AssertionError("Unexpected failure: " + getExitCode());
        }
        return getFile();
    }
}
