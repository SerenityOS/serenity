/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.options;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import nsk.share.TestBug;

/**
 * This is a "value" class for holding information about the defined options.
 */
public final class OptionDefinition {
        private static final Map<String, OptionObjectFactory> factories = new HashMap<String, OptionObjectFactory>();
        private String prefix;
        private String name;
        private String description;
        private String defaultValue;
        private Class<? extends OptionObjectFactory> factory;
        private Field field;
        private Object owner;

        public OptionDefinition(String prefix, String name, String description, String defaultValue, Class<? extends OptionObjectFactory> factory, Field field, Object owner) {
                this.prefix = prefix;
                this.name = name;
                this.description = description;
                this.defaultValue = defaultValue;
                this.factory = factory;
                this.field = field;
                this.owner = owner;
        }

        public String getPrefix() {
                return prefix;
        }

        public String getName() {
                return name;
        }

        public String getFullName() {
                if (prefix != null)
                        return prefix + "." + name;
                else
                        return name;
        }

        public Field getField() {
                return field;
        }

        public Object getOwner() {
                return owner;
        }

        public String getDescription() {
                if (hasFactory())
                        if (description == null)
                                return getFactory().getDescription();
                return description;
        }

        public String getDefaultValue() {
                if (hasFactory())
                        if (defaultValue == null)
                                return getFactory().getDefaultValue();
                return defaultValue;
        }

        public boolean hasFactory() {
                return factory != null;
        }

        public String getPlaceHolder() {
                if (hasFactory())
                        return getFactory().getPlaceholder();
                else
                        return getField().getType().toString();
        }

        public synchronized OptionObjectFactory getFactory() {
                if (factory == null)
                        throw new TestBug("Called getFactory() on OptionDefinition with unset factory");
                OptionObjectFactory factory = factories.get(this.factory);
                if (factory == null) {
                        try {
                                factory = this.factory.newInstance();
                        } catch (InstantiationException ex) {
                                throw new TestBug("Failed to instantiate factory " + this.factory, ex);
                        } catch (IllegalAccessException ex) {
                                throw new TestBug("Failed to instantiate factory " + this.factory, ex);
                        }
                }
                return factory;
        }

        public String toString() {
                return "Option " + name + " field " + field + " object " + owner;
        }
}
