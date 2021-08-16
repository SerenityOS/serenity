/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

/**
 * A set of attributes which control a print job.
 * <p>
 * Instances of this class control the number of copies, default selection,
 * destination, print dialog, file and printer names, page ranges, multiple
 * document handling (including collation), and multi-page imposition (such
 * as duplex) of every print job which uses the instance. Attribute names are
 * compliant with the Internet Printing Protocol (IPP) 1.1 where possible.
 * Attribute values are partially compliant where possible.
 * <p>
 * To use a method which takes an inner class type, pass a reference to
 * one of the constant fields of the inner class. Client code cannot create
 * new instances of the inner class types because none of those classes
 * has a public constructor. For example, to set the print dialog type to
 * the cross-platform, pure Java print dialog, use the following code:
 * <pre>
 * import java.awt.JobAttributes;
 *
 * public class PureJavaPrintDialogExample {
 *     public void setPureJavaPrintDialog(JobAttributes jobAttributes) {
 *         jobAttributes.setDialog(JobAttributes.DialogType.COMMON);
 *     }
 * }
 * </pre>
 * <p>
 * Every IPP attribute which supports an <i>attributeName</i>-default value
 * has a corresponding <code>set<i>attributeName</i>ToDefault</code> method.
 * Default value fields are not provided.
 *
 * @author      David Mendenhall
 * @since 1.3
 */
public final class JobAttributes implements Cloneable {
    /**
     * A type-safe enumeration of possible default selection states.
     * @since 1.3
     */
    public static final class DefaultSelectionType extends AttributeValue {
        private static final int I_ALL = 0;
        private static final int I_RANGE = 1;
        private static final int I_SELECTION = 2;

        private static final String[] NAMES = {
            "all", "range", "selection"
        };

        /**
         * The {@code DefaultSelectionType} instance to use for
         * specifying that all pages of the job should be printed.
         */
        public static final DefaultSelectionType ALL =
           new DefaultSelectionType(I_ALL);
        /**
         * The {@code DefaultSelectionType} instance to use for
         * specifying that a range of pages of the job should be printed.
         */
        public static final DefaultSelectionType RANGE =
           new DefaultSelectionType(I_RANGE);
        /**
         * The {@code DefaultSelectionType} instance to use for
         * specifying that the current selection should be printed.
         */
        public static final DefaultSelectionType SELECTION =
           new DefaultSelectionType(I_SELECTION);

        private DefaultSelectionType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * A type-safe enumeration of possible job destinations.
     * @since 1.3
     */
    public static final class DestinationType extends AttributeValue {
        private static final int I_FILE = 0;
        private static final int I_PRINTER = 1;

        private static final String[] NAMES = {
            "file", "printer"
        };

        /**
         * The {@code DestinationType} instance to use for
         * specifying print to file.
         */
        public static final DestinationType FILE =
            new DestinationType(I_FILE);
        /**
         * The {@code DestinationType} instance to use for
         * specifying print to printer.
         */
        public static final DestinationType PRINTER =
            new DestinationType(I_PRINTER);

        private DestinationType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * A type-safe enumeration of possible dialogs to display to the user.
     * @since 1.3
     */
    public static final class DialogType extends AttributeValue {
        private static final int I_COMMON = 0;
        private static final int I_NATIVE = 1;
        private static final int I_NONE = 2;

        private static final String[] NAMES = {
            "common", "native", "none"
        };

        /**
         * The {@code DialogType} instance to use for
         * specifying the cross-platform, pure Java print dialog.
         */
        public static final DialogType COMMON = new DialogType(I_COMMON);
        /**
         * The {@code DialogType} instance to use for
         * specifying the platform's native print dialog.
         */
        public static final DialogType NATIVE = new DialogType(I_NATIVE);
        /**
         * The {@code DialogType} instance to use for
         * specifying no print dialog.
         */
        public static final DialogType NONE = new DialogType(I_NONE);

