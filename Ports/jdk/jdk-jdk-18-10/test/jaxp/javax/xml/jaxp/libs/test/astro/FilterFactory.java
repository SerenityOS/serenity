/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.astro;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

/*
 * Defines the interface for all concrete implementations of a Filter
 * Factory.
 */
public interface FilterFactory {
    /*
     * Allows only the stars between right ascension (R.A.) of min and max to
     * pass. Filters out all stars that are not in that range of right
     * ascension. Units of right ascension are hours (h), range of parameters is
     * 0h to 24h.
     *
     * @param min minimum R.A.
     *
     * @param max maxmimum R.A.
     */
    TransformerHandler newRAFilter(double min, double max) throws TransformerConfigurationException, SAXException, ParserConfigurationException, IOException;

    /*
     * Allows only the stars between declination (DEC) of min and max to pass.
     * Filters out all stars that are not in that range of declination. Units of
     * declination are degrees (degs), range of parameters is -90 and +90 degs.
     *
     * @param min minimum DEC
     *
     * @param max maxmimum DEC
     */
    TransformerHandler newDECFilter(double min, double max) throws TransformerConfigurationException, SAXException, ParserConfigurationException, IOException;

    /*
     * Combines the usage of newRAFilter and newDECFilter into one.
     *
     * @param rmin minimum R.A.
     *
     * @param rmax maxmimum R.A.
     *
     * @param dmin minimum DEC
     *
     * @param dmax maxmimum DEC
     */
    TransformerHandler newRADECFilter(double rmin, double rmax, double dmin, double dmax) throws TransformerConfigurationException, SAXException,
            ParserConfigurationException, IOException;

    TransformerHandler newStellarTypeFilter(String type) throws TransformerConfigurationException, SAXException, ParserConfigurationException, IOException;

    TransformerHandler newHTMLOutput() throws TransformerConfigurationException, SAXException, ParserConfigurationException, IOException;
}
