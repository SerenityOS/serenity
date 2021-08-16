/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6514600
   @summary Verifies if AbstractAction throws NullPointerException when cloned
   @run main AbstractActionBug
 */
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public final class AbstractActionBug extends AbstractAction implements Cloneable
{
    public static final void main(String[] args) throws Exception
    {
        AbstractActionBug a1 = new AbstractActionBug("a1");
        a1 = (AbstractActionBug) a1.clone();
        System.out.println("a1 cloned ok");

        AbstractActionBug a2 = new AbstractActionBug("a2");
        a2.putValue(NAME, "null");
        a2 = (AbstractActionBug) a2.clone();
        System.out.println("a2 cloned ok");

        AbstractActionBug a3 = new AbstractActionBug(null);
        a3 = (AbstractActionBug) a3.clone();
        System.out.println("a3 cloned ok");
    }

    private AbstractActionBug(String name) {
        putValue(NAME, name);
    }

    public void actionPerformed(ActionEvent e)
    {
    }
}