/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code;

import java.util.Locale;

/**
 * Exception thrown when the compiler refuses to compile a method because of problems with the
 * method. e.g. bytecode wouldn't verify, too big, JSR/ret too complicated, etc. This exception is
 * <i>not</i> meant to indicate problems with the compiler itself.
 */
public class BailoutException extends RuntimeException {

    public static final long serialVersionUID = 8974598793458772L;
    private final boolean permanent;

    /**
     * Creates a new {@link BailoutException}.
     *
     *
     * @param args parameters to the formatter
     */
    public BailoutException(String format, Object... args) {
        super(String.format(Locale.ENGLISH, format, args));
        this.permanent = true;
    }

    /**
     * Creates a new {@link BailoutException}.
     *
     *
     * @param args parameters to the formatter
     */
    public BailoutException(Throwable cause, String format, Object... args) {
        super(String.format(Locale.ENGLISH, format, args), cause);
        this.permanent = true;
    }

    /**
     * Creates a new {@link BailoutException}.
     *
     * @param permanent specifies whether this exception will occur again if compilation is retried
     * @param args parameters to the formatter
     */
    public BailoutException(boolean permanent, String format, Object... args) {
        super(String.format(Locale.ENGLISH, format, args));
        this.permanent = permanent;
    }

    /**
     * @return whether this exception will occur again if compilation is retried
     */
    public boolean isPermanent() {
        return permanent;
    }
}
