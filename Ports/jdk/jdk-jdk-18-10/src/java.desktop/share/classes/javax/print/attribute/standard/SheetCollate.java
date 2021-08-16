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

package javax.print.attribute.standard;

import java.io.Serial;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code SheetCollate} is a printing attribute class, an enumeration,
 * that specifies whether or not the media sheets of each copy of each printed
 * document in a job are to be in sequence, when multiple copies of the document
 * are specified by the {@link Copies Copies} attribute. When
 * {@code SheetCollate} is {@code COLLATED}, each copy of each document is
 * printed with the print-stream sheets in sequence. When {@code SheetCollate}
 * is {@code UNCOLLATED}, each print-stream sheet is printed a number of times
 * equal to the value of the {@link Copies Copies} attribute in succession. For
 * example, suppose a document produces two media sheets as output,
 * {@link Copies Copies} is 6, and {@code SheetCollate} is UNCOLLATED; in this
 * case six copies of the first media sheet are printed followed by six copies
 * of the second media sheet.
 * <p>
 * Whether the effect of sheet collation is achieved by placing copies of a
 * document in multiple output bins or in the same output bin with
 * implementation defined document separation is implementation dependent. Also
 * whether it is achieved by making multiple passes over the job or by using an
 * output sorter is implementation dependent.
 * <p>
 * If a printer does not support the {@code SheetCollate} attribute (meaning the
 * client cannot specify any particular sheet collation), the printer must
 * behave as though {@code SheetCollate} were always set to {@code COLLATED}.
 * <p>
 * The {@code SheetCollate} attribute interacts with the
 * {@link MultipleDocumentHandling MultipleDocumentHandling} attribute. The
 * {@link MultipleDocumentHandling MultipleDocumentHandling} attribute describes
 * the collation of entire documents, and the {@code SheetCollate} attribute
 * describes the semantics of collating individual pages within a document.
 * <p>
 * The effect of a {@code SheetCollate} attribute on a multidoc print job (a job
 * with multiple documents) depends on whether all the docs have the same sheet
 * collation specified or whether different docs have different sheet collations
 * specified, and on the (perhaps defaulted) value of the
 * {@link MultipleDocumentHandling MultipleDocumentHandling} attribute.
 * <ul>
 *   <li>If all the docs have the same sheet collation specified, then the
 *   following combinations of {@code SheetCollate} and
 *   {@link MultipleDocumentHandling MultipleDocumentHandling} are permitted,
 *   and the printer reports an error when the job is submitted if any other
 *   combination is specified:
 *   <ul>
 *     <li>SheetCollate = COLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SINGLE_DOCUMENT -- All the input docs will be
 *     combined into one output document. Multiple copies of the output document
 *     will be produced with pages in collated order, i.e. pages 1, 2, 3, . . .,
 *     1, 2, 3, . . .
 *     <li>SheetCollate = COLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SINGLE_DOCUMENT_NEW_SHEET -- All the input
 *     docs will be combined into one output document, and the first impression
 *     of each input doc will always start on a new media sheet. Multiple copies
 *     of the output document will be produced with pages in collated order,
 *     i.e. pages 1, 2, 3, . . ., 1, 2, 3, . . .
 *     <li>SheetCollate = COLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SEPARATE_DOCUMENTS_UNCOLLATED_COPIES -- Each
 *     input doc will remain a separate output document. Multiple copies of each
 *     output document (call them A, B, . . .) will be produced with each
 *     document's pages in collated order, but the documents themselves in
 *     uncollated order, i.e. pages A1, A2, A3, . . ., A1, A2, A3, . . ., B1,
 *     B2, B3, . . ., B1, B2, B3, . . .
 *     <li>SheetCollate = COLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SEPARATE_DOCUMENTS_COLLATED_COPIES -- Each
 *     input doc will remain a separate output document. Multiple copies of each
 *     output document (call them A, B, . . .) will be produced with each
 *     document's pages in collated order, with the documents themselves also in
 *     collated order, i.e. pages A1, A2, A3, . . ., B1, B2, B3, . . ., A1, A2,
 *     A3, . . ., B1, B2, B3, . . .
 *     <li>SheetCollate = UNCOLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SINGLE_DOCUMENT -- All the input docs will be
 *     combined into one output document. Multiple copies of the output document
 *     will be produced with pages in uncollated order, i.e. pages 1, 1, . . .,
 *     2, 2, . . ., 3, 3, . . .
 *     <li>SheetCollate = UNCOLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SINGLE_DOCUMENT_NEW_SHEET -- All the input
 *     docs will be combined into one output document, and the first impression
 *     of each input doc will always start on a new media sheet. Multiple copies
 *     of the output document will be produced with pages in uncollated order,
 *     i.e. pages 1, 1, . . ., 2, 2, . . ., 3, 3, . . .
 *     <li>SheetCollate = UNCOLLATED, {@link MultipleDocumentHandling
 *     MultipleDocumentHandling} = SEPARATE_DOCUMENTS_UNCOLLATED_COPIES -- Each
 *     input doc will remain a separate output document. Multiple copies of each
 *     output document (call them A, B, . . .) will be produced with each
 *     document's pages in uncollated order, with the documents themselves also
 *     in uncollated order, i.e. pages A1, A1, . . ., A2, A2, . . ., A3, A3, . .
 *     ., B1, B1, . . ., B2, B2, . . ., B3, B3, . . .
 *   </ul>
 *   <li>If different docs have different sheet collations specified, then only
 *   one value of {@link MultipleDocumentHandling MultipleDocumentHandling} is
 *   permitted, and the printer reports an error when the job is submitted if
 *   any other value is specified:
 *   <ul>
 *     <li>{@link MultipleDocumentHandling MultipleDocumentHandling} =
 *     SEPARATE_DOCUMENTS_UNCOLLATED_COPIES -- Each input doc will remain a
 *     separate output document. Multiple copies of each output document (call
 *     them A, B, . . .) will be produced with each document's pages in collated
 *     or uncollated order as the corresponding input doc's SheetCollate
 *     attribute specifies, and with the documents themselves in uncollated
 *     order. If document A had SheetCollate = UNCOLLATED and document B had
 *     SheetCollate = COLLATED, the following pages would be produced: A1, A1, .
 *     . ., A2, A2, . . ., A3, A3, . . ., B1, B2, B3, . . ., B1, B2, B3, . . .
 *   </ul>
 * </ul>
 * <p>
 * <b>IPP Compatibility:</b> SheetCollate is not an IPP attribute at present.
 *
 * @author Alan Kaminsky
 * @see MultipleDocumentHandling
 */
