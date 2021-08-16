/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.beans.infos;

import java.beans.*;

/**
 * BeanInfo descriptor for a standard AWT component.
 */

public class ComponentBeanInfo extends SimpleBeanInfo {
    private static final Class<java.awt.Component> beanClass = java.awt.Component.class;

    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor
                      name = new PropertyDescriptor("name",       beanClass),
                background = new PropertyDescriptor("background", beanClass),
                foreground = new PropertyDescriptor("foreground", beanClass),
                      font = new PropertyDescriptor("font",       beanClass),
                   enabled = new PropertyDescriptor("enabled",    beanClass),
                   visible = new PropertyDescriptor("visible",    beanClass),
                 focusable = new PropertyDescriptor("focusable",  beanClass);

            enabled.setExpert(true);
            visible.setHidden(true);

            background.setBound(true);
            foreground.setBound(true);
            font.setBound(true);
            focusable.setBound(true);

            PropertyDescriptor[] rv = {name, background, foreground, font, enabled, visible, focusable };
            return rv;
        } catch (IntrospectionException e) {
            throw new Error(e.toString());
        }
    }
}
