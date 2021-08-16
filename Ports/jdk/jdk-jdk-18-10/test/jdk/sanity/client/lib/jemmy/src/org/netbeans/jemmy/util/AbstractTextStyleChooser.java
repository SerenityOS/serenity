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

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import org.netbeans.jemmy.operators.JTextComponentOperator.TextChooser;

/**
 * Makes easier to implement searching criteria for
 * {@code javax.swing.text.StyledDocument}
 * {@code JTextComponentOperator.getPositionByText(String, JTextComponentOperator.TextChooser, int)}.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public abstract class AbstractTextStyleChooser implements TextChooser {

    /**
     * Constructor.
     */
    public AbstractTextStyleChooser() {
    }

    /**
     * Should return true if position fulfils criteria.
     *
     * @param doc a styled document to be searched.
     * @param element an element to be checked.
     * @param offset checked position.
     * @return true if position fits the criteria.
     */
    public abstract boolean checkElement(StyledDocument doc, Element element, int offset);

    @Override
    public abstract String getDescription();

    @Override
    public String toString() {
        return "AbstractTextStyleChooser{description = " + getDescription() + '}';
    }

    @Override
    public final boolean checkPosition(Document document, int offset) {
        return (checkElement(((StyledDocument) document),
                ((StyledDocument) document).getCharacterElement(offset),
                offset));
    }
}
