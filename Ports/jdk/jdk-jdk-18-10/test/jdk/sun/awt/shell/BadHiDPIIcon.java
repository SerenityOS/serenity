/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8151385
 * @summary JOptionPane icons are cropped on Windows 10 with HiDPI display
 * @author Hendrik Schreiber
 * @requires os.family == "windows"
 * @modules java.desktop/sun.awt.shell
 * @run main BadHiDPIIcon
 */
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import sun.awt.shell.ShellFolder;

public class BadHiDPIIcon {

    public static void main(String[] args) {
        // the error icon is round and in all four corner transparent
        // we check that all corners are identical
        Image icon = (Image) ShellFolder.get("optionPaneIcon Error");
        final BufferedImage image = getBufferedImage(icon);
        final int upperLeft = image.getRGB(0, 0);
        final int upperRight = image.getRGB(image.getWidth() - 1, 0);
        final int lowerLeft = image.getRGB(0, image.getHeight() - 1);
        final int lowerRight = image.getRGB(image.getWidth() - 1, image.getHeight() - 1);
        if (upperLeft != upperRight || upperLeft != lowerLeft || upperLeft != lowerRight) {
            throw new RuntimeException("optionPaneIcon Error is not a round icon with transparent background.");
        }
    }

    private static BufferedImage getBufferedImage(Image image) {
        if (image instanceof MultiResolutionImage) {
            MultiResolutionImage mrImage = (MultiResolutionImage) image;
            return (BufferedImage) mrImage.getResolutionVariant(32, 32);
        }
        return (BufferedImage) image;
    }
}
