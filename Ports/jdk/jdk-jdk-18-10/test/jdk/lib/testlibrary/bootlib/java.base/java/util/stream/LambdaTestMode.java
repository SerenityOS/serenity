/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

/**
 * Runtime modes of test execution.
 */
public enum LambdaTestMode {
    /**
     * Execution mode with no particular runtime constraints.
     */
    NORMAL,

    /**
     * Execution mode where tests are executed for testing lambda serialization
     * and deserialization.
     *
     * <p>This mode may be queried by tests or data supplied by data
     * providers, which cannot otherwise be assigned to the test group
     * <em>serialization-hostile</em>, to not execute or declare
     * serialization-hostile code or data.
     *
     * <p>This mode is enabled if the boolean system property
     * {@code org.openjdk.java.util.stream.sand.mode} is declared with a
     * {@code true} value.
     */
    SERIALIZATION;

    /**
     * {@code true} if tests are executed in the mode for testing lambda
     * Serialization ANd Deserialization (SAND).
     */
    private static final boolean IS_LAMBDA_SERIALIZATION_MODE =
            Boolean.getBoolean("org.openjdk.java.util.stream.sand.mode");

    /**
     *
     * @return the mode of test execution.
     */
    public static LambdaTestMode getMode() {
        return IS_LAMBDA_SERIALIZATION_MODE ? SERIALIZATION : NORMAL;
    }

    /**
     *
     * @return {@code true} if normal test mode.
     */
    public static boolean isNormalMode() {
        return getMode() == NORMAL;
    }
}
