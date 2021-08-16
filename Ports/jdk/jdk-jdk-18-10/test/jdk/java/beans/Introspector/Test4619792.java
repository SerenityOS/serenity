/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4619792
 * @summary Tests property descriptor for the focusable property
 * @author Mark Davidson
 */

import java.awt.Component;
import java.awt.Container;
import java.beans.IntrospectionException;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

/**
 * The focusable property is new in 1.4 and is a boolean and bound property
 * implemented in Component with isFocusable and setFocusable.
 * For some reason, this property is not picked up
 * by the Introspector all allong the class heirarchy.
 * <p/>
 * The MethodDescriptors for a BeanInfo can find
 * the isFocusable()/setFocusable() methods as part of the set.
 * <p/>
 * This has never worked from the earliest days of merlin.
 */
public class Test4619792 {
    public static void main(String[] args) throws IntrospectionException {
        Class[] types = {
                Component.class,
                Container.class,
                JComponent.class,
                AbstractButton.class,
                JButton.class,
                JToggleButton.class,
        };
        // Control set. "enabled" and "name" has the same pattern and can be found
        String[] names = {
                "enabled",
                "name",
                "focusable",
        };
        for (String name : names) {
            for (Class type : types) {
                BeanUtils.getPropertyDescriptor(type, name);
            }
        }
    }
}
