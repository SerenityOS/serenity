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
 * Package {@code javax.print.attribute.standard} contains classes for specific
 * printing attributes. The parent package, <a href="../package-summary.html">
 * javax.print.attribute</a>, provides classes and interfaces that describe the
 * types of Java Print Service attributes and how they can be collected into
 * attribute sets.
 * <p>
 * An attribute represents a printing feature that a print service can provide.
 * For each attribute, a print service either does or does not support the
 * attribute. For each possible value of a supported attribute, a print service
 * either does or does not support the value.
 * <p>
 * The API requires every print service to support certain attributes; other
 * attributes are optional and the service can choose whether or not to support
 * them. Each attribute has a set of values that it accepts. The API requires
 * every print service to support certain values for certain attributes; other
 * attribute values are optional and the service can choose whether or not to
 * support them. These support requirements are recorded in the documentation
 * for each attribute class.
 * <p>
 * Package {@code javax.print.attribute.standard} contains standard printing
 * attributes and standard printing attribute values that are widely used in the
 * printing domain. A print service vendor can provide new vendor-specific
 * printing attributes in addition to the standard ones. A vendor can also
 * provide vendor-specific extensions (subclasses) of the standard printing
 * attributes -- for example, to provide additional vendor-specific values for
 * an existing standard attribute. Of course, if a vendor wants clients to be
 * able to use any added or extended attributes, the vendor must publish the new
 * attribute classes.
 * <p>
 * Many of the standard attribute classes extend one of the abstract syntax
 * classes of the {@code javax.print.attribute} package. These abstract syntax
 * classes each represent a different type. The <a href="../EnumSyntax.html">
 * EnumSyntax</a> class, for example, represents a type-safe enumeration. The
 * abstract syntax class provides a wrapper for the attribute value.
 * <p>
 * If an attribute class extends {@code EnumSyntax}, and the value of the
 * attribute is an IPP-compatible value, the attribute's {@code toString} method
 * returns the IPP string representation of the attribute value, such as
 * "processing-stopped" for the <a href="JobState.html">JobState</a> attribute.
 * However, because the {@code EnumSyntax} class is extensible, vendors can
 * define their own attribute values. If an attribute uses the
 * {@code EnumSyntax} class and is set to one of these vendor-defined values
 * then the {@code toString} method will not return the IPP string
 * representation of the value.
 * <p>
 * A printing client application will typically not need to use all the printing
 * attribute classes in package javax.print.attribute.standard, just the ones
 * that pertain to the application.
 * <p>
 * The attribute classes in package {@code javax.print.attribute.standard} are
 * based on the Internet Printing Protocol (IPP) attributes as defined in the
 * Internet RFC document, <i>RFC 2911 Internet Printing Protocol/1.1: Model and
 * Semantics</i> dated September 2000. See
 * <a href="http://www.ietf.org/rfc/rfc2911.txt">RFC 2911</a> for more
 * information. The descriptive text for each attribute class was taken largely
 * from the above documents. The above authors' contribution to the API is
 * gratefully acknowledged.
 *
 * <h2>Attribute Organization</h2>
 * There are five kinds of printing attributes: doc attributes, print request
 * attributes, print job attributes, print service attributes, and
 * supported-values attributes.
 *
 * <h3>Doc Attributes</h3>
 * Doc attributes specify the characteristics of an individual doc and the print
 * job settings to be applied to an individual doc. A doc attribute class
 * implements interface <a href="../DocAttribute.html">DocAttribute</a>. A doc
 * attribute can appear in a <a href="../DocAttributeSet.html">
 * DocAttributeSet</a>.
 *
 * <h3>Print Request Attributes</h3>
 * Print request attributes specify the settings to be applied to a whole print
 * job and to all the docs in the print job. A print request attribute class
 * implements interface <a href="../PrintRequestAttribute.html">
 * PrintRequestAttribute</a>. A print request attribute can appear in a
 * <a href="../PrintRequestAttributeSet.html">PrintRequestAttributeSet</a>.
 * <p>
 * Some attributes are doc attributes but not print request attributes and may
 * only be specified at the doc level. Some attributes are print request
 * attributes but not doc attributes and may only be specified at the Print
 * Request level. Some attributes are both doc attributes and print request
 * attributes and may be specified either at the doc level or at the Print
 * Request level.
 * <p>
 * When specified at the doc level, an attribute applies just to that one doc.
 * When specified at the Print Request level, an attribute applies to the whole
 * job, including all the docs in the job. However, an attribute specified at
 * the doc level overrides an attribute in the same category specified at the
 * Print Request level.
 *
 * <h3>Print Job Attributes</h3>
 * Print job attributes report the status of a Print Job. A print job attribute
 * class implements interface <a href="../PrintJobAttribute.html">
 * PrintJobAttribute</a>. A print job attribute can appear in a
 * <a href="../PrintJobAttributeSet.html">PrintJobAttributeSet</a>.
 * <p>
 * Some attributes are both print request attributes and print job attributes; a
 * client may include such attributes in a Print Request to specify
 * characteristics for the ensuing Print Job, and those attributes then also
 * appear in the Print Job's attribute set. Some attributes are print job
 * attributes but not print request attributes; the print service itself adds
 * these attributes to the Print Job's attribute set.
 *
 * <h3>Print Service Attributes</h3>
 * Print service attributes report the status of a print service. A print
 * service attribute class implements interface
 * <a href="../PrintServiceAttribute.html">PrintServiceAttribute</a>. A print
 * service attribute can appear in a <a href="../PrintServiceAttributeSet.html">
 * PrintServiceAttributeSet</a>.
 *
 * <h3>Supported-Values Attributes</h3>
 * A supported-value attribute indicates the legal values for another attribute
 * that a print service supports. A supported-values attribute class implements
 * interface <a href="../SupportedValuesAttribute.html">
 * SupportedValuesAttribute</a>. However, supported-values attributes never
 * appear in attribute sets, so there is no restricted
 * <a href="../AttributeSet.html">AttributeSet</a> subinterface for them.
 *
 * <h3>Attribute Table</h3>
 * The table below lists all the printing attributes. The table shows the
 * tagging interfaces each attribute class implements in addition to interface
 * <a href="../Attribute.html"> Attribute</a>, thus indicating how each
 * attribute is used in the API. For each doc attribute and print request
 * attribute, the column marked "SupportedValuesAttribute" lists the
 * supported-values attribute class, if any, with which a print service
 * indicates the supported values for that attribute category.
 *
 * <table class="striped">
 * <caption>Lists all printing attributes as described above</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Attribute Class
 *     <th scope="col">Doc<br>Attribute
 *     <th scope="col">Print<br>Request<br>Attribute
 *     <th scope="col">Print<br>Job<br>Attribute
 *     <th scope="col">Print<br>Service<br>Attribute
 *     <th scope="col">SupportedValuesAttribute
 * </thead>
 * <tbody style="text-align: center">
 *   <tr>
 *     <th scope="row"><a href="Compression.html">Compression</a>
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="DocumentName.html">DocumentName</a>
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="Chromaticity.html">Chromaticity</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="Copies.html">Copies</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td><a href="CopiesSupported.html">CopiesSupported</a>
 *   <tr>
 *     <th scope="row"><a href="Finishings.html">Finishings</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobHoldUntil.html">JobHoldUntil</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobImpressions.html">JobImpressions</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td><a href="JobImpressionsSupported.html">JobImpressionsSupported</a>
 *   <tr>
 *     <th scope="row"><a href="JobKOctets.html">JobKOctets</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td><a href="JobKOctetsSupported.html">JobKOctetsSupported</a>
 *   <tr>
 *     <th scope="row"><a href="JobMediaSheets.html">JobMediaSheets</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td><a href="JobMediaSheetsSupported.html">JobMediaSheetsSupported</a>
 *   <tr>
 *     <th scope="row"><a href="JobName.html">JobName</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobPriority.html">JobPriority</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td><a href="JobPrioritySupported.html">JobPrioritySupported</a>
 *   <tr>
 *     <th scope="row"><a href="JobSheets.html">JobSheets</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="Media.html">Media</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="MediaSize.html">MediaSize</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="MultipleDocumentHandling.html">
 *     MultipleDocumentHandling</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="NumberUp.html">NumberUp</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td><a href="NumberUpSupported.html">NumberUpSupported</a>
 *   <tr>
 *     <th scope="row"><a href="OrientationRequested.html">
 *     OrientationRequested</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PageRanges.html">PageRanges</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PresentationDirection.html">
 *     PresentationDirection</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterResolution.html">PrinterResolution</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrintQuality.html">PrintQuality</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="RequestingUserName.html">RequestingUserName</a>
 *     <td>&nbsp;
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="SheetCollate.html">SheetCollate</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="Sides.html">Sides</a>
 *     <td>X
 *     <td>X
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="DateTimeAtCompleted.html">
 *     DateTimeAtCompleted</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="DateTimeAtCreation.html">DateTimeAtCreation</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="DateTimeAtProcessing.html">
 *     DateTimeAtProcessing</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobImpressionsCompleted.html">
 *     JobImpressionsCompleted</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobKOctetsProcessed.html">
 *     JobKOctetsProcessed</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobMediaSheetsCompleted.html">
 *     JobMediaSheetsCompleted</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobMessageFromOperator.html">
 *     JobMessageFromOperator</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobOriginatingUserName.html">
 *     JobOriginatingUserName</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobState.html">JobState</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="JobStateReasons.html">JobStateReasons</a>
 *     <br>
 *     Contains zero or more --
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row">-- <a href="JobStateReason.html">JobStateReason</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="NumberOfDocuments.html">NumberOfDocuments</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="NumberOfInterveningJobs.html">
 *     NumberOfInterveningJobs</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="OutputDeviceAssigned.html">
 *     OutputDeviceAssigned</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="ColorSupported.html">ColorSupported</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PagesPerMinute.html">PagesPerMinute</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PagesPerMinuteColor.html">
 *     PagesPerMinuteColor</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PDLOverrideSupported.html">
 *     PDLOverrideSupported</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterIsAcceptingJobs.html">
 *     PrinterIsAcceptingJobs</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterInfo.html">PrinterInfo</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterLocation.html">PrinterLocation</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterMessageFromOperator.html">
 *     PrinterMessageFromOperator</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterMakeAndModel.html">
 *     PrinterMakeAndModel</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterMoreInfo.html">PrinterMoreInfo</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterMoreInfoManufacturer.html">
 *     PrinterMoreInfoManufacturer</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterName.html">PrinterName</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterState.html">PrinterState</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="PrinterStateReasons.html">
 *     PrinterStateReasons</a>
 *     <br>
 *     Contains zero or more --
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row">-- <a href="PrinterStateReason.html">
 *     PrinterStateReason</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row">-- <a href="Severity.html">Severity</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="QueuedJobCount.html">QueuedJobCount</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>X
 *     <td>&nbsp;
 *   <tr>
 *     <th scope="row"><a href="ReferenceUriSchemesSupported.html">
 *     ReferenceUriSchemesSupported</a>
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 *     <td>&nbsp;
 * </tbody>
 * </table>
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
package javax.print.attribute.standard;
