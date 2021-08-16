/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.loading;


// java import

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class represents the contents of the <CODE>MLET</CODE> tag.
 * It can be consulted by a subclass of {@link MLet} that overrides
 * the {@link MLet#check MLet.check} method.
 *
 * @since 1.6
 */
public class MLetContent {


    /**
     * A map of the attributes of the <CODE>MLET</CODE> tag
     * and their values.
     */
    private Map<String,String> attributes;

    /**
     * An ordered list of the TYPE attributes that appeared in nested
     * &lt;PARAM&gt; tags.
     */
    private List<String> types;

    /**
     * An ordered list of the VALUE attributes that appeared in nested
     * &lt;PARAM&gt; tags.
     */
    private List<String> values;

    /**
     * The MLet text file's base URL.
     */
    private URL documentURL;
    /**
     * The base URL.
     */
    private URL baseURL;


    /**
     * Creates an <CODE>MLet</CODE> instance initialized with attributes read
     * from an <CODE>MLET</CODE> tag in an MLet text file.
     *
     * @param url The URL of the MLet text file containing the
     * <CODE>MLET</CODE> tag.
     * @param attributes A map of the attributes of the <CODE>MLET</CODE> tag.
     * The keys in this map are the attribute names in lowercase, for
     * example <code>codebase</code>.  The values are the associated attribute
     * values.
     * @param types A list of the TYPE attributes that appeared in nested
     * &lt;PARAM&gt; tags.
     * @param values A list of the VALUE attributes that appeared in nested
     * &lt;PARAM&gt; tags.
     */
    public MLetContent(URL url, Map<String,String> attributes,
                       List<String> types, List<String> values) {
        this.documentURL = url;
        this.attributes = Collections.unmodifiableMap(attributes);
        this.types = Collections.unmodifiableList(types);
        this.values = Collections.unmodifiableList(values);

        // Initialize baseURL
        //
        String att = getParameter("codebase");
        if (att != null) {
            if (!att.endsWith("/")) {
                att += "/";
            }
            try {
                baseURL = new URL(documentURL, att);
            } catch (MalformedURLException e) {
                // OK : Move to next block as baseURL could not be initialized.
            }
        }
        if (baseURL == null) {
            String file = documentURL.getFile();
            int i = file.lastIndexOf('/');
            if (i >= 0 && i < file.length() - 1) {
                try {
                    baseURL = new URL(documentURL, file.substring(0, i + 1));
                } catch (MalformedURLException e) {
                    // OK : Move to next block as baseURL could not be initialized.
                }
            }
        }
        if (baseURL == null)
            baseURL = documentURL;

    }

    // GETTERS AND SETTERS
    //--------------------

    /**
     * Gets the attributes of the <CODE>MLET</CODE> tag.  The keys in
     * the returned map are the attribute names in lowercase, for
     * example <code>codebase</code>.  The values are the associated
     * attribute values.
     * @return A map of the attributes of the <CODE>MLET</CODE> tag
     * and their values.
     */
    public Map<String,String> getAttributes() {
        return attributes;
    }

    /**
     * Gets the MLet text file's base URL.
     * @return The MLet text file's base URL.
     */
    public URL getDocumentBase() {
        return documentURL;
    }

    /**
     * Gets the code base URL.
     * @return The code base URL.
     */
    public URL getCodeBase() {
        return baseURL;
    }

    /**
     * Gets the list of <CODE>.jar</CODE> files specified by the <CODE>ARCHIVE</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     * @return A comma-separated list of <CODE>.jar</CODE> file names.
     */
    public String getJarFiles() {
        return getParameter("archive");
    }

    /**
     * Gets the value of the <CODE>CODE</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     * @return The value of the <CODE>CODE</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     */
    public String getCode() {
        return getParameter("code");
    }

    /**
     * Gets the value of the <CODE>OBJECT</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     * @return The value of the <CODE>OBJECT</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     */
    public String getSerializedObject() {
        return getParameter("object");
    }

    /**
     * Gets the value of the <CODE>NAME</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     * @return The value of the <CODE>NAME</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     */
    public String getName() {
        return getParameter("name");
    }


    /**
     * Gets the value of the <CODE>VERSION</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     * @return The value of the <CODE>VERSION</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     */
    public String getVersion() {
        return getParameter("version");
    }

    /**
     * Gets the list of values of the <code>TYPE</code> attribute in
     * each nested &lt;PARAM&gt; tag within the <code>MLET</code>
     * tag.
     * @return the list of types.
     */
    public List<String> getParameterTypes() {
        return types;
    }

    /**
     * Gets the list of values of the <code>VALUE</code> attribute in
     * each nested &lt;PARAM&gt; tag within the <code>MLET</code>
     * tag.
     * @return the list of values.
     */
    public List<String> getParameterValues() {
        return values;
    }

    /**
     * Gets the value of the specified
     * attribute of the <CODE>MLET</CODE> tag.
     *
     * @param name A string representing the name of the attribute.
     * @return The value of the specified
     * attribute of the <CODE>MLET</CODE> tag.
     */
    private String getParameter(String name) {
        return attributes.get(name.toLowerCase());
    }

}
