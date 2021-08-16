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

import java.io.OutputStream;

/**
 * This class extends {@link PrintService} and represents a print service that
 * prints data in different formats to a client-provided output stream. This is
 * principally intended for services where the output format is a document type
 * suitable for viewing or archiving. The output format must be declared as a
 * mime type. This is equivalent to an output document flavor where the
 * representation class is always "java.io.OutputStream" An instance of the
 * {@code StreamPrintService} class is obtained from a
 * {@link StreamPrintServiceFactory} instance.
 * <p>
 * Note that a {@code StreamPrintService} is different from a
 * {@code PrintService}, which supports a
 * {@link javax.print.attribute.standard.Destination Destination} attribute. A
 * {@code StreamPrintService} always requires an output stream, whereas a
 * {@code PrintService} optionally accepts a {@code Destination}. A
 * {@code StreamPrintService} has no default destination for its formatted
 * output. Additionally a {@code StreamPrintService} is expected to generate
 * output in a format useful in other contexts. {@code StreamPrintService}'s are
 * not expected to support the {@code Destination} attribute.
 */
public abstract class StreamPrintService implements PrintService {

    /**
     * The output stream to which this service will send formatted print data.
     */
    private OutputStream outStream;

    /**
     * Whether or not this {@code StreamPrintService} has been disposed.
     */
    private boolean disposed = false;

    /**
     * Constructs a {@code StreamPrintService} object.
     */
    private StreamPrintService() {
    };

    /**
     * Constructs a {@code StreamPrintService} object.
     *
     * @param  out stream to which to send formatted print data
     */
    protected StreamPrintService(OutputStream out) {
        this.outStream = out;
    }

    /**
     * Gets the output stream.
     *
     * @return the stream to which this service will send formatted print data
     */
    public OutputStream getOutputStream() {
        return outStream;
    }

    /**
     * Returns the document format emitted by this print service. Must be in
     * mimetype format, compatible with the mime type components of
     * {@code DocFlavors}
     *
     * @return mime type identifying the output format
     * @see DocFlavor
     */
    public abstract String getOutputFormat();

    /**
     * Disposes this {@code StreamPrintService}. If a stream service cannot be
     * re-used, it must be disposed to indicate this. Typically the client will
     * call this method. Services which write data which cannot meaningfully be
     * appended to may also dispose the stream. This does not close the stream.
     * It just marks it as not for further use by this service.
     */
    public void dispose() {
        disposed = true;
    }

    /**
     * Returns a {@code boolean} indicating whether or not this
     * {@code StreamPrintService} has been disposed. If this object has been
     * disposed, will return {@code true}. Used by services and client
     * applications to recognize streams to which no further data should be
     * written.
     *
     * @return {@code true} if this {@code StreamPrintService} has been
     *         disposed; {@code false} otherwise
     */
    public boolean isDisposed() {
        return disposed;
    }
}
