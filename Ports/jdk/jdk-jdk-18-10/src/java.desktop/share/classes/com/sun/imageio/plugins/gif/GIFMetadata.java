/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.gif;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import org.w3c.dom.Node;

/**
 * Class which adds utility DOM element attribute access methods to
 * {@code IIOMetadata} for subclass use.
 */
abstract class GIFMetadata extends IIOMetadata {

    /**
     * Represents an undefined value of integer attributes.
     */
    static final int UNDEFINED_INTEGER_VALUE = -1;

    //
    // Note: These attribute methods were shamelessly lifted from
    // com.sun.imageio.plugins.png.PNGMetadata and modified.
    //

    // Shorthand for throwing an IIOInvalidTreeException
    protected static void fatal(Node node, String reason)
      throws IIOInvalidTreeException {
        throw new IIOInvalidTreeException(reason, node);
    }

    // Get an integer-valued attribute
    protected static String getStringAttribute(Node node, String name,
                                               String defaultValue,
                                               boolean required,
                                               String[] range)
      throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        String value = attr.getNodeValue();

        if (range != null) {
            if (value == null) {
                fatal(node,
                      "Null value for "+node.getNodeName()+
                      " attribute "+name+"!");
            }
            boolean validValue = false;
            int len = range.length;
            for (int i = 0; i < len; i++) {
                if (value.equals(range[i])) {
                    validValue = true;
                    break;
                }
            }
            if (!validValue) {
                fatal(node,
                      "Bad value for "+node.getNodeName()+
                      " attribute "+name+"!");
            }
        }

        return value;
    }


    // Get an integer-valued attribute
    protected static int getIntAttribute(Node node, String name,
                                         int defaultValue, boolean required,
                                         boolean bounded, int min, int max)
      throws IIOInvalidTreeException {
        String value = getStringAttribute(node, name, null, required, null);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        int intValue = defaultValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            fatal(node,
                  "Bad value for "+node.getNodeName()+
                  " attribute "+name+"!");
        }
        if (bounded && (intValue < min || intValue > max)) {
            fatal(node,
                  "Bad value for "+node.getNodeName()+
                  " attribute "+name+"!");
        }
        return intValue;
    }

    // Get a float-valued attribute
    protected static float getFloatAttribute(Node node, String name,
                                             float defaultValue,
                                             boolean required)
      throws IIOInvalidTreeException {
        String value = getStringAttribute(node, name, null, required, null);
        if (value == null) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    // Get a required integer-valued attribute
    protected static int getIntAttribute(Node node, String name,
                                         boolean bounded, int min, int max)
      throws IIOInvalidTreeException {
        return getIntAttribute(node, name, -1, true, bounded, min, max);
    }

    // Get a required float-valued attribute
    protected static float getFloatAttribute(Node node, String name)
      throws IIOInvalidTreeException {
        return getFloatAttribute(node, name, -1.0F, true);
    }

    // Get a boolean-valued attribute
    protected static boolean getBooleanAttribute(Node node, String name,
                                                 boolean defaultValue,
                                                 boolean required)
      throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        String value = attr.getNodeValue();
        // Allow lower case booleans for backward compatibility, #5082756
        if (value.equals("TRUE") || value.equals("true")) {
            return true;
        } else if (value.equals("FALSE") || value.equals("false")) {
            return false;
        } else {
            fatal(node, "Attribute " + name + " must be 'TRUE' or 'FALSE'!");
            return false;
        }
    }

    // Get a required boolean-valued attribute
    protected static boolean getBooleanAttribute(Node node, String name)
      throws IIOInvalidTreeException {
        return getBooleanAttribute(node, name, false, true);
    }

    // Get an enumerated attribute as an index into a String array
    protected static int getEnumeratedAttribute(Node node,
                                                String name,
                                                String[] legalNames,
                                                int defaultValue,
                                                boolean required)
      throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        String value = attr.getNodeValue();
        for (int i = 0; i < legalNames.length; i++) {
            if(value.equals(legalNames[i])) {
                return i;
            }
        }

        fatal(node, "Illegal value for attribute " + name + "!");
        return -1;
    }

    // Get a required enumerated attribute as an index into a String array
    protected static int getEnumeratedAttribute(Node node,
                                                String name,
                                                String[] legalNames)
      throws IIOInvalidTreeException {
        return getEnumeratedAttribute(node, name, legalNames, -1, true);
    }

    // Get a String-valued attribute
    protected static String getAttribute(Node node, String name,
                                         String defaultValue, boolean required)
      throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        return attr.getNodeValue();
    }

    // Get a required String-valued attribute
    protected static String getAttribute(Node node, String name)
      throws IIOInvalidTreeException {
        return getAttribute(node, name, null, true);
    }

    protected GIFMetadata(boolean standardMetadataFormatSupported,
                          String nativeMetadataFormatName,
                          String nativeMetadataFormatClassName,
                          String[] extraMetadataFormatNames,
                          String[] extraMetadataFormatClassNames) {
        super(standardMetadataFormatSupported,
              nativeMetadataFormatName,
              nativeMetadataFormatClassName,
              extraMetadataFormatNames,
              extraMetadataFormatClassNames);
    }

    public void mergeTree(String formatName, Node root)
      throws IIOInvalidTreeException {
        if (formatName.equals(nativeMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeNativeTree(root);
        } else if (formatName.equals
                  (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeStandardTree(root);
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    protected byte[] getColorTable(Node colorTableNode,
                                   String entryNodeName,
                                   boolean lengthExpected,
                                   int expectedLength)
      throws IIOInvalidTreeException {
        byte[] red = new byte[256];
        byte[] green  = new byte[256];
        byte[] blue = new byte[256];
        int maxIndex = -1;

        Node entry = colorTableNode.getFirstChild();
        if (entry == null) {
            fatal(colorTableNode, "Palette has no entries!");
        }

        while (entry != null) {
            if (!entry.getNodeName().equals(entryNodeName)) {
                fatal(colorTableNode,
                      "Only a "+entryNodeName+" may be a child of a "+
                      entry.getNodeName()+"!");
            }

            int index = getIntAttribute(entry, "index", true, 0, 255);
            if (index > maxIndex) {
                maxIndex = index;
            }
            red[index] = (byte)getIntAttribute(entry, "red", true, 0, 255);
            green[index] = (byte)getIntAttribute(entry, "green", true, 0, 255);
            blue[index] = (byte)getIntAttribute(entry, "blue", true, 0, 255);

            entry = entry.getNextSibling();
        }

        int numEntries = maxIndex + 1;

        if (lengthExpected && numEntries != expectedLength) {
            fatal(colorTableNode, "Unexpected length for palette!");
        }

        byte[] colorTable = new byte[3*numEntries];
        for (int i = 0, j = 0; i < numEntries; i++) {
            colorTable[j++] = red[i];
            colorTable[j++] = green[i];
            colorTable[j++] = blue[i];
        }

        return colorTable;
    }

    protected abstract void mergeNativeTree(Node root)
      throws IIOInvalidTreeException;

   protected abstract void mergeStandardTree(Node root)
      throws IIOInvalidTreeException;
}
