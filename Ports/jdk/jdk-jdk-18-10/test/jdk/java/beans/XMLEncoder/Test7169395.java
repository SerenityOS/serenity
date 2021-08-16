/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7169395
 * @summary Tests that array list initialized correctly
 * @run main/othervm -Djava.security.manager=allow Test7169395
 * @author Sergey Malenkov
 */

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class Test7169395 extends AbstractTest {

    public static void main(String[] args) {
        new Test7169395().test(true);
    }

    protected Object getObject() {
        Container container = new Container();
        container.add("test-null", null);
        container.add("test-value", "value");
        container.add("test-other", "other");
        return container;
    }

    public static class Component {

        private final Container container;
        private final String name;
        private String value;

        @ConstructorProperties({ "container", "name" })
        public Component(Container container, String name) {
            this.container = container;
            this.name = name;
        }

        public Container getContainer() {
            return this.container;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Container {

        private final Map<String, Component> map = new TreeMap<String, Component>();

        public Collection<Component> getComponents() {
            return new ArrayList<Component>(this.map.values());
        }

        public void setComponents(Collection<Component> components) {
            this.map.clear();
            for (Component component : components){
                this.map.put(component.getName(), component);
            }
        }

        public void add(String name, String value) {
            Component list = new Component(this, name);
            list.setValue(value);
            this.map.put(name, list);
        }
    }
}
