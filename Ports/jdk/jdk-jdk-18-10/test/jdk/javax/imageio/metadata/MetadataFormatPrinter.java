/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

//

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import com.sun.imageio.plugins.png.PNGMetadata;

public class MetadataFormatPrinter {

    private int indentLevel = 0;

    private int column = 0;

    private PrintStream out;

    private static final int maxColumn = 75;

    private static String[] dataTypeNames = {
        "String", "Boolean", "Integer", "Float", "Double"
    };

    // "Infinite" values
    private static String maxInteger = Integer.toString(Integer.MAX_VALUE);

    public MetadataFormatPrinter(PrintStream out) {
        this.out = out;
    }

    private void println() {
        out.println();
        column = 0;
    }

    private void println(String s) {
        out.println(s);
        column = 0;
    }

    private void printWrapped(String in, int leftIndent) {
        StringTokenizer t = new StringTokenizer(in);
        while (t.hasMoreTokens()) {
            String s = t.nextToken();
            int length = s.length();
            if (column + length > maxColumn) {
                println();
                indent();
                for (int i = 0; i < leftIndent; i++) {
                    print(" ");
                }
            }
            out.print(s);
            out.print(" ");
            column += length + 1;
        }
    }

    private void print(String s) {
        int length = s.length();
        if (column + length > maxColumn) {
            println();
            indent();
            print("  ");
        }
        out.print(s);
        column += length;
    }

