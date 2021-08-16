/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

public class AssignableFromImpl
    implements AssignableFrom, java.io.Serializable
{
    // ignored during record deserialization, but enforced for non-record deserialization
    private static final long serialVersionUID = 1L;

    private final Number number;

    public AssignableFromImpl(Number number) {
        this.number = number;
    }

    @Override
    public Number number() {
        return number;
    }

    @Override
    public String toString() {
        return String.format("AssignableFromImpl[number=%s(%s)]",
                             number, number.getClass().getName());
    }
}
