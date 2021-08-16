/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5060820 5054615 5056707 5061476
 * @modules java.desktop
 *          java.naming
 * @compile GenerifiedUses.java
 */

import java.awt.image.CropImageFilter;
import java.awt.image.ImageFilter;
import java.awt.image.PixelGrabber;
import java.awt.image.ReplicateScaleFilter;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.spi.NamingManager;

public class GenerifiedUses {

    static void foo() throws Exception {

        Properties props = new Properties();

        // 5060820
        new InitialDirContext(props);

        // 5054615
        new InitialContext(props);

        // 5056707
        NamingManager.getObjectInstance(null, null, null, props);

        // 5061476
        new CropImageFilter(0, 0, 0, 0).setProperties(props);
        new ImageFilter().setProperties(props);
        new PixelGrabber(null, 0, 0, 0, 0, false).setProperties(props);
        new ReplicateScaleFilter(1, 1).setProperties(props);

    }

}