        private DialogType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * A type-safe enumeration of possible multiple copy handling states.
     * It is used to control how the sheets of multiple copies of a single
     * document are collated.
     * @since 1.3
     */
    public static final class MultipleDocumentHandlingType extends
                                                               AttributeValue {
        private static final int I_SEPARATE_DOCUMENTS_COLLATED_COPIES = 0;
        private static final int I_SEPARATE_DOCUMENTS_UNCOLLATED_COPIES = 1;

        private static final String[] NAMES = {
            "separate-documents-collated-copies",
            "separate-documents-uncollated-copies"
        };

        /**
         * The {@code MultipleDocumentHandlingType} instance to use for specifying
         * that the job should be divided into separate, collated copies.
         */
        public static final MultipleDocumentHandlingType
            SEPARATE_DOCUMENTS_COLLATED_COPIES =
                new MultipleDocumentHandlingType(
                    I_SEPARATE_DOCUMENTS_COLLATED_COPIES);
        /**
         * The {@code MultipleDocumentHandlingType} instance to use for specifying
         * that the job should be divided into separate, uncollated copies.
         */
        public static final MultipleDocumentHandlingType
            SEPARATE_DOCUMENTS_UNCOLLATED_COPIES =
                new MultipleDocumentHandlingType(
                    I_SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);

        private MultipleDocumentHandlingType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * A type-safe enumeration of possible multi-page impositions. These
     * impositions are in compliance with IPP 1.1.
     * @since 1.3
     */
    public static final class SidesType extends AttributeValue {
        private static final int I_ONE_SIDED = 0;
        private static final int I_TWO_SIDED_LONG_EDGE = 1;
        private static final int I_TWO_SIDED_SHORT_EDGE = 2;

        private static final String[] NAMES = {
            "one-sided", "two-sided-long-edge", "two-sided-short-edge"
        };

        /**
         * The {@code SidesType} instance to use for specifying that
         * consecutive job pages should be printed upon the same side of
         * consecutive media sheets.
         */
        public static final SidesType ONE_SIDED = new SidesType(I_ONE_SIDED);
        /**
         * The {@code SidesType} instance to use for specifying that
         * consecutive job pages should be printed upon front and back sides
         * of consecutive media sheets, such that the orientation of each pair
         * of pages on the medium would be correct for the reader as if for
         * binding on the long edge.
         */
        public static final SidesType TWO_SIDED_LONG_EDGE =
            new SidesType(I_TWO_SIDED_LONG_EDGE);
        /**
         * The {@code SidesType} instance to use for specifying that
         * consecutive job pages should be printed upon front and back sides
         * of consecutive media sheets, such that the orientation of each pair
         * of pages on the medium would be correct for the reader as if for
         * binding on the short edge.
         */
        public static final SidesType TWO_SIDED_SHORT_EDGE =
            new SidesType(I_TWO_SIDED_SHORT_EDGE);

        private SidesType(int type) {
            super(type, NAMES);
        }
    }

    private int copies;
    private DefaultSelectionType defaultSelection;
    private DestinationType destination;
    private DialogType dialog;
    private String fileName;
    private int fromPage;
    private int maxPage;
    private int minPage;
    private MultipleDocumentHandlingType multipleDocumentHandling;
    private int[][] pageRanges;
    private int prFirst;
    private int prLast;
    private String printer;
    private SidesType sides;
    private int toPage;

    /**
     * Constructs a {@code JobAttributes} instance with default
     * values for every attribute.  The dialog defaults to
     * {@code DialogType.NATIVE}.  Min page defaults to
     * {@code 1}.  Max page defaults to {@code Integer.MAX_VALUE}.
     * Destination defaults to {@code DestinationType.PRINTER}.
     * Selection defaults to {@code DefaultSelectionType.ALL}.
     * Number of copies defaults to {@code 1}. Multiple document handling defaults
     * to {@code MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES}.
     * Sides defaults to {@code SidesType.ONE_SIDED}. File name defaults
     * to {@code null}.
     */
    public JobAttributes() {
        setCopiesToDefault();
        setDefaultSelection(DefaultSelectionType.ALL);
        setDestination(DestinationType.PRINTER);
        setDialog(DialogType.NATIVE);
        setMaxPage(Integer.MAX_VALUE);
        setMinPage(1);
        setMultipleDocumentHandlingToDefault();
        setSidesToDefault();
    }

    /**
     * Constructs a {@code JobAttributes} instance which is a copy
     * of the supplied {@code JobAttributes}.
     *
     * @param   obj the {@code JobAttributes} to copy
     */
    public JobAttributes(JobAttributes obj) {
        set(obj);
    }

    /**
     * Constructs a {@code JobAttributes} instance with the
     * specified values for every attribute.
     *
     * @param   copies an integer greater than 0
     * @param   defaultSelection {@code DefaultSelectionType.ALL},
     *          {@code DefaultSelectionType.RANGE}, or
     *          {@code DefaultSelectionType.SELECTION}
     * @param   destination {@code DestinationType.FILE} or
     *          {@code DestinationType.PRINTER}
     * @param   dialog {@code DialogType.COMMON},
     *          {@code DialogType.NATIVE}, or
     *          {@code DialogType.NONE}
     * @param   fileName the possibly {@code null} file name
     * @param   maxPage an integer greater than zero and greater than or equal
     *          to <i>minPage</i>
     * @param   minPage an integer greater than zero and less than or equal
     *          to <i>maxPage</i>
     * @param   multipleDocumentHandling
     *     {@code MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_COLLATED_COPIES} or
     *     {@code MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES}
     * @param   pageRanges an array of integer arrays of two elements; an array
     *          is interpreted as a range spanning all pages including and
     *          between the specified pages; ranges must be in ascending
     *          order and must not overlap; specified page numbers cannot be
     *          less than <i>minPage</i> nor greater than <i>maxPage</i>;
     *          for example:
     *          <pre>
     *          (new int[][] { new int[] { 1, 3 }, new int[] { 5, 5 },
     *                         new int[] { 15, 19 } }),
     *          </pre>
     *          specifies pages 1, 2, 3, 5, 15, 16, 17, 18, and 19. Note that
     *          ({@code new int[][] { new int[] { 1, 1 }, new int[] { 1, 2 } }}),
     *          is an invalid set of page ranges because the two ranges
     *          overlap
     * @param   printer the possibly {@code null} printer name
     * @param   sides {@code SidesType.ONE_SIDED},
     *          {@code SidesType.TWO_SIDED_LONG_EDGE}, or
     *          {@code SidesType.TWO_SIDED_SHORT_EDGE}
     * @throws  IllegalArgumentException if one or more of the above
     *          conditions is violated
     */
    public JobAttributes(int copies, DefaultSelectionType defaultSelection,
                         DestinationType destination, DialogType dialog,
                         String fileName, int maxPage, int minPage,
                         MultipleDocumentHandlingType multipleDocumentHandling,
                         int[][] pageRanges, String printer, SidesType sides) {
        setCopies(copies);
        setDefaultSelection(defaultSelection);
        setDestination(destination);
        setDialog(dialog);
        setFileName(fileName);
        setMaxPage(maxPage);
        setMinPage(minPage);
        setMultipleDocumentHandling(multipleDocumentHandling);
        setPageRanges(pageRanges);
        setPrinter(printer);
        setSides(sides);
    }

    /**
     * Creates and returns a copy of this {@code JobAttributes}.
     *
     * @return  the newly created copy; it is safe to cast this Object into
     *          a {@code JobAttributes}
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Since we implement Cloneable, this should never happen
            throw new InternalError(e);
        }
    }

    /**
     * Sets all of the attributes of this {@code JobAttributes} to
     * the same values as the attributes of obj.
     *
     * @param   obj the {@code JobAttributes} to copy
     */
    public void set(JobAttributes obj) {
        copies = obj.copies;
        defaultSelection = obj.defaultSelection;
        destination = obj.destination;
        dialog = obj.dialog;
        fileName = obj.fileName;
        fromPage = obj.fromPage;
        maxPage = obj.maxPage;
        minPage = obj.minPage;
        multipleDocumentHandling = obj.multipleDocumentHandling;
        // okay because we never modify the contents of pageRanges
        pageRanges = obj.pageRanges;
        prFirst = obj.prFirst;
        prLast = obj.prLast;
        printer = obj.printer;
        sides = obj.sides;
        toPage = obj.toPage;
    }

    /**
     * Returns the number of copies the application should render for jobs
     * using these attributes. This attribute is updated to the value chosen
     * by the user.
     *
     * @return  an integer greater than 0.
     */
    public int getCopies() {
        return copies;
    }

    /**
     * Specifies the number of copies the application should render for jobs
     * using these attributes. Not specifying this attribute is equivalent to
     * specifying {@code 1}.
     *
     * @param   copies an integer greater than 0
     * @throws  IllegalArgumentException if {@code copies} is less than
     *      or equal to 0
     */
    public void setCopies(int copies) {
        if (copies <= 0) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "copies");
        }
        this.copies = copies;
    }

