/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Window;
import java.awt.print.PrinterJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.PrintRequestAttributeSet;

public abstract class DocumentPropertiesUI {

    /**
     * For Win32 doc properties sheet.
     */
    public static final int
        DOCUMENTPROPERTIES_ROLE = ServiceUIFactory.RESERVED_UIROLE +100;

    /**
     * Name of (this) abstract class for Document Properties.
     */
    public static final String
        DOCPROPERTIESCLASSNAME = DocumentPropertiesUI.class.getName();

    /**
     * Invokes whatever code is needed to display a native dialog
     * with the specified owner. The owner should be the cross-platform
     * dialog. If the user cancels the dialog the return value is null.
     * A non-null return value is always a new attribute set (or is it?)
     * The cross-platform dialog may need to be updated to reflect the
     * updated properties.
     */
    public abstract PrintRequestAttributeSet
        showDocumentProperties(PrinterJob job,
                               Window owner,
                               PrintService service,
                               PrintRequestAttributeSet aset);

}
