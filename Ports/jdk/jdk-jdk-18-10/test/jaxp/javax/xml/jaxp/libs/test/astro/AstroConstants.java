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

import static java.io.File.separator;
import static jaxp.library.JAXPTestUtilities.getPathByClassName;

public class AstroConstants {
    // Query parameters :

    public static final double RA_MIN = 0.0; // hours
    public static final double RA_MAX = 24.0; // hours
    public static final double DEC_MIN = -90.000; // degrees
    public static final double DEC_MAX = 90.000; // degrees

    // Stylesheet source paths:

    public static final String XSLPATH = getPathByClassName(AstroConstants.class, "xmlfiles" + separator + "xsl");
    public static final String RAXSL = XSLPATH + "ra.xsl";
    public static final String DECXSL = XSLPATH + "dec.xsl";
    public static final String RADECXSL = XSLPATH + "radec.xsl";
    public static final String STYPEXSL = XSLPATH + "stellartype.xsl";
    public static final String HTMLXSL = XSLPATH + "html.xsl";

    public static final String RAENTXSL = XSLPATH + "ra-ent.xsl";
    public static final String DECENTXSL = XSLPATH + "dec-ent.xsl";
    public static final String RAURIXSL = XSLPATH + "ra-uri.xsl";
    public static final String TOPTEMPLXSL = XSLPATH + "toptemplate.xsl";
    public static final String TOPTEMPLINCXSL = XSLPATH + "toptemplateinc.xsl";

    // Catalog references

    public static final String ASTROCAT = getPathByClassName(AstroConstants.class, "xmlfiles") + "catalog.xml";


    public static final String GOLDEN_DIR = getPathByClassName(AstroConstants.class, "xmlfiles" + separator + "gold");
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
}
