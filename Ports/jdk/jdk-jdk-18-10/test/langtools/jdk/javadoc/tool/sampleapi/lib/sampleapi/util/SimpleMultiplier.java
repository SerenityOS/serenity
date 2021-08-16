/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package sampleapi.util;

import java.util.StringTokenizer;
import java.util.ArrayList;

/*
 * The class implements unknown number of nested loops. The number of modifiers
 * in class/interface definitions could be any size. Annotations are also multiplied
 * by this class.
 * That is, dataset xml can provide any number of modifier sets, and generator should
 * iterate through all possible modifiers, annotations and types combinations.
 *
 * For example, class definition xml provides 3 modifiers sets:
 *
 * "public,private"
 * "static"
 * "final,abstract"
 *
 * and one types set "void,int"
 *
 * the class will generate the sequence like:
 * "public static final void"
 * "public static final int"
 * "public static abstract void"
 * "public static abstract int"
 * "private static final void"
 * "private static final int"
 * "private static abstract void"
 * "private static abstract int".
 *
 * This sequence could be processed by just one loop instead of four.
 *
 * In other places where the number of possible positions are known,
 * the generator uses nested loops instead.
 */
public class SimpleMultiplier {

    ArrayList<ArrayList<String>> valueSpace = new ArrayList<>();

    int size = 0;
    int index = 0;

    public void addAxis(String values) {
        ArrayList<String> valueAxis = new ArrayList<>();
        StringTokenizer valuesTokens = new StringTokenizer(values, "|");
        while (valuesTokens.hasMoreTokens())
            valueAxis.add(valuesTokens.nextToken());
        valueSpace.add(valueAxis);
    }

    public void initIterator() {
        size = 1;
        if (!valueSpace.isEmpty()) {
            for (int i = 0; i < valueSpace.size(); i++)
                size *= valueSpace.get(i).size();
        }
        index = 0;
    }

    public boolean hasNext() {
        return index < size;
    }

    public ArrayList<String> getNext() {
        ArrayList<String> next = new ArrayList<>();
        int positionIndex = index;

        // last added changing faster
        for (int i = valueSpace.size() - 1; i >= 0; i--) {
            ArrayList<String> valueAxis = valueSpace.get(i);
            int axisSize = valueAxis.size();
            next.add(valueAxis.get(positionIndex % axisSize));
            positionIndex /= axisSize;
        }
        index += 1;

        return next;
    }
}
