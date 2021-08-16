/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.rowset;

import java.io.*;
import java.util.*;

/**
 * This class is used to help in localization of resources,
 * especially the exception strings.
 *
 * @author Amit Handa
 */

public class JdbcRowSetResourceBundle implements Serializable {

    /**
     * This <code>String</code> variable stores the location
     * of the resource bundle location.
     */
    private static String fileName;

    /**
     * This variable will hold the <code>PropertyResourceBundle</code>
     * of the text to be internationalized.
     */
    private transient PropertyResourceBundle propResBundle;

    /**
     * The constructor initializes to this object
     *
     */
    private static volatile JdbcRowSetResourceBundle jpResBundle;

    /**
     * The variable which will represent the properties
     * the suffix or extension of the resource bundle.
     **/
    private static final String PROPERTIES = "properties";

    /**
     * The variable to represent underscore
     **/
    private static final String UNDERSCORE = "_";

    /**
     * The variable which will represent dot
     **/
    private static final String DOT = ".";

    /**
     * The variable which will represent the slash.
     **/
    private static final String SLASH = "/";

    /**
     * The variable where the default resource bundle will
     * be placed.
     **/
    private static final String PATH = "com/sun/rowset/RowSetResourceBundle";

    /**
     * The constructor which initializes the resource bundle.
     * Note this is a private constructor and follows Singleton
     * Design Pattern.
     *
     * @throws IOException if unable to load the ResourceBundle
     * according to locale or the default one.
     */
    private JdbcRowSetResourceBundle () throws IOException {
        // Try to load the resource bundle according
        // to the locale. Else if no bundle found according
        // to the locale load the default.

        // In default case the default locale resource bundle
        // should always be loaded else it
        // will be difficult to throw appropriate
        // exception string messages.
        Locale locale = Locale.getDefault();

        // Load appropriate bundle according to locale
        propResBundle = (PropertyResourceBundle) ResourceBundle.getBundle(PATH,
                           locale, JdbcRowSetResourceBundle.class.getModule());

   }

    /**
     * This method is used to get a handle to the
     * initialized instance of this class. Note that
     * at any time there is only one instance of this
     * class initialized which will be returned.
     *
     * @throws IOException if unable to find the RowSetResourceBundle.properties
     */
    public static JdbcRowSetResourceBundle getJdbcRowSetResourceBundle()
    throws IOException {

         if(jpResBundle == null){
             synchronized(JdbcRowSetResourceBundle.class) {
                if(jpResBundle == null){
                    jpResBundle = new JdbcRowSetResourceBundle();
                } //end if
             } //end synchronized block
         } //end if
         return jpResBundle;
    }

    /**
     * This method returns an enumerated handle of the keys
     * which correspond to values translated to various locales.
     *
     * @return an enumeration of keys which have messages tranlated to
     * corresponding locales.
     */
    @SuppressWarnings("rawtypes")
    public Enumeration getKeys() {
       return propResBundle.getKeys();
    }


    /**
     * This method takes the key as an argument and
     * returns the corresponding value reading it
     * from the Resource Bundle loaded earlier.
     *
     * @return value in locale specific language
     * according to the key passed.
     */
    public Object handleGetObject(String key) {
       return propResBundle.handleGetObject(key);
    }

    static final long serialVersionUID = 436199386225359954L;
}