    /**
     * Sets the number of copies the application should render for jobs using
     * these attributes to the default. The default number of copies is 1.
     */
    public void setCopiesToDefault() {
        setCopies(1);
    }

    /**
     * Specifies whether, for jobs using these attributes, the application
     * should print all pages, the range specified by the return value of
     * {@code getPageRanges}, or the current selection. This attribute
     * is updated to the value chosen by the user.
     *
     * @return  DefaultSelectionType.ALL, DefaultSelectionType.RANGE, or
     *          DefaultSelectionType.SELECTION
     */
    public DefaultSelectionType getDefaultSelection() {
        return defaultSelection;
    }

    /**
     * Specifies whether, for jobs using these attributes, the application
     * should print all pages, the range specified by the return value of
     * {@code getPageRanges}, or the current selection. Not specifying
     * this attribute is equivalent to specifying DefaultSelectionType.ALL.
     *
     * @param   defaultSelection DefaultSelectionType.ALL,
     *          DefaultSelectionType.RANGE, or DefaultSelectionType.SELECTION.
     * @throws  IllegalArgumentException if defaultSelection is {@code null}
     */
    public void setDefaultSelection(DefaultSelectionType defaultSelection) {
        if (defaultSelection == null) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "defaultSelection");
        }
        this.defaultSelection = defaultSelection;
    }

