/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.event;

import java.io.Serial;

import javax.print.DocPrintJob;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.PrintJobAttributeSet;

/**
 * Class {@code PrintJobAttributeEvent} encapsulates an event a
 * {@code PrintService} reports to let the client know that one or more printing
 * attributes for a {@code PrintJob} have changed.
 */
public class PrintJobAttributeEvent extends PrintEvent {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -6534469883874742101L;

    /**
     * The printing service attributes that changed.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private PrintJobAttributeSet attributes;

    /**
     * Constructs a {@code PrintJobAttributeEvent} object.
     *
     * @param  source the print job generating this event
     * @param  attributes the attribute changes being reported
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public PrintJobAttributeEvent (DocPrintJob source,
                                   PrintJobAttributeSet attributes)  {
        super(source);

        this.attributes = AttributeSetUtilities.unmodifiableView(attributes);
    }

    /**
     * Determine the {@code PrintJob} to which this print job event pertains.
     *
     * @return {@code PrintJob} object
     */
    public DocPrintJob getPrintJob() {

        return (DocPrintJob) getSource();
    }

    /**
     * Determine the printing attributes that changed and their new values.
     *
     * @return attributes containing the new values for the print job attributes
     *         that changed. The returned set may not be modifiable.
     */
    public PrintJobAttributeSet getAttributes() {

        return attributes;
    }
}
