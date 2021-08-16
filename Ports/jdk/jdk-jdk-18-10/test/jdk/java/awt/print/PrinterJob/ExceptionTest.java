/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @key printer
 * @bug 6467557
 * @summary No exception should be thrown.
 * @run main ExceptionTest
 */

import java.awt.*;
import java.awt.print.*;

public class ExceptionTest {
private TextCanvas c;

public static void main(String args[]) {
    ExceptionTest f = new ExceptionTest();
}

public ExceptionTest() {
    c = new TextCanvas();
    PrinterJob pj = PrinterJob.getPrinterJob();

    if (pj != null) {

        pj.setPageable(c);
        try {
           pj.print();
        } catch (PrinterException pe) {
            if (!(pe.getCause() instanceof IndexOutOfBoundsException)) {
              throw new RuntimeException("initCause of Exception not thrown");
            }
        }
    }
}


class TextCanvas extends Panel implements Pageable, Printable {

    public static final int MAXPAGE = 8;

    public int getNumberOfPages() {
        return MAXPAGE;
    }

    public PageFormat getPageFormat(int pageIndex) {
       if (pageIndex > MAXPAGE) throw new IndexOutOfBoundsException();
           PageFormat pf = new PageFormat();
       return pf;
    }

    public Printable getPrintable(int pageIndex) {
       if (pageIndex == 1) throw new IndexOutOfBoundsException();
       return this;
    }

    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {
        System.out.println("****"+pgIndex);
        return Printable.PAGE_EXISTS;
    }

}

}