    /**
     * Specifies whether output will be to a printer or a file for jobs using
     * these attributes. This attribute is updated to the value chosen by the
     * user.
     *
     * @return  DestinationType.FILE or DestinationType.PRINTER
     */
    public DestinationType getDestination() {
        return destination;
    }

    /**
     * Specifies whether output will be to a printer or a file for jobs using
     * these attributes. Not specifying this attribute is equivalent to
     * specifying DestinationType.PRINTER.
     *
     * @param   destination DestinationType.FILE or DestinationType.PRINTER.
     * @throws  IllegalArgumentException if destination is null.
     */
    public void setDestination(DestinationType destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "destination");
        }
        this.destination = destination;
    }

    /**
     * Returns whether, for jobs using these attributes, the user should see
     * a print dialog in which to modify the print settings, and which type of
     * print dialog should be displayed. DialogType.COMMON denotes a cross-
     * platform, pure Java print dialog. DialogType.NATIVE denotes the
     * platform's native print dialog. If a platform does not support a native
     * print dialog, the pure Java print dialog is displayed instead.
     * DialogType.NONE specifies no print dialog (i.e., background printing).
     * This attribute cannot be modified by, and is not subject to any
     * limitations of, the implementation or the target printer.
     *
     * @return  {@code DialogType.COMMON}, {@code DialogType.NATIVE}, or
     *          {@code DialogType.NONE}
     */
    public DialogType getDialog() {
        return dialog;
    }

    /**
     * Specifies whether, for jobs using these attributes, the user should see
     * a print dialog in which to modify the print settings, and which type of
     * print dialog should be displayed. DialogType.COMMON denotes a cross-
     * platform, pure Java print dialog. DialogType.NATIVE denotes the
     * platform's native print dialog. If a platform does not support a native
     * print dialog, the pure Java print dialog is displayed instead.
     * DialogType.NONE specifies no print dialog (i.e., background printing).
     * Not specifying this attribute is equivalent to specifying
     * DialogType.NATIVE.
     *
     * @param   dialog DialogType.COMMON, DialogType.NATIVE, or
     *          DialogType.NONE.
     * @throws  IllegalArgumentException if dialog is null.
     */
    public void setDialog(DialogType dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "dialog");
        }
        this.dialog = dialog;
    }

    /**
     * Specifies the file name for the output file for jobs using these
     * attributes. This attribute is updated to the value chosen by the user.
     *
     * @return  the possibly {@code null} file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Specifies the file name for the output file for jobs using these
     * attributes. Default is platform-dependent and implementation-defined.
     *
     * @param   fileName the possibly null file name.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns, for jobs using these attributes, the first page to be
     * printed, if a range of pages is to be printed. This attribute is
     * updated to the value chosen by the user. An application should ignore
     * this attribute on output, unless the return value of the
     * {@code getDefaultSelection} method is DefaultSelectionType.RANGE. An
     * application should honor the return value of {@code getPageRanges}
     * over the return value of this method, if possible.
     *
     * @return  an integer greater than zero and less than or equal to
     *          <i>toPage</i> and greater than or equal to <i>minPage</i> and
     *          less than or equal to <i>maxPage</i>.
     */
    public int getFromPage() {
        if (fromPage != 0) {
            return fromPage;
        } else if (toPage != 0) {
            return getMinPage();
        } else if (pageRanges != null) {
            return prFirst;
        } else {
            return getMinPage();
        }
    }

    /**
     * Specifies, for jobs using these attributes, the first page to be
     * printed, if a range of pages is to be printed. If this attribute is not
     * specified, then the values from the pageRanges attribute are used. If
     * pageRanges and either or both of fromPage and toPage are specified,
     * pageRanges takes precedence. Specifying none of pageRanges, fromPage,
     * or toPage is equivalent to calling
     * setPageRanges(new int[][] { new int[] { <i>minPage</i> } });
     *
     * @param   fromPage an integer greater than zero and less than or equal to
     *          <i>toPage</i> and greater than or equal to <i>minPage</i> and
     *          less than or equal to <i>maxPage</i>.
     * @throws  IllegalArgumentException if one or more of the above
     *          conditions is violated.
     */
    public void setFromPage(int fromPage) {
        if (fromPage <= 0 ||
            (toPage != 0 && fromPage > toPage) ||
            fromPage < minPage ||
            fromPage > maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "fromPage");
        }
        this.fromPage = fromPage;
    }

    /**
     * Specifies the maximum value the user can specify as the last page to
     * be printed for jobs using these attributes. This attribute cannot be
     * modified by, and is not subject to any limitations of, the
     * implementation or the target printer.
     *
     * @return  an integer greater than zero and greater than or equal
     *          to <i>minPage</i>.
     */
    public int getMaxPage() {
        return maxPage;
    }

    /**
     * Specifies the maximum value the user can specify as the last page to
     * be printed for jobs using these attributes. Not specifying this
     * attribute is equivalent to specifying {@code Integer.MAX_VALUE}.
     *
     * @param   maxPage an integer greater than zero and greater than or equal
     *          to <i>minPage</i>
     * @throws  IllegalArgumentException if one or more of the above
     *          conditions is violated
     */
    public void setMaxPage(int maxPage) {
        if (maxPage <= 0 || maxPage < minPage) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "maxPage");
        }
        this.maxPage = maxPage;
    }

    /**
     * Specifies the minimum value the user can specify as the first page to
     * be printed for jobs using these attributes. This attribute cannot be
     * modified by, and is not subject to any limitations of, the
     * implementation or the target printer.
     *
     * @return  an integer greater than zero and less than or equal
     *          to <i>maxPage</i>.
     */
    public int getMinPage() {
        return minPage;
    }

    /**
     * Specifies the minimum value the user can specify as the first page to
     * be printed for jobs using these attributes. Not specifying this
     * attribute is equivalent to specifying {@code 1}.
     *
     * @param   minPage an integer greater than zero and less than or equal
     *          to <i>maxPage</i>.
     * @throws  IllegalArgumentException if one or more of the above
     *          conditions is violated.
     */
    public void setMinPage(int minPage) {
        if (minPage <= 0 || minPage > maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "minPage");
        }
        this.minPage = minPage;
    }

    /**
     * Specifies the handling of multiple copies, including collation, for
     * jobs using these attributes. This attribute is updated to the value
     * chosen by the user.
     *
     * @return
     *     MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_COLLATED_COPIES or
     *     MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES.
     */
    public MultipleDocumentHandlingType getMultipleDocumentHandling() {
        return multipleDocumentHandling;
    }

    /**
     * Specifies the handling of multiple copies, including collation, for
     * jobs using these attributes. Not specifying this attribute is equivalent
     * to specifying
     * MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES.
     *
     * @param   multipleDocumentHandling
     *     MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_COLLATED_COPIES or
     *     MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES.
     * @throws  IllegalArgumentException if multipleDocumentHandling is null.
     */
    public void setMultipleDocumentHandling(MultipleDocumentHandlingType
                                            multipleDocumentHandling) {
        if (multipleDocumentHandling == null) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "multipleDocumentHandling");
        }
        this.multipleDocumentHandling = multipleDocumentHandling;
    }

    /**
     * Sets the handling of multiple copies, including collation, for jobs
     * using these attributes to the default. The default handling is
     * MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES.
     */
    public void setMultipleDocumentHandlingToDefault() {
        setMultipleDocumentHandling(
            MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);
    }

    /**
     * Specifies, for jobs using these attributes, the ranges of pages to be
     * printed, if a range of pages is to be printed. All range numbers are
     * inclusive. This attribute is updated to the value chosen by the user.
     * An application should ignore this attribute on output, unless the
     * return value of the {@code getDefaultSelection} method is
     * DefaultSelectionType.RANGE.
     *
     * @return  an array of integer arrays of 2 elements. An array
     *          is interpreted as a range spanning all pages including and
     *          between the specified pages. Ranges must be in ascending
     *          order and must not overlap. Specified page numbers cannot be
     *          less than <i>minPage</i> nor greater than <i>maxPage</i>.
     *          For example:
     *          (new int[][] { new int[] { 1, 3 }, new int[] { 5, 5 },
     *                         new int[] { 15, 19 } }),
     *          specifies pages 1, 2, 3, 5, 15, 16, 17, 18, and 19.
     */
    public int[][] getPageRanges() {
        if (pageRanges != null) {
            // Return a copy because otherwise client code could circumvent the
            // the checks made in setPageRanges by modifying the returned
            // array.
            int[][] copy = new int[pageRanges.length][2];
            for (int i = 0; i < pageRanges.length; i++) {
                copy[i][0] = pageRanges[i][0];
                copy[i][1] = pageRanges[i][1];
            }
            return copy;
        } else if (fromPage != 0 || toPage != 0) {
            int fromPage = getFromPage();
            int toPage = getToPage();
            return new int[][] { new int[] { fromPage, toPage } };
        } else {
            int minPage = getMinPage();
            return new int[][] { new int[] { minPage, minPage } };
        }
    }

    /**
     * Specifies, for jobs using these attributes, the ranges of pages to be
     * printed, if a range of pages is to be printed. All range numbers are
     * inclusive. If this attribute is not specified, then the values from the
     * fromPage and toPages attributes are used. If pageRanges and either or
     * both of fromPage and toPage are specified, pageRanges takes precedence.
     * Specifying none of pageRanges, fromPage, or toPage is equivalent to
     * calling setPageRanges(new int[][] { new int[] { <i>minPage</i>,
     *                                                 <i>minPage</i> } });
     *
     * @param   pageRanges an array of integer arrays of 2 elements. An array
     *          is interpreted as a range spanning all pages including and
     *          between the specified pages. Ranges must be in ascending
     *          order and must not overlap. Specified page numbers cannot be
     *          less than <i>minPage</i> nor greater than <i>maxPage</i>.
     *          For example:
     *          (new int[][] { new int[] { 1, 3 }, new int[] { 5, 5 },
     *                         new int[] { 15, 19 } }),
     *          specifies pages 1, 2, 3, 5, 15, 16, 17, 18, and 19. Note that
     *          (new int[][] { new int[] { 1, 1 }, new int[] { 1, 2 } }),
     *          is an invalid set of page ranges because the two ranges
     *          overlap.
     * @throws  IllegalArgumentException if one or more of the above
     *          conditions is violated.
     */
    public void setPageRanges(int[][] pageRanges) {
        String xcp = "Invalid value for attribute pageRanges";
        int first = 0;
        int last = 0;

        if (pageRanges == null) {
            throw new IllegalArgumentException(xcp);
        }

        for (int i = 0; i < pageRanges.length; i++) {
            if (pageRanges[i] == null ||
                pageRanges[i].length != 2 ||
                pageRanges[i][0] <= last ||
                pageRanges[i][1] < pageRanges[i][0]) {
                    throw new IllegalArgumentException(xcp);
            }
            last = pageRanges[i][1];
            if (first == 0) {
                first = pageRanges[i][0];
            }
        }

        if (first < minPage || last > maxPage) {
            throw new IllegalArgumentException(xcp);
        }

        // Store a copy because otherwise client code could circumvent the
        // the checks made above by holding a reference to the array and
        // modifying it after calling setPageRanges.
        int[][] copy = new int[pageRanges.length][2];
        for (int i = 0; i < pageRanges.length; i++) {
            copy[i][0] = pageRanges[i][0];
            copy[i][1] = pageRanges[i][1];
        }
        this.pageRanges = copy;
        this.prFirst = first;
        this.prLast = last;
    }

    /**
     * Returns the destination printer for jobs using these attributes. This
     * attribute is updated to the value chosen by the user.
     *
     * @return  the possibly null printer name.
     */
    public String getPrinter() {
        return printer;
    }

    /**
     * Specifies the destination printer for jobs using these attributes.
     * Default is platform-dependent and implementation-defined.
     *
     * @param   printer the possibly null printer name.
     */
    public void setPrinter(String printer) {
        this.printer = printer;
    }

    /**
     * Returns how consecutive pages should be imposed upon the sides of the
     * print medium for jobs using these attributes. SidesType.ONE_SIDED
     * imposes each consecutive page upon the same side of consecutive media
     * sheets. This imposition is sometimes called <i>simplex</i>.
     * SidesType.TWO_SIDED_LONG_EDGE imposes each consecutive pair of pages
     * upon front and back sides of consecutive media sheets, such that the
     * orientation of each pair of pages on the medium would be correct for
     * the reader as if for binding on the long edge. This imposition is
     * sometimes called <i>duplex</i>. SidesType.TWO_SIDED_SHORT_EDGE imposes
     * each consecutive pair of pages upon front and back sides of consecutive
     * media sheets, such that the orientation of each pair of pages on the
     * medium would be correct for the reader as if for binding on the short
     * edge. This imposition is sometimes called <i>tumble</i>. This attribute
     * is updated to the value chosen by the user.
     *
     * @return  SidesType.ONE_SIDED, SidesType.TWO_SIDED_LONG_EDGE, or
     *          SidesType.TWO_SIDED_SHORT_EDGE.
     */
    public SidesType getSides() {
        return sides;
    }

    /**
     * Specifies how consecutive pages should be imposed upon the sides of the
     * print medium for jobs using these attributes. SidesType.ONE_SIDED
     * imposes each consecutive page upon the same side of consecutive media
     * sheets. This imposition is sometimes called <i>simplex</i>.
     * SidesType.TWO_SIDED_LONG_EDGE imposes each consecutive pair of pages
     * upon front and back sides of consecutive media sheets, such that the
     * orientation of each pair of pages on the medium would be correct for
     * the reader as if for binding on the long edge. This imposition is
     * sometimes called <i>duplex</i>. SidesType.TWO_SIDED_SHORT_EDGE imposes
     * each consecutive pair of pages upon front and back sides of consecutive
     * media sheets, such that the orientation of each pair of pages on the
     * medium would be correct for the reader as if for binding on the short
     * edge. This imposition is sometimes called <i>tumble</i>. Not specifying
     * this attribute is equivalent to specifying SidesType.ONE_SIDED.
     *
     * @param   sides SidesType.ONE_SIDED, SidesType.TWO_SIDED_LONG_EDGE, or
     *          SidesType.TWO_SIDED_SHORT_EDGE.
     * @throws  IllegalArgumentException if sides is null.
     */
    public void setSides(SidesType sides) {
        if (sides == null) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "sides");
        }
        this.sides = sides;
    }

    /**
     * Sets how consecutive pages should be imposed upon the sides of the
     * print medium for jobs using these attributes to the default. The
     * default imposition is SidesType.ONE_SIDED.
     */
    public void setSidesToDefault() {
        setSides(SidesType.ONE_SIDED);
    }

    /**
     * Returns, for jobs using these attributes, the last page (inclusive)
     * to be printed, if a range of pages is to be printed. This attribute is
     * updated to the value chosen by the user. An application should ignore
     * this attribute on output, unless the return value of the
     * {@code getDefaultSelection} method is DefaultSelectionType.RANGE. An
     * application should honor the return value of {@code getPageRanges}
     * over the return value of this method, if possible.
     *
     * @return  an integer greater than zero and greater than or equal
     *          to <i>toPage</i> and greater than or equal to <i>minPage</i>
     *          and less than or equal to <i>maxPage</i>.
     */
    public int getToPage() {
        if (toPage != 0) {
            return toPage;
        } else if (fromPage != 0) {
            return fromPage;
        } else if (pageRanges != null) {
            return prLast;
        } else {
            return getMinPage();
        }
    }

    /**
     * Specifies, for jobs using these attributes, the last page (inclusive)
     * to be printed, if a range of pages is to be printed.
     * If this attribute is not specified, then the values from the pageRanges
     * attribute are used. If pageRanges and either or both of fromPage and
     * toPage are specified, pageRanges takes precedence. Specifying none of
     * pageRanges, fromPage, or toPage is equivalent to calling
     * setPageRanges(new int[][] { new int[] { <i>minPage</i> } });
     *
     * @param   toPage an integer greater than zero and greater than or equal
     *          to <i>fromPage</i> and greater than or equal to <i>minPage</i>
     *          and less than or equal to <i>maxPage</i>.
     * @throws  IllegalArgumentException if one or more of the above
     *          conditions is violated.
     */
    public void setToPage(int toPage) {
        if (toPage <= 0 ||
            (fromPage != 0 && toPage < fromPage) ||
            toPage < minPage ||
            toPage > maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute "+
                                               "toPage");
        }
        this.toPage = toPage;
    }

    /**
     * Determines whether two JobAttributes are equal to each other.
     * <p>
     * Two JobAttributes are equal if and only if each of their attributes are
     * equal. Attributes of enumeration type are equal if and only if the
     * fields refer to the same unique enumeration object. A set of page
     * ranges is equal if and only if the sets are of equal length, each range
     * enumerates the same pages, and the ranges are in the same order.
     *
     * @param   obj the object whose equality will be checked.
     * @return  whether obj is equal to this JobAttribute according to the
     *          above criteria.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof JobAttributes)) {
            return false;
        }
        JobAttributes rhs = (JobAttributes)obj;

        if (fileName == null) {
            if (rhs.fileName != null) {
                return false;
            }
        } else {
            if (!fileName.equals(rhs.fileName)) {
                return false;
            }
        }

        if (pageRanges == null) {
            if (rhs.pageRanges != null) {
                return false;
            }
        } else {
            if (rhs.pageRanges == null ||
                    pageRanges.length != rhs.pageRanges.length) {
                return false;
            }
            for (int i = 0; i < pageRanges.length; i++) {
                if (pageRanges[i][0] != rhs.pageRanges[i][0] ||
                    pageRanges[i][1] != rhs.pageRanges[i][1]) {
                    return false;
                }
            }
        }

        if (printer == null) {
            if (rhs.printer != null) {
                return false;
            }
        } else {
            if (!printer.equals(rhs.printer)) {
                return false;
            }
        }

        return (copies == rhs.copies &&
                defaultSelection == rhs.defaultSelection &&
                destination == rhs.destination &&
                dialog == rhs.dialog &&
                fromPage == rhs.fromPage &&
                maxPage == rhs.maxPage &&
                minPage == rhs.minPage &&
                multipleDocumentHandling == rhs.multipleDocumentHandling &&
                prFirst == rhs.prFirst &&
                prLast == rhs.prLast &&
                sides == rhs.sides &&
                toPage == rhs.toPage);
    }

    /**
     * Returns a hash code value for this JobAttributes.
     *
     * @return  the hash code.
     */
    public int hashCode() {
        int rest = ((copies + fromPage + maxPage + minPage + prFirst + prLast +
                     toPage) * 31) << 21;
        if (pageRanges != null) {
            int sum = 0;
            for (int i = 0; i < pageRanges.length; i++) {
                sum += pageRanges[i][0] + pageRanges[i][1];
            }
            rest ^= (sum * 31) << 11;
        }
        if (fileName != null) {
            rest ^= fileName.hashCode();
        }
        if (printer != null) {
            rest ^= printer.hashCode();
        }
        return (defaultSelection.hashCode() << 6 ^
                destination.hashCode() << 5 ^
                dialog.hashCode() << 3 ^
                multipleDocumentHandling.hashCode() << 2 ^
                sides.hashCode() ^
                rest);
    }

    /**
     * Returns a string representation of this JobAttributes.
     *
     * @return  the string representation.
     */
    public String toString() {
        int[][] pageRanges = getPageRanges();
        String prStr = "[";
        boolean first = true;
        for (int i = 0; i < pageRanges.length; i++) {
            if (first) {
                first = false;
            } else {
                prStr += ",";
            }
            prStr += pageRanges[i][0] + ":" + pageRanges[i][1];
        }
        prStr += "]";

        return "copies=" + getCopies() + ",defaultSelection=" +
            getDefaultSelection() + ",destination=" + getDestination() +
            ",dialog=" + getDialog() + ",fileName=" + getFileName() +
            ",fromPage=" + getFromPage() + ",maxPage=" + getMaxPage() +
            ",minPage=" + getMinPage() + ",multiple-document-handling=" +
            getMultipleDocumentHandling() + ",page-ranges=" + prStr +
            ",printer=" + getPrinter() + ",sides=" + getSides() + ",toPage=" +
            getToPage();
    }
}
