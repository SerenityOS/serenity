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
package org.netbeans.jemmy.util;

import java.awt.Component;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.Operator;

/**
 *
 * Specifies criteria for component lookup basing on component name.
 *
 * By default uses new Operator.DefaultStringComparator(true, true) compa
 *
 * @author Nathan Paris (Nathan_Paris@adp.com)
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class NameComponentChooser implements ComponentChooser {

    private String name;
    private Operator.StringComparator comparator;

    /**
     * Creates an instance to search for a component by name.
     *
     * @param name Expecten component name pattern.
     * @param comparator Comparator for a comparision of a component name with a
     * pattern.
     */
    public NameComponentChooser(String name, Operator.StringComparator comparator) {
        this.name = name;
        this.comparator = comparator;
    }

    /**
     * Creates an instance to search for a component by name using exact
     * comparision.
     *
     * @param name Expecten component name pattern.
     */
    public NameComponentChooser(String name) {
        this(name, new Operator.DefaultStringComparator(true, true));
    }

    @Override
    public boolean checkComponent(Component component) {
        return comparator.equals(component.getName(), name);
    }

    @Override
    public String getDescription() {
        return "Component having \"" + name + "\" name.";
    }

    @Override
    public String toString() {
        return "NameComponentChooser{" + "name=" + name + ", comparator=" + comparator + '}';
    }
}