    private void print(IIOMetadataFormat format) {
        String rootName = format.getRootName();
        println("<!DOCTYPE \"" +
                           rootName +
                           "\" [");
        ++indentLevel;
        print(format, rootName);
        --indentLevel;
        print("]>");
        println();
        println();
    }

    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            out.print("  ");
            column += 2;
        }
    }

    private void printElementInfo(IIOMetadataFormat format,
                                  String elementName) {
        println();
        indent();
        print("<!ELEMENT \"" +
              elementName +
              "\"");

        String[] childNames = format.getChildNames(elementName);
        boolean hasChildren = true;
        String separator = " "; // symbol to place between children
        String terminator = ""; // symbol to follow last child
        String repeater = ""; // "*" if repeating

        switch (format.getChildPolicy(elementName)) {
        case IIOMetadataFormat.CHILD_POLICY_EMPTY:
            hasChildren = false;
            break;
        case IIOMetadataFormat.CHILD_POLICY_ALL:
            separator = ", ";
            break;
        case IIOMetadataFormat.CHILD_POLICY_SOME:
            separator = "?, ";
            terminator = "?";
            break;
        case IIOMetadataFormat.CHILD_POLICY_CHOICE:
            separator = " | ";
            break;
        case IIOMetadataFormat.CHILD_POLICY_SEQUENCE:
            separator = " | ";
            repeater = "*";
            break;
        case IIOMetadataFormat.CHILD_POLICY_REPEAT:
            repeater = "*";
            break;
        default:
            break;
        }

        if (hasChildren) {
            print(" (");
            for (int i = 0; i < childNames.length - 1; i++) {
                print(childNames[i] + separator);
            }
            print(childNames[childNames.length - 1] + terminator);
            print(")" + repeater + ">");
        } else {
            print(" EMPTY>");
        }
        println();

        String description = format.getElementDescription(elementName, null);
        if (description != null) {
            ++indentLevel;
            indent();
            printWrapped("<!-- " + description + " -->", 5);
            println();
            --indentLevel;
        }
        if (format.getChildPolicy(elementName) ==
            IIOMetadataFormat.CHILD_POLICY_REPEAT) {
            int minChildren = format.getElementMinChildren(elementName);
            if (minChildren != 0) {
                indent();
                println("  <!-- Min children: " +
                        minChildren +
                        " -->");
            }
            int maxChildren = format.getElementMaxChildren(elementName);
            if (maxChildren != Integer.MAX_VALUE) {
                indent();
                println("  <!-- Max children: " +
                        maxChildren +
                        " -->");
            }
        }
    }

    private void printAttributeInfo(IIOMetadataFormat format,
                                    String elementName,
                                    String attrName) {
        indent();
        print("<!ATTLIST \"" +
              elementName +
              "\" \"" +
              attrName +
              "\"");

        int attrValueType =
            format.getAttributeValueType(elementName, attrName);
        switch (attrValueType) {
        case IIOMetadataFormat.VALUE_NONE:
            throw new RuntimeException
                ("Encountered VALUE_NONE for an attribute!");
            // break;
        case IIOMetadataFormat.VALUE_ARBITRARY:
            print(" #CDATA");
            break;
        case IIOMetadataFormat.VALUE_RANGE:
        case IIOMetadataFormat.VALUE_RANGE_MIN_INCLUSIVE:
        case IIOMetadataFormat.VALUE_RANGE_MAX_INCLUSIVE:
        case IIOMetadataFormat.VALUE_RANGE_MIN_MAX_INCLUSIVE:
            print(" #CDATA");
            break;
        case IIOMetadataFormat.VALUE_ENUMERATION:
            print(" (");
            String[] attrValues =
                format.getAttributeEnumerations(elementName, attrName);
            for (int j = 0; j < attrValues.length - 1; j++) {
                print("\"" + attrValues[j] + "\" | ");
            }
            print("\"" + attrValues[attrValues.length - 1] + "\")");
            break;
        case IIOMetadataFormat.VALUE_LIST:
            print(" #CDATA");
            break;
        default:
            throw new RuntimeException
                ("Encountered unknown value type for an attribute!");
            // break;
        }

        String defaultValue =
            format.getAttributeDefaultValue(elementName, attrName);
        if (defaultValue != null) {
            print(" ");
            print("\"" + defaultValue + "\"");
        } else {
            if (format.isAttributeRequired(elementName, attrName)) {
                print(" #REQUIRED");
            } else {
                print(" #IMPLIED");
            }
        }
        println(">");

        String description = format.getAttributeDescription(elementName,
                                                            attrName,
                                                            null);
        if (description != null) {
            ++indentLevel;
            indent();
            printWrapped("<!-- " + description + " -->", 5);
            println();
            --indentLevel;
        }

        int dataType = format.getAttributeDataType(elementName, attrName);

        switch (attrValueType) {
        case IIOMetadataFormat.VALUE_ARBITRARY:
            indent();
            println("  <!-- Data type: " + dataTypeNames[dataType] + " -->");
            break;

        case IIOMetadataFormat.VALUE_RANGE:
        case IIOMetadataFormat.VALUE_RANGE_MIN_INCLUSIVE:
        case IIOMetadataFormat.VALUE_RANGE_MAX_INCLUSIVE:
        case IIOMetadataFormat.VALUE_RANGE_MIN_MAX_INCLUSIVE:
            indent();
            println("  <!-- Data type: " + dataTypeNames[dataType] + " -->");

            boolean minInclusive =
                (attrValueType &
                 IIOMetadataFormat.VALUE_RANGE_MIN_INCLUSIVE_MASK) != 0;
            boolean maxInclusive =
                (attrValueType &
                 IIOMetadataFormat.VALUE_RANGE_MAX_INCLUSIVE_MASK) != 0;
            indent();
            println("  <!-- Min value: " +
                    format.getAttributeMinValue(elementName, attrName) +
                    " " +
                    (minInclusive ? "(inclusive)" : "(exclusive)") +
                    " -->");
            String maxValue =
                format.getAttributeMaxValue(elementName, attrName);
            // Hack: don't print "infinite" max values
            if (dataType != IIOMetadataFormat.DATATYPE_INTEGER ||
                !maxValue.equals(maxInteger)) {
                indent();
                println("  <!-- Max value: " +
                        maxValue +
                        " " +
                        (maxInclusive ? "(inclusive)" : "(exclusive)") +
                        " -->");
            }
            break;

        case IIOMetadataFormat.VALUE_LIST:
            indent();
            println("  <!-- Data type: List of " + dataTypeNames[dataType] + " -->");

            int minLength =
                format.getAttributeListMinLength(elementName, attrName);
            if (minLength != 0) {
                indent();
                println("  <!-- Min length: " +
                        minLength +
                        " -->");
            }
            int maxLength =
                format.getAttributeListMaxLength(elementName, attrName);
            if (maxLength != Integer.MAX_VALUE) {
                indent();
                println("  <!-- Max length: " +
                        maxLength +
                        " -->");
            }
            break;
        }
    }

    private void printObjectInfo(IIOMetadataFormat format,
                                 String elementName) {
        int objectType = format.getObjectValueType(elementName);
        if (objectType == IIOMetadataFormat.VALUE_NONE) {
            return;
        }

        Class objectClass = format.getObjectClass(elementName);
        if (objectClass != null) {
            indent();
            if (objectType == IIOMetadataFormat.VALUE_LIST) {
                println("  <!-- User object: array of " +
                        objectClass.getName() +
                        " -->");
            } else {
                println("  <!-- User object: " +
                        objectClass.getName() +
                        " -->");
            }

            Object defaultValue = format.getObjectDefaultValue(elementName);
            if (defaultValue != null) {
                indent();
                println("  <!-- Default value: " +
                        defaultValue.toString() +
                        " -->");
            }

            switch (objectType) {
            case IIOMetadataFormat.VALUE_RANGE:
                indent();
                println("  <!-- Min value: " +
                        format.getObjectMinValue(elementName).toString() +
                        " -->");
                indent();
                println("  <!-- Max value: " +
                        format.getObjectMaxValue(elementName).toString() +
                        " -->");
                break;

            case IIOMetadataFormat.VALUE_ENUMERATION:
                Object[] enums = format.getObjectEnumerations(elementName);
                for (int i = 0; i < enums.length; i++) {
                    indent();
                    println("  <!-- Enumerated value: " +
                            enums[i].toString() +
                            " -->");
                }
                break;

            case IIOMetadataFormat.VALUE_LIST:
                int minLength = format.getObjectArrayMinLength(elementName);
                if (minLength != 0) {
                    indent();
                    println("  <!-- Min length: " +
                            minLength +
                            " -->");
                }
                int maxLength = format.getObjectArrayMaxLength(elementName);
                if (maxLength != Integer.MAX_VALUE) {
                    indent();
                    println("  <!-- Max length: " +
                            maxLength +
                            " -->");
                }
                break;
            }
        }
    }

    // Set of elements that have been printed already
    Set printedElements = new HashSet();

    // Set of elements that have been scheduled to be printed
    Set scheduledElements = new HashSet();

    private void print(IIOMetadataFormat format,
                       String elementName) {
        // Don't print elements more than once
        if (printedElements.contains(elementName)) {
            return;
        }
        printedElements.add(elementName);

        // Add the unscheduled children of this node to a list,
        // and mark them as scheduled
        List children = new ArrayList();
        String[] childNames = format.getChildNames(elementName);
        if (childNames != null) {
            for (int i = 0; i < childNames.length; i++) {
                String childName = childNames[i];
                if (!scheduledElements.contains(childName)) {
                    children.add(childName);
                    scheduledElements.add(childName);
                }
            }
        }

        printElementInfo(format, elementName);
        printObjectInfo(format, elementName);

        ++indentLevel;
        String[] attrNames = format.getAttributeNames(elementName);
        for (int i = 0; i < attrNames.length; i++) {
            printAttributeInfo(format, elementName, attrNames[i]);
        }

        // Recurse on child nodes
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
            print(format, (String)iter.next());
        }
        --indentLevel;
    }

    public static void main(String[] args) {
        IIOMetadataFormat format = null;
        if (args.length == 0 || args[0].equals("javax_imageio_1.0")) {
            format = IIOMetadataFormatImpl.getStandardFormatInstance();
        } else {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            Iterator iter = registry.getServiceProviders(ImageReaderSpi.class,
                                                         false);
            while (iter.hasNext()) {
                ImageReaderSpi spi = (ImageReaderSpi)iter.next();
                if (args[0].equals
                    (spi.getNativeStreamMetadataFormatName())) {
                    System.out.print(spi.getDescription(null));
                    System.out.println(": native stream format");
                    format = spi.getStreamMetadataFormat(args[0]);
                    break;
                }

                String[] extraStreamFormatNames =
                    spi.getExtraStreamMetadataFormatNames();
                if (extraStreamFormatNames != null &&
                    Arrays.asList(extraStreamFormatNames).
                    contains(args[0])) {
                    System.out.print(spi.getDescription(null));
                    System.out.println(": extra stream format");
                    format = spi.getStreamMetadataFormat(args[0]);
                    break;
                }

                if (args[0].equals
                    (spi.getNativeImageMetadataFormatName())) {
                    System.out.print(spi.getDescription(null));
                    System.out.println(": native image format");
                    format = spi.getImageMetadataFormat(args[0]);
                    break;
                }

                String[] extraImageFormatNames =
                    spi.getExtraImageMetadataFormatNames();
                if (extraImageFormatNames != null &&
                    Arrays.asList(extraImageFormatNames).contains(args[0])) {
                    System.out.print(spi.getDescription(null));
                    System.out.println(": extra image format");
                    format = spi.getImageMetadataFormat(args[0]);
                    break;
                }
            }
        }

        if (format == null) {
            System.err.println("Unknown format: " + args[0]);
            System.exit(0);
        }

        MetadataFormatPrinter printer = new MetadataFormatPrinter(System.out);
        printer.print(format);
    }
}
