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

package jdk.test.lib.jittester.arrays;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.LocalVariable;
import jdk.test.lib.jittester.types.TypeArray;
import jdk.test.lib.jittester.visitors.Visitor;


/*
Array extraction produces and array with N dimentions from an array with M
dimentions, where N < M.
 */
public class ArrayExtraction extends IRNode {
    private final List<Byte> dims;
    public ArrayExtraction(IRNode array, ArrayList<IRNode> dimensionExpressions) {
        super(array.getResultType());
        addChild(array);
        addChildren(dimensionExpressions);
        if (array instanceof ArrayCreation) {
            dims = new ArrayList<>();
            ArrayCreation ac = (ArrayCreation) array;
            for (int i = dimensionExpressions.size(); i < ac.getDimensionsCount(); ++i) {
                dims.add(ac.getDimensionSize(i));
            }
        } else if (array instanceof ArrayExtraction) {
            dims = new ArrayList<>();
            ArrayExtraction ae = (ArrayExtraction) array;
            for (int i = dimensionExpressions.size(); i < ae.getDimsNumber(); ++i) {
                dims.add(ae.getDim(i));
            }
        } else if (array instanceof LocalVariable) {
            LocalVariable loc = (LocalVariable) array;
            TypeArray type = (TypeArray) loc.getVariableInfo().type;
            dims = type.getDims();
            for (int i = dimensionExpressions.size(); i < type.dimensions; ++i) {
                dims.add(type.getDims().get(i));
            }
        } else {
            dims = dimensionExpressions.stream()
                .map(d -> {
                    if (d instanceof Literal) {
                        Literal n = (Literal) d;
                        return (Byte)n.getValue();
                    }
                    return (byte)0;
                })
                .collect(Collectors.toList());
        }
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public byte getDim(int dim) {
        return dims.get(dim);
    }

    public int getDimsNumber() {
        return dims.size();
    }
}
