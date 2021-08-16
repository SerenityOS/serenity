/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.anonloader.share;

import vm.mlvm.share.Env;

public class AnonkTestee01 {
    public final static String muzzy = "BIG \uFFFF\u0000\uFFFE\uFEFF MUZZY";
    public final static String
    theDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrum
    = String.format("%65500c%X", 'c', Env.getRNG().nextLong());

    public final String beatingTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrum() {
        return theDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrumIsTheDrum;
    }

    public final String toString() {
        return "Something that looks like " + super.toString();
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Making fun of errors");
    }
}
