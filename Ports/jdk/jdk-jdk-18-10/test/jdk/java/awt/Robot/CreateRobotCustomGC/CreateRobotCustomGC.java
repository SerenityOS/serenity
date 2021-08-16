/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Robot;

/**
 * @test
 * @key headful
 * @bug 8238936
 * @summary Check that Robot(GraphicsDevice) constructor throw AWTException if
 *          device is unsupported
 */
public final class CreateRobotCustomGC {

    public static void main(String[] args) {
        try {
            new Robot(new GraphicsDevice() {
                @Override
                public int getType() {
                    return TYPE_RASTER_SCREEN;
                }

                @Override
                public String getIDstring() {
                    return "Custom screen device";
                }

                @Override
                public GraphicsConfiguration[] getConfigurations() {
                    return new GraphicsConfiguration[0];
                }

                @Override
                public GraphicsConfiguration getDefaultConfiguration() {
                    return null;
                }
            });
            throw new RuntimeException("Expected AWTException did not occur");
        } catch (AWTException ignored) {
            // expected AWTException
        }
    }
}