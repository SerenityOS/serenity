/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.awt.Graphics;
import java.awt.PrintGraphics;
import java.awt.PrintJob;

/**
 * A subclass of Graphics that can be printed to. The
 * graphics calls are forwared to another Graphics instance
 * that does the actual rendering.
 */

public class ProxyPrintGraphics extends ProxyGraphics
                                implements PrintGraphics {

    private PrintJob printJob;

    public ProxyPrintGraphics(Graphics graphics, PrintJob thePrintJob) {
        super(graphics);
        printJob = thePrintJob;
    }

    /**
     * Returns the PrintJob object from which this PrintGraphics
     * object originated.
     */
    public PrintJob getPrintJob() {
        return printJob;
    }

   /**
     * Creates a new {@code Graphics} object that is
     * a copy of this {@code Graphics} object.
     * @return     a new graphics context that is a copy of
     *                       this graphics context.
     */
    public Graphics create() {
        return new ProxyPrintGraphics(getGraphics().create(), printJob);
    }


    /**
     * Creates a new {@code Graphics} object based on this
     * {@code Graphics} object, but with a new translation and
     * clip area.
     * Refer to
     * {@link sun.print.ProxyGraphics#create(int, int, int, int)}
     * for a complete description of this method.
     * <p>
     * @param      x   the <i>x</i> coordinate.
     * @param      y   the <i>y</i> coordinate.
     * @param      width   the width of the clipping rectangle.
     * @param      height   the height of the clipping rectangle.
     * @return     a new graphics context.
     * @see        java.awt.Graphics#translate
     * @see        java.awt.Graphics#clipRect
     */
    public Graphics create(int x, int y, int width, int height) {
        Graphics g = getGraphics().create(x, y, width, height);
        return new ProxyPrintGraphics(g, printJob);
    }

    public Graphics getGraphics() {
        return super.getGraphics();
    }


   /* Spec implies dispose() should flush the page, but the implementation
    * has in fact always done this on the getGraphics() call, thereby
    * ensuring that multiple pages are cannot be rendered simultaneously.
    * We will preserve that behaviour and there is consqeuently no need
    * to take any action in this dispose method.
    */
    public void dispose() {
     super.dispose();
    }

}
