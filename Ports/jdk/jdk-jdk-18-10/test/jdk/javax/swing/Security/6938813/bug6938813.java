/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key headful
 * @bug 6938813
 * @summary Swing mutable statics
 * @author Pavel Porvatov
 * @modules java.desktop/javax.swing.text.html.parser:open
 * @modules java.desktop/sun.awt
 */

import sun.awt.AppContext;
import sun.awt.SunToolkit;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.ParserDelegator;
import java.lang.reflect.Field;

public class bug6938813 {
    public static final String DTD_KEY = "dtd_key";

    private static volatile StyleSheet styleSheet;

    public static void main(String[] args) throws Exception {
        // Run validation and init values for this AppContext
        validate();

        Thread thread = new ThreadInAnotherAppContext();

        thread.start();
        thread.join();
    }

    private static void validate() throws Exception {
        AppContext appContext = AppContext.getAppContext();

        assertTrue(DTD.getDTD(DTD_KEY).getName().equals(DTD_KEY), "DTD.getDTD() mixed AppContexts");

        // Spoil hash value
        DTD invalidDtd = DTD.getDTD("invalid DTD");

        DTD.putDTDHash(DTD_KEY, invalidDtd);

        assertTrue(DTD.getDTD(DTD_KEY) == invalidDtd, "Something wrong with DTD.getDTD()");

        Object dtdKey = getParserDelegator_DTD_KEY();

        assertTrue(appContext.get(dtdKey) == null, "ParserDelegator mixed AppContexts");

        // Init default DTD
        new ParserDelegator();

        Object dtdValue = appContext.get(dtdKey);

        assertTrue(dtdValue != null, "ParserDelegator.defaultDTD isn't initialized");

        // Try reinit default DTD
        new ParserDelegator();

        assertTrue(dtdValue == appContext.get(dtdKey), "ParserDelegator.defaultDTD created a duplicate");

        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();

        if (styleSheet == null) {
            // First AppContext
            styleSheet = htmlEditorKit.getStyleSheet();

            assertTrue(styleSheet != null, "htmlEditorKit.getStyleSheet() returns null");
            assertTrue(htmlEditorKit.getStyleSheet() == styleSheet, "Something wrong with htmlEditorKit.getStyleSheet()");
        } else {
            assertTrue(htmlEditorKit.getStyleSheet() != styleSheet, "HtmlEditorKit.getStyleSheet() mixed AppContexts");
        }
    }

    private static void assertTrue(boolean b, String msg) {
        if (!b) {
            throw new RuntimeException("Test failed: " + msg);
        }
    }

    private static Object getParserDelegator_DTD_KEY() throws Exception {
        Field field = ParserDelegator.class.getDeclaredField("DTD_KEY");

        field.setAccessible(true);

        return field.get(null);
    }

    private static class ThreadInAnotherAppContext extends Thread {
        public ThreadInAnotherAppContext() {
            super(new ThreadGroup("6938813"), "ThreadInAnotherAppContext");
        }

        public void run() {
            SunToolkit.createNewAppContext();

            try {
                validate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
