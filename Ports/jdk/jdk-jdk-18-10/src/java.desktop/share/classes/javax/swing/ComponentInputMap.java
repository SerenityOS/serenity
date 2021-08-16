/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
package javax.swing;

/**
 * A <code>ComponentInputMap</code> is an <code>InputMap</code>
 * associated with a particular <code>JComponent</code>.
 * The component is automatically notified whenever
 * the <code>ComponentInputMap</code> changes.
 * <code>ComponentInputMap</code>s are used for
 * <code>WHEN_IN_FOCUSED_WINDOW</code> bindings.
 *
 * @author Scott Violet
 * @since 1.3
 */
@SuppressWarnings("serial") // Field data not serializable across versions
public class ComponentInputMap extends InputMap {
    /** Component binding is created for. */
    private JComponent          component;

    /**
     * Creates a <code>ComponentInputMap</code> associated with the
     * specified component.
     *
     * @param component  a non-null <code>JComponent</code>
     * @throws IllegalArgumentException  if <code>component</code> is null
     */
    public ComponentInputMap(JComponent component) {
        this.component = component;
        if (component == null) {
            throw new IllegalArgumentException("ComponentInputMaps must be associated with a non-null JComponent");
        }
    }

    /**
     * Sets the parent, which must be a <code>ComponentInputMap</code>
     * associated with the same component as this
     * <code>ComponentInputMap</code>.
     *
     * @param map  a <code>ComponentInputMap</code>
     *
     * @throws IllegalArgumentException  if <code>map</code>
     *         is not a <code>ComponentInputMap</code>
     *         or is not associated with the same component
     */
    public void setParent(InputMap map) {
        if (getParent() == map) {
            return;
        }
        if (map != null && (!(map instanceof ComponentInputMap) ||
                 ((ComponentInputMap)map).getComponent() != getComponent())) {
            throw new IllegalArgumentException("ComponentInputMaps must have a parent ComponentInputMap associated with the same component");
        }
        super.setParent(map);
        getComponent().componentInputMapChanged(this);
    }

    /**
     * Returns the component the {@code InputMap} was created for.
     *
     * @return the component the {@code InputMap} was created for.
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * Adds a binding for <code>keyStroke</code> to <code>actionMapKey</code>.
     * If <code>actionMapKey</code> is null, this removes the current binding
     * for <code>keyStroke</code>.
     */
    public void put(KeyStroke keyStroke, Object actionMapKey) {
        super.put(keyStroke, actionMapKey);
        if (getComponent() != null) {
            getComponent().componentInputMapChanged(this);
        }
    }

    /**
     * Removes the binding for <code>key</code> from this object.
     */
    public void remove(KeyStroke key) {
        super.remove(key);
        if (getComponent() != null) {
            getComponent().componentInputMapChanged(this);
        }
    }

    /**
     * Removes all the mappings from this object.
     */
    public void clear() {
        int oldSize = size();
        super.clear();
        if (oldSize > 0 && getComponent() != null) {
            getComponent().componentInputMapChanged(this);
        }
    }
}
