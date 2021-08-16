/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5023552
 * @summary Tests reference count updating
 * @run main/othervm -Djava.security.manager=allow Test5023552
 * @author Sergey Malenkov
 */

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLEncoder;

public final class Test5023552 extends AbstractTest {
    public static void main(String[] args) {
        new Test5023552().test(true);
    }

    protected Object getObject() {
        Component component = new Component();
        return component.create(component);
    }

    protected void initialize(XMLEncoder encoder) {
        encoder.setPersistenceDelegate(Container.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                Container container = (Container) oldInstance;
                Component component = container.getComponent();
                return new Expression(container, component, "create", new Object[] {component});
            }
        });
    }

    public static final class Component {
        public Container create(Component component) {
            return new Container(component);
        }
    }

    public static final class Container {
        private final Component component;

        public Container(Component component) {
            this.component = component;
        }

        public Component getComponent() {
            return this.component;
        }
    }
}
