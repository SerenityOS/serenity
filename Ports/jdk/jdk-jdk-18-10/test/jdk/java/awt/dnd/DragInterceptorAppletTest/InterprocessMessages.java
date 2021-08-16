/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

public interface InterprocessMessages {
    final static int TEST_PASSED = 0;
    final static int DATA_WAS_INTERCEPTED = 212;
    final static int EXCEPTION_HANDLER_WAS_NOT_TRIGGERED = 213;
    final static int DATA_WAS_INTERCEPTED_AND_EXCEPTION_HANDLER_WAS_NOT_TRIGGERED  = 214;

    final static int UNEXPECTED_IO_EXCEPTION = 400;
    final static int UNEXPECTED_UNSUPPORTED_FLAVOR_EXCEPTION = 401;
}