public final class SheetCollate extends EnumSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 7080587914259873003L;

    /**
     * Sheets within a document appear in uncollated order when multiple copies
     * are printed.
     */
    public static final SheetCollate UNCOLLATED = new SheetCollate(0);

    /**
     * Sheets within a document appear in collated order when multiple copies
     * are printed.
     */
    public static final SheetCollate COLLATED = new SheetCollate(1);

    /**
     * Construct a new sheet collate enumeration value with the given integer
     * value.
     *
     * @param  value Integer value
     */
    protected SheetCollate(int value) {
        super (value);
    }

    /**
     * The string table for class {@code SheetCollate}.
     */
    private static final String[] myStringTable = {
        "uncollated",
        "collated"
    };

    /**
     * The enumeration value table for class {@code SheetCollate}.
     */
    private static final SheetCollate[] myEnumValueTable = {
        UNCOLLATED,
        COLLATED
    };

    /**
     * Returns the string table for class {@code SheetCollate}.
     */
    protected String[] getStringTable() {
        return myStringTable;
    }

    /**
     * Returns the enumeration value table for class {@code SheetCollate}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code SheetCollate}, the category is class
     * {@code SheetCollate} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return SheetCollate.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code SheetCollate}, the category name is
     * {@code "sheet-collate"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "sheet-collate";
    }
}
