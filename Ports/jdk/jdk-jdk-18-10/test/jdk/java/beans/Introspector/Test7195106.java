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
 * @bug 7195106
 * @summary Tests that explicit BeanInfo is not collected
 * @author Sergey Malenkov
 * @run main/othervm -Xmx128m Test7195106
 */

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.SimpleBeanInfo;

public class Test7195106 {

    public static void main(String[] arg) throws Exception {
        BeanInfo info = Introspector.getBeanInfo(My.class);
        if (null == info.getIcon(BeanInfo.ICON_COLOR_16x16)) {
            throw new Error("Unexpected behavior");
        }
        try {
            int[] array = new int[1024];
            while (true) {
                array = new int[array.length << 1];
            }
        }
        catch (OutOfMemoryError error) {
            System.gc();
        }
        if (null == info.getIcon(BeanInfo.ICON_COLOR_16x16)) {
            throw new Error("Explicit BeanInfo is collected");
        }
    }

    public static class My {
    }

    public static class MyBeanInfo extends SimpleBeanInfo {
        @Override
        public Image getIcon(int type) {
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        }
    }
}
