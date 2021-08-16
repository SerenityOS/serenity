/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy;

/**
 *
 * Exception is throught as a result of test. either test failed or passed.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class TestCompletedException extends JemmyException {

    private static final long serialVersionUID = 42L;

    private int status;

    /**
     * Constructor.
     *
     * @param st Exit status.
     * @param ex Exception provoked test failure.
     */
    public TestCompletedException(int st, Exception ex) {
        super("Test "
                + ((st == 0)
                        ? "passed"
                        : "failed with status " + Integer.toString(st)),
                ex);
        status = st;
    }

    /**
     * Constructor.
     *
     * @param st Exit status.
     * @param description Failure reason
     */
    public TestCompletedException(int st, String description) {
        super("Test "
                + ((st == 0)
                        ? "passed"
                        : "failed with status " + Integer.toString(st)
                        + "\n" + description));
        status = st;
    }

    /**
     * Returns status.
     *
     * @return test status
     */
    public int getStatus() {
        return status;
    }
}
