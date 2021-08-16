/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print;

import javax.print.attribute.PrintRequestAttributeSet;

/**
 * Obtained from a {@code MultiDocPrintService}, a {@code MultiDocPrintJob} can
 * print a specified collection of documents as a single print job with a set of
 * job attributes.
 */
public interface MultiDocPrintJob extends DocPrintJob {

    /**
     * Print a {@code MultiDoc} with the specified job attributes. This method
     * should only be called once for a given print job. Calling it again will
     * not result in a new job being spooled to the printer. The service
     * implementation will define policy for service interruption and recovery.
     * Application clients which want to monitor the success or failure should
     * register a {@code PrintJobListener}.
     *
     * @param  multiDoc the documents to be printed. ALL must be a flavor
     *         supported by the PrintJob {@literal &} PrintService.
     * @param  attributes the job attributes to be applied to this print job. If
     *         this parameter is {@code null} then the default attributes are
     *         used.
     * @throws PrintException the exception additionally may implement an
     *         interfaces which more precisely describes the cause of the
     *         exception
     *         <ul>
     *           <li>{@code FlavorException}. If the document has a flavor not
     *           supported by this print job.
     *           <li>{@code AttributeException}. If one or more of the
     *           attributes are not valid for this print job.
     *         </ul>
     */
    public void print(MultiDoc multiDoc, PrintRequestAttributeSet attributes)
                throws PrintException;
}
