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
 * @bug 4652928
 * @summary Tests encoding of collections
 * @run main/othervm -Djava.security.manager=allow Test4652928
 * @author Sergey Malenkov, Mark Davidson
 */

import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextServicesSupport;
import java.beans.beancontext.BeanContextSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;

public class Test4652928 extends AbstractTest<List> {
    public static void main(String[] args) {
        new Test4652928().test(true);
    }

    protected List getObject() {
        List<BeanContext> list = new ArrayList<BeanContext>();
        list.add(fill(new BeanContextSupport()));
        list.add(fill(new BeanContextServicesSupport()));
        return list;
    }

    private static BeanContext fill(BeanContext context) {
        context.add(new JLabel("label"));
        context.add(new JButton("button"));

        JButton button = new JButton();
        button.setText("another button");
        context.add(button);

        return context;
    }
}
