/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 7092283
 * @author Alexander Scherbatiy
 * @summary Property Window.locationByPlatform is not cleared by calling
 *          setVisible(false)
 * @run main LocationByPlatformTest
 */
import java.awt.Window;

public class LocationByPlatformTest {

    public static void main(String[] args) {

        Window window = new Window(null);
        window.setSize(100, 100);
        window.setLocationByPlatform(true);

        if (!window.isLocationByPlatform()) {
            throw new RuntimeException("Location by platform is not set");
        }

        window.setLocation(10, 10);

        if (window.isLocationByPlatform()) {
            throw new RuntimeException("Location by platform is not cleared");
        }

        window.setLocationByPlatform(true);
        window.setBounds(20, 20, 50, 50);

        if (window.isLocationByPlatform()) {
            throw new RuntimeException("Location by platform is not cleared");
        }

        window.setLocationByPlatform(true);
        window.setVisible(false);

        if (window.isLocationByPlatform()) {
            throw new RuntimeException("Location by platform is not cleared");
        }

        window.setLocationByPlatform(true);
        window.setVisible(true);

        if (window.isLocationByPlatform()) {
            throw new RuntimeException("Location by platform is not cleared");
        }

        window.dispose();
    }
}
