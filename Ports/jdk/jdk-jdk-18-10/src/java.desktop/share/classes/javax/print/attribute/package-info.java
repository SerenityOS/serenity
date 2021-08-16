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
 * Provides classes and interfaces that describe the types of Java Print
 * Service attributes and how they can be collected into attribute sets.
 *
 * <h2>What is an Attribute?</h2>
 * When setting up a print job, a client specifies two things: <b>print data</b>
 * and <b>processing instructions.</b> The print data is the actual content to
 * be printed. The processing instructions tell the printer how to print the
 * print data, such as: what media to use, how many copies to print, and whether
 * to print on one or both sides of a sheet. The client specifies these
 * processing instructions with the attribute definitions of the Java Print
 * Service API.
 * <p>
 * The print data and the processing instructions are separate entities. This
 * means that:
 * <ul>
 *   <li>You can print the same print data at different times using different
 *   processing instructions.
 *   <br>
 *   For example, you can print a slide presentation on US letter-sized white
 *   paper, double-sided, stapled, 20 copies to make handouts for a talk; and
 *   you could print the same slide presentation on US letter-sized
 *   transparencies, single-sided, one copy to make the actual slides for the
 *   talk.
 *   <li>You can use the same processing instructions at different times to
 *   print different data. For example, you could set your default processing
 *   instructions to: US letter-sized paper, double sided, stapled. Whenever you
 *   print a job, it prints with these settings, unless you explicitly override
 *   them.
 * </ul>
 * The processing instruction does not specify how the print job processes the
 * request; each processing instruction is only a description of the results of
 * a print job. The print job determines the manner in which it achieves the
 * results specified by the processing instructions. Representing processing
 * instructions as descriptive items provides more flexibility for implementing
 * print jobs.
 *
 * <h3>Attribute Categories and Values</h3>
 * Each printer has a set of capabilities, such as the ability to print on
 * different paper sizes or the ability to print more than one copy. Each of the
 * capabilities has a range of values. For example, a printer's orientation
 * capability might have this range of values: [landscape, portrait]. For each
 * print request, the capability is set to one of these values. The Java Print
 * Service API uses the term <b>attribute category</b> to refer to a printer
 * capability and the term <b>attribute value</b> to refer to the value of the
 * capability.
 * <p>
 * In the Java Print Service API, an attribute category is represented by a Java
 * class implementing the <a href="Attribute.html">Attribute</a> interface.
 * Attribute values are instances of such a class or one of its subclasses. For
 * example, to specify the number of copies, an application constructs an
 * instance of the <a href="standard/Copies.html">Copies</a> class with the
 * number of desired copies and uses the {@code Copies} instance as part of the
 * print request. In this case, the {@code Copies} class represents the
 * attribute category, and the {@code Copies} instance represents the attribute
 * value.
 *
 * <h3><a id="role"></a>Attribute Roles</h3>
 * When submitting a print job to a printer, the client provides the attributes
 * describing the characteristics of the print data, such as the document name,
 * and how the print data should be printed, such as double-sided, five copies.
 * If a print job consists of multiple pieces of print data, different pieces
 * might have different processing instructions, such as 8 x 11 inch media for
 * the first document, and 11 x 17 inch media for another document.
 * <p>
 * Once the printer starts processing the print job, additional information
 * about the job becomes available, which might include: the job state (such as
 * <i>completed</i> or <i>queued</i>) and the number of pages printed so far.
 * These pieces of information are also attributes. Attributes can also describe
 * the printer itself, such as: the printer name, the printer location, and the
 * number of jobs queued.
 * <p>
 * The Java Print Service API defines these different kinds of attributes with
 * five subinterfaces of {@code Attribute}:
 * <ul>
 *   <li><a href="DocAttribute.html">DocAttribute</a> specifies a characteristic
 *   of an individual document and the print job settings to be applied to an
 *   individual document.
 *   <li><a href="PrintRequestAttribute.html">PrintRequestAttribute</a>
 *   specifies a setting applied to a whole print job and to all the documents
 *   in the print job.
 *   <li><a href="PrintJobAttribute.html">PrintJobAttribute</a> reports the
 *   status of a print job.
 *   <li><a href="PrintServiceAttribute.html">PrintServiceAttribute</a> reports
 *   the status of a print service.
 *   <li><a href="SupportedValuesAttribute.html">SupportedValuesAttribute</a>
 *   gives the supported values for another attribute.
 * </ul>
 * Each attribute class implements one or more of these tagging subinterfaces to
 * indicate where the attribute can be used in the API. If an attribute class
 * implements multiple tagging subinterfaces, the attribute can be used in
 * multiple contexts. For example, the media attribute can apply to one document
 * in a print job as a {@code DocAttribute} or to an entire print job as a
 * {@code PrintRequestAttribute}. Certain low-level attributes are never used on
 * their own but are always aggregated into higher-level attributes. These
 * low-level attribute classes only implement interface
 * <a href="Attribute.html">Attribute</a>, not any of the tagging subinterfaces.
 * <p>
 * The Java Print Service API defines a group of standard attribute classes
 * modeled upon the attributes in the Internet Printing Protocol (IPP) version
 * 1.1. The standard attribute classes are in the subpackage
 * {@code javax.print.attribute.standard} to keep the actual attribute classes
 * conceptually separate from the generic apparatus defined in package
 * {@code javax.print.attribute}.
 *
 * <h2>Attribute Sets</h2>
 * A client usually needs to provide more than one processing instruction when
 * submitting a print job. For example, the client might need to specify a media
 * size of A4 and a landscape orientation. To send more than one processing
 * instruction, the client collects the attributes into an attribute set, which
 * the Java Print Service API represents with the
 * <a href="AttributeSet.html">AttributeSet</a> interface.
 * <p>
 * The {@code AttributeSet} interface is similar to the
 * {@link java.util.Map Map} interface: it provides a map of
 * key to values, in which each key is unique and can contain no more than one
 * value. However, the {@code AttributeSet} interface is designed to
 * specifically support the needs of the Java Print Service API. An
 * {@code AttributeSet} requires that:
 * <ol type=1>
 *   <li>Each key in an {@code AttributeSet} corresponds to a category, and the
 *   value of the key can only be one of the attribute values that belong to the
 *   category represented by the key. Thus, unlike a {@code Map}, an
 *   {@code AttributeSet} restricts the possible values of a key: an attribute
 *   category cannot be set to an attribute value that does not belong to that
 *   category.
 *   <li>No two attributes from the same category can exist in the same set. For
 *   example, an attribute collection must not contain both a "one-sided"
 *   attribute and a "two-sided" attribute because these two attributes give the
 *   printer conflicting instructions.
 *   <li>Only attributes implementing the {@code Attribute} interface can be
 *   added to the set.
 * </ol>
 * The {@code javax.print.attribute} package includes
 * <a href="HashAttributeSet.html">HashAttributeSet</a> as a concrete
 * implementation of the attribute set interface. {@code HashAttributeSet}
 * provides an attribute set based on a hash map. You can use this
 * implementation or provide your own implementation of interface
 * {@code AttributeSet}.
 * <p>
 * The Java Print Service API provides four specializations of an attribute set
 * that are restricted to contain just one of the four kinds of attributes, as
 * discussed in the <a href="#role">Attribute Roles</a> section:
 * <ul>
 *   <li><a href="DocAttributeSet.html">DocAttributeSet</a>
 *   <li><a href="PrintRequestAttributeSet.html">PrintRequestAttributeSet</a>
 *   <li><a href="PrintJobAttributeSet.html"> PrintJobAttributeSet</a>
 *   <li><a href="PrintServiceAttributeSet.html">PrintServiceAttributeSet</a>
 * </ul>
 * Notice that only four kinds of attribute sets are listed here, but there are
 * five kinds of attributes. Interface
 * <a href="SupportedValuesAttribute.html">SupportedValuesAttribute</a> denotes
 * an attribute that gives the supported values for another attribute.
 * Supported-values attributes are never aggregated into attribute sets, so
 * there is no attribute set subinterface defined for them.
 * <p>
 * In some contexts, an attribute set is read-only, which means that the client
 * is only allowed to examine an attribute set's contents but not change them.
 * In other contexts, the attribute set is read-write, which means that the
 * client is allowed both to examine and to change an attribute set's contents.
 * For a read-only attribute set, calling a mutating operation throws an
 * {@code UnmodifiableSetException}.
 * <p>
 * Package {@code javax.print.attribute} includes one concrete implementation of
 * each of the attribute set subinterfaces:
 * <ul>
 *   <li><a href="HashDocAttributeSet.html"> HashDocAttributeSet</a>
 *   <li><a href="HashPrintRequestAttributeSet.html">
 *   HashPrintRequestAttributeSet</a>,
 *   <li><a href="HashPrintJobAttributeSet.html">HashPrintJobAttributeSet</a>,
 *   <li><a href="HashPrintServiceAttributeSet.html">
 *   HashPrintServiceAttributeSet</a>.
 * </ul>
 * All of these classes extend
 * <a href="HashAttributeSet.html">HashAttributeSet</a> and enforce the
 * restriction that the attribute set is only allowed to contain the
 * corresponding kind of attribute.
 *
 * <h2>Attribute Class Design</h2>
 * An attribute value is a small, atomic data item, such as an integer or an
 * enumerated value. The Java Print Service API does not use primitive data
 * types, such as int, to represent attribute values for these reasons:
 * <ul>
 *   <li>Primitive data types are not type-safe. For example, a compiler should
 *   not allow a "copies" attribute value to be used for a "sides" attribute.
 *   <li>Some attributes must be represented as a record of several values. One
 *   example is printer resolution, which requires two numbers, such as 600 and
 *   300 representing 600 x 300 dpi.
 * </ul>
 * For type-safety and to represent all attributes uniformly, the Java Print
 * Service API defines each attribute category as a class, such as class
 * {@code Copies}, class <a href="standard/Sides.html">Sides</a>, and class
 * <a href="standard/PrinterResolution.html">PrinterResolution</a>. Each
 * attribute class wraps one or more primitive data items containing the
 * attribute's value. Attribute set operations perform frequent comparisons
 * between attribute category objects when adding attributes, finding existing
 * attributes in the same category, and looking up an attribute given its
 * category. Because an attribute category is represented by a class, fast
 * attribute-value comparisons can be performed with the {@code Class.equals}
 * method.
 * <p>
 * Even though the Java Print Service API includes a large number of different
 * attribute categories, there are only a few different types of attribute
 * values. Most attributes can be represented by a small number of data types,
 * such as: integer values, integer ranges, text, or an enumeration of integer
 * values. The type of the attribute value that a category accepts is called the
 * attribute's abstract syntax. To provide consistency and reduce code
 * duplication, the Java Print Service API defines abstract syntax classes to
 * represent each abstract syntax, and these classes are used as the parent of
 * standard attributes whenever possible. The abstract syntax classes are:
 * <ul>
 *   <li><a href="EnumSyntax.html">EnumSyntax</a> provides a type-safe
 *   enumeration in which enumerated values are represented as singleton
 *   objects. Each enumeration singleton is an instance of the enumeration class
 *   that wraps a hidden int value.
 *   <li><a href="IntegerSyntax.html">IntegerSyntax</a> is the abstract syntax
 *   for integer-valued attributes.
 *   <li><a href="TextSyntax.html">TextSyntax</a> is the abstract syntax for
 *   text-valued attributes, and includes a locale giving the text string's
 *   natural language.
 *   <li><a href="SetOfIntegerSyntax.html">SetOfIntegerSyntax</a> is the
 *   abstract syntax for attributes representing a range or set of integers
 *   <li><a href="ResolutionSyntax.html">ResolutionSyntax</a> is the abstract
 *   syntax for attributes representing resolution values, such as 600x300
 *   dpi.
 *   <li><a href="Size2DSyntax.html">Size2DSyntax</a> is the abstract syntax for
 *   attributes representing a two-dimensional size, such as a paper size of
 *   8.5 x 11 inches.
 *   <li><a href="DateTimeSyntax.html">DateTimeSyntax</a> is the abstract syntax
 *   for attributes whose value is a date and time.
 *   <li><a href="URISyntax.html">URISyntax</a> is the abstract syntax for
 *   attributes whose value is a Uniform Resource Indicator.
 * </ul>
 * The abstract syntax classes are independent of the attributes that use them.
 * In fact, applications that have nothing to do with printing can use the
 * abstract syntax classes. Although most of the standard attribute classes
 * extend one of the abstract syntax classes, no attribute class is required to
 * extend one of these classes. The abstract syntax classes merely provide a
 * convenient implementation that can be shared by many attribute classes.
 * <p>
 * Each attribute class implements the {@code Attribute} interface, either
 * directly or indirectly, to mark it as a printing attribute. An attribute
 * class that can appear in restricted attribute sets in certain contexts also
 * implements one or more subinterfaces of {@code Attribute}. Most attribute
 * classes also extend the appropriate abstract syntax class to get the
 * implementation. Consider the {@code Sides} attribute class:
 * <blockquote>
 * <pre>{@code
 * public class Sides
 *     extends EnumSyntax
 *     implements DocAttribute, PrintRequestAttribute, PrintJobAttribute
 * {
 *     public final Object getCategory()
 *     {
 *         return Sides.class;
 *     }
 * ...
 * }}
 * </pre>
 * </blockquote>
 * <p>
 * Since every attribute class implements {@code Attribute}, every attribute
 * class must provide an implementation for the
 * {@link javax.print.attribute.Attribute#getCategory() getCategory} method,
 * which returns the attribute category. In the case of {@code Sides}, the
 * {@code getCategory} method returns {@code Sides.class}. The
 * {@code getCategory} method is final to ensure that any vendor-defined
 * subclasses of a standard attribute class appear in the same category. Every
 * attribute object is immutable once constructed so that attribute object
 * references can be passed around freely. To get a different attribute value,
 * construct a different attribute object.
 *
 * <h2>Attribute Vendors</h2>
 * The Java Print Service API is designed so that vendors can:
 * <ul>
 *   <li>define new vendor-specific values for any standard attribute defined in
 *   <a href="standard/package-summary.html">javax.print.attribute.standard</a>.
 *   <li>define new attribute categories representing the vendor printer's
 *   proprietary capabilities not already supported by the standard attributes.
 * </ul>
 * To define a new value for an attribute, a client can construct instances of
 * such attributes with arbitrary values at runtime. However, an enumerated
 * attribute using an abstract syntax class of {@code EnumSyntax} specifies all
 * the possible attribute values at compile time as singleton instances of the
 * attribute class. This means that new enumerated values cannot be constructed
 * at run time. To define new vendor-specific values for a standard enumerated
 * attribute, the vendor must define a new attribute class specifying the new
 * singleton instances. To ensure that the new attribute values fall in the same
 * category as the standard attribute values, the new attribute class must be a
 * subclass of the standard attribute class.
 * <p>
 * To define a new attribute category, a vendor defines a new attribute class.
 * This attribute class, like the standard attribute classes, implements
 * {@code Attribute} or one of its subinterfaces and extends an abstract syntax
 * class. The vendor can either use an existing abstract syntax class or define
 * a new one. The new vendor-defined attribute can be used wherever an
 * {@code Attribute} is used, such as in an {@code AttributeSet}.
 *
 * <h2>Using Attributes</h2>
 * A typical printing application uses the {@code PrintRequestAttributeSet}
 * because print-request attributes are the types of attributes that client
 * usually specifies. This example demonstrates creating an attribute set of
 * print-request attributes and locating a printer that can print the document
 * according to the specified attributes:
 * <blockquote>
 * <pre>{@code
 * FileInputStream psStream;
 * try {
 *     psstream = new FileInputStream("file.ps");
 * } catch (FileNotFoundException ffne) {
 * }
 * if (psstream == null) {
 *     return;
 * }
 * //Set the document type. See the DocFlavor documentation for
 * //more information.
 * DocFlavor psInFormat = DocFlavor.INPUT_STREAM.POSTSCRIPT;
 * Doc myDoc = new SimpleDoc(pstream, psInFormat, null);
 * PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
 * aset.add(new Copies(5));
 * aset.add(MediaSize.A4);
 * aset.add(Sides.DUPLEX);
 * PrintService[] services =
 *     PrintServiceLookup.lookupPrintServices(psInFormat, aset);
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
package javax.print.attribute;
