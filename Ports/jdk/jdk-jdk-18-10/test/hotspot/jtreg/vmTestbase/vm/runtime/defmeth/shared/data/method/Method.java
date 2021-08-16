/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.data.method;

import vm.runtime.defmeth.shared.data.Visitor;
import vm.runtime.defmeth.shared.data.Element;

/*
 * Represents a method w/o a link to any class.
 */
public class Method implements Element {
    protected int acc;
    protected String name;
    protected String desc;
    protected String sig;

    public Method(int acc, String name, String desc, String sig) {
        this.acc = acc;
        this.name = name;
        this.desc = desc;
        this.sig = sig;
    }

    public int acc() {
        return acc;
    }

    public String name() {
        return name;
    }

    public String desc() {
        return desc;
    }

    public String sig() {
        return sig;
    }

    public String[] getExceptions() {
        return new String[0]; // No exceptions supported yet
    }

    public boolean hasNonVoidReturn() {
        return !desc.matches(".*V");
    }

    public boolean isConstructor() {
        return name.equals("<init>") &&
               desc.equals("()V");
    }
    @Override
    public void visit(Visitor v) {
        v.visitMethod(this);
    }
}
