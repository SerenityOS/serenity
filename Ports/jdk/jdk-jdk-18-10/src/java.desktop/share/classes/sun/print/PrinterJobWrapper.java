/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.print.PrinterJob;
import java.io.Serial;

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;

public class PrinterJobWrapper implements PrintRequestAttribute {

    /**
     * Use serialVersionUID from JDK 1.8 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8792124426995707237L;

    private PrinterJob job;

    public PrinterJobWrapper(PrinterJob job) {
        this.job = job;
    }

    public PrinterJob getPrinterJob() {
        return job;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterJobWrapper.class;
    }

    public final String getName() {
        return "printerjob-wrapper";
    }

    public String toString() {
       return "printerjob-wrapper: " + job.toString();
    }

    public int hashCode() {
        return job.hashCode();
    }
}
