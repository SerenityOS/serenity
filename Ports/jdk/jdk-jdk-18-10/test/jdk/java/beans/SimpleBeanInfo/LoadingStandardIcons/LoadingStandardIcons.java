/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;

import javax.swing.JButton;

/**
 * @test
 * @bug 4141523
 * @run main/othervm/policy=java.policy -Djava.security.manager LoadingStandardIcons
 */
public final class LoadingStandardIcons {

    public static void main(final String[] args) {
        final Object bi;
        try {
            bi = Introspector.getBeanInfo(JButton.class);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
        final Image m16 = ((BeanInfo) bi).getIcon(BeanInfo.ICON_MONO_16x16);
        final Image m32 = ((BeanInfo) bi).getIcon(BeanInfo.ICON_MONO_32x32);
        final Image c16 = ((BeanInfo) bi).getIcon(BeanInfo.ICON_COLOR_16x16);
        final Image c32 = ((BeanInfo) bi).getIcon(BeanInfo.ICON_COLOR_32x32);
        if (m16 == null || m32 == null || c16 == null || c32 == null) {
            throw new RuntimeException("Image should not be null");
        }
    }
}
