/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 *
 * Base class for tests that check inner classes in inner class.
 *
 * @author Andrei Eremeev
 */
public abstract class InnerClassesInInnerClassTestBase extends InnerClassesTestBase {

    public InnerClassesInInnerClassTestBase() {
        setPrefix("Inner {");
        setSuffix("}");
    }

    @Override
    public void getAdditionalFlags(
            Map<String, Set<String>> class2Flags, ClassType type, Modifier...flags) {
        super.getAdditionalFlags(class2Flags, type, flags);
        class2Flags.put("Inner", getFlags(type, Arrays.asList(flags)));
    }
}
