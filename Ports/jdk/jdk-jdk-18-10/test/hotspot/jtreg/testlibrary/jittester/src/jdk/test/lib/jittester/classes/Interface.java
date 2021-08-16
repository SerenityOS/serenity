/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.classes;

import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.visitors.Visitor;

public class Interface extends IRNode {
    private final String name;
    private TypeKlass parent = null;

    public Interface(TypeKlass parent, String name, int level, IRNode functionDeclaraionBlock) {
        super(TypeList.find(functionDeclaraionBlock.getOwner().getName()));
        this.parent = parent;
        this.name = name;
        this.level = level;
        addChild(functionDeclaraionBlock);
    }

    @Override
    public long complexity() {
        return 0;
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public TypeKlass getParentKlass() {
        return parent;
    }
}
