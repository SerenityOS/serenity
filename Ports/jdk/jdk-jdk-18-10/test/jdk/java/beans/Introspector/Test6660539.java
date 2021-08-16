/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6660539
 * @summary Tests changeable BeanInfo cache in different application contexts
 * @author Sergey Malenkov
 */

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

public class Test6660539 implements Runnable {
    private static final String NAME = "$$$";

    public static void main(String[] args) throws Exception {
        for (PropertyDescriptor pd : getPropertyDescriptors()) {
            pd.setDisplayName(NAME);
        }
        ThreadGroup group = new ThreadGroup(NAME);
        Thread thread = new Thread(group, new Test6660539());
        thread.start();
        thread.join();
    }

    public void run() {
        for (PropertyDescriptor pd : getPropertyDescriptors()) {
            if (pd.getDisplayName().equals(NAME))
                throw new Error("shared BeanInfo cache");
        }
    }

    private static PropertyDescriptor[] getPropertyDescriptors() {
        try {
            BeanInfo info = Introspector.getBeanInfo(Test6660539.class);
            return info.getPropertyDescriptors();
        }
        catch (IntrospectionException exception) {
            throw new Error("unexpected", exception);
        }
    }
}
