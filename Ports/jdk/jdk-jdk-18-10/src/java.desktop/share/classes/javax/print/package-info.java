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

/**
 * Provides the principal classes and interfaces for the Java Print
 * Service API. The Java Print Service API enables client and server
 * applications to:
 * <ul>
 *   <li>Discover and select print services based on their capabilities
 *   <li>Specify the format of print data
 *   <li>Submit print jobs to services that support the document type to be
 *   printed.
 * </ul>
 *
 * <h2>Print Service Discovery</h2>
 * An application invokes the static methods of the abstract class
 * {@link javax.print.PrintServiceLookup PrintServiceLookup} to locate print
 * services that have the capabilities to satisfy the application's print
 * request. For example, to print a double-sided document, the application first
 * needs to find printers that have the double-sided printing capability.
 * <p>
 * The JDK includes {@code PrintServiceLookup} implementations that can locate
 * the standard platform printers. To locate other types of printers, such as
 * IPP printers or JINI printers, a print-service provider can write
 * implementations of {@code PrintServiceLookup}. The print-service provider can
 * dynamically install these {@code PrintServiceLookup} implementations using
 * the {@link java.util.ServiceLoader} facility.
 *
 * <h2>Attribute Definitions</h2>
 * The {@link javax.print.attribute} and {@link javax.print.attribute.standard}
 * packages define print attributes, which describe the capabilities of a print
 * service, specify the requirements of a print job, and track the progress of a
 * print job.
 * <p>
 * The {@code javax.print.attribute} package describes the types of attributes
 * and how they can be collected into sets. The
 * {@code javax.print.attribute.standard} package enumerates all of the standard
 * attributes supported by the API, most of which are implementations of
 * attributes specified in the IETF Specification,
 * <a href="http://www.ietf.org/rfc/rfc2911.txt">RFC 2911 Internet Printing
 * Protocol, 1.1: Model and Semantics</a>, dated September 2000. The attributes
 * specified in {@code javax.print.attribute.standard} include common
 * capabilities, such as: resolution, copies, media sizes, job priority, and
 * page ranges.
 *
 * <h2>Document Type Specification</h2>
 * The {@link javax.print.DocFlavor DocFlavor} class represents the print data
 * format, such as JPEG or PostScript. A {@code DocFlavor} object consists of a
 * MIME type, which describes the format, and a document representation class
 * name that indicates how the document is delivered to the printer or output
 * stream. An application uses the {@code DocFlavor} and an attribute set to
 * find printers that can print the document type specified by the
 * {@code DocFlavor} and have the capabilities specified by the attribute set.
 *
 * <h2>Using the API</h2>
 * A typical application using the Java Print Service API performs these steps
 * to process a print request:
 * <ol>
 *   <li>Chooses a {@code DocFlavor}.
 *   <li>Creates a set of attributes.
 *   <li>Locates a print service that can handle the print request as specified
 *   by the {@code DocFlavor} and the attribute set.
 *   <li>Creates a {@link javax.print.Doc Doc} object encapsulating the
 *   {@code DocFlavor} and the actual print data, which can take many forms
 *   including: a Postscript file, a JPEG image, a {@code URL}, or plain text.
 *   <li>Gets a print job, represented by
 *   {@link javax.print.DocPrintJob DocPrintJob}, from the print service.
 *   <li>Calls the print method of the print job.
 * </ol>
 * The following code sample demonstrates a typical use of the Java Print
 * Service API: locating printers that can print five double-sided copies of a
 * Postscript document on size A4 paper, creating a print job from one of the
 * returned print services, and calling print.
 * <blockquote>
 * <pre>{@code
 * FileInputStream psStream;
 * try {
 *     psStream = new FileInputStream("file.ps");
 * } catch (FileNotFoundException ffne) {
 * }
 * if (psStream == null) {
 *     return;
 * }
 * DocFlavor psInFormat = DocFlavor.INPUT_STREAM.POSTSCRIPT;
 * Doc myDoc = new SimpleDoc(psStream, psInFormat, null);
 * PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
 * aset.add(new Copies(5));
 * aset.add(MediaSizeName.ISO_A4);
 * aset.add(Sides.DUPLEX);
 * PrintService[] services =
 * PrintServiceLookup.lookupPrintServices(psInFormat, aset);
 * if (services.length > 0) {
 *     DocPrintJob job = services[0].createPrintJob();
 *     try {
 *         job.print(myDoc, aset);
 *     } catch (PrintException pe) {}
 * }
 * }</pre>
 * </blockquote>
 * <p>
 * Please note: In the {@code javax.print} APIs, a {@code null} reference
 * parameter to methods is incorrect unless explicitly documented on the method
 * as having a meaningful interpretation. Usage to the contrary is incorrect
 * coding and may result in a run time exception either immediately or at some
 * later time. {@code IllegalArgumentException} and {@code NullPointerException}
 * are examples of typical and acceptable run time exceptions for such cases.
 *
 * @since 1.4
 */
package javax.print;
