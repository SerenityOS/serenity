/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html.parser;

import sun.awt.AppContext;

import javax.swing.text.html.HTMLEditorKit;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Responsible for starting up a new DocumentParser
 * each time its parse method is invoked. Stores a
 * reference to the dtd.
 *
 * @author  Sunita Mani
 */
@SuppressWarnings("serial") // Same-version serialization only
public class ParserDelegator extends HTMLEditorKit.Parser implements Serializable {
    private static final Object DTD_KEY = new Object();

    /**
     * Sets the default DTD.
     */
    protected static void setDefaultDTD() {
        getDefaultDTD();
    }

    private static synchronized DTD getDefaultDTD() {
        AppContext appContext = AppContext.getAppContext();

        DTD dtd = (DTD) appContext.get(DTD_KEY);

        if (dtd == null) {
            DTD _dtd = null;
            // (PENDING) Hate having to hard code!
            String nm = "html32";
            try {
                _dtd = DTD.getDTD(nm);
            } catch (IOException e) {
                // (PENDING) UGLY!
                System.out.println("Throw an exception: could not get default dtd: " + nm);
            }
            dtd = createDTD(_dtd, nm);

            appContext.put(DTD_KEY, dtd);
        }

        return dtd;
    }

    /**
     * Recreates a DTD from an archived format with the specified {@code name}.
     *
     * @param dtd a DTD
     * @param name the name of the resource, relative to the  ParserDelegator class.
     * @return the DTD with the specified {@code name}.
     */
    protected static DTD createDTD(DTD dtd, String name) {

        InputStream in = null;
        boolean debug = true;
        try {
            String path = name + ".bdtd";
            in = getResourceAsStream(path);
            if (in != null) {
                dtd.read(new DataInputStream(new BufferedInputStream(in)));
                DTD.putDTDHash(name, dtd);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return dtd;
    }

    /**
     * Creates {@code ParserDelegator} with default DTD.
     */
    public ParserDelegator() {
        setDefaultDTD();
    }

    public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet) throws IOException {
        new DocumentParser(getDefaultDTD()).parse(r, cb, ignoreCharSet);
    }

    /**
     * Fetch a resource relative to the ParserDelegator classfile.
     * If this is called on 1.2 the loading will occur under the
     * protection of a doPrivileged call to allow the ParserDelegator
     * to function when used in an applet.
     *
     * @param name the name of the resource, relative to the
     *  ParserDelegator class.
     * @return a stream representing the resource
     */
    @SuppressWarnings("removal")
    static InputStream getResourceAsStream(final String name) {
        return AccessController.doPrivileged(
                new PrivilegedAction<InputStream>() {
                    public InputStream run() {
                        return ParserDelegator.class.getResourceAsStream(name);
                    }
                });
    }

    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        setDefaultDTD();
    }
}
