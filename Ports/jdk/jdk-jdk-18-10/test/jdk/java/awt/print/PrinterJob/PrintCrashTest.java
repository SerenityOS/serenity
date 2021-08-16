/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key printer headful
 * @bug 8163889
 * @summary Printing crashes on OSX.
 * @run main PrintCrashTest
 */

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Destination;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

import java.io.File;

public class PrintCrashTest {
    public static void main(String[] args) throws Exception {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex != 0) {
                return Printable.NO_SUCH_PAGE;
            } else {
                Shape shape = new Rectangle(110, 110, 10, 10);
                Rectangle rect = shape.getBounds();

                BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration().createCompatibleImage(rect.width, rect.height, Transparency.BITMASK);
                graphics.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);

                return Printable.PAGE_EXISTS;
            }
        });

        File file = null;
        try {
            HashPrintRequestAttributeSet hashPrintRequestAttributeSet = new HashPrintRequestAttributeSet();
            file = File.createTempFile("out", "ps");
            file.deleteOnExit();
            Destination destination = new Destination(file.toURI());
            hashPrintRequestAttributeSet.add(destination);
            printerJob.print(hashPrintRequestAttributeSet);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }
}

