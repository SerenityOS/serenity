/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5102289 4403721
 * @summary Test XML support as shown in the ResourceBundle.Control description.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import static java.util.ResourceBundle.Control.*;

public class XMLResourceBundleTest {
    public static void main(String[] args) {
        ResourceBundle.Control xmlControl = new ResourceBundle.Control() {
                   @Override
                   public List<String> getFormats(String baseName) {
                       if (baseName == null) {
                           throw new NullPointerException();
                       }
                       return Arrays.asList("xml");
                   }
                   @Override
                   public ResourceBundle newBundle(String baseName, Locale locale,
                                                   String format,
                                                   ClassLoader loader,
                                                   boolean reload)
                       throws IllegalAccessException,
                              InstantiationException, IOException {
                       if (baseName == null || locale == null
                           || format == null || loader == null) {
                           throw new NullPointerException();
                       }
                       ResourceBundle bundle = null;
                       if (format.equals("xml")) {
                           String bundleName = toBundleName(baseName, locale);
                           String resourceName = toResourceName(bundleName, format);
                           URL url = loader.getResource(resourceName);
                           if (url != null) {
                               URLConnection connection = url.openConnection();
                               if (connection != null) {
                                   if (reload) {
                                       // disable caches if reloading
                                       connection.setUseCaches(false);
                                   }
                                   InputStream stream = connection.getInputStream();
                                   if (stream != null) {
                                       BufferedInputStream bis = new BufferedInputStream(stream);
                                       bundle = new XMLResourceBundle(bis);
                                       bis.close();
                                   }
                               }
                           }
                       }
                       return bundle;
                   }
               };
        ResourceBundle rb = ResourceBundle.getBundle("XmlRB", new Locale(""), xmlControl);
        String type = rb.getString("type");
        if (!type.equals("XML")) {
            throw new RuntimeException("Root Locale: type: got " + type
                                       + ", expected XML (ASCII)");
        }

        rb = ResourceBundle.getBundle("XmlRB", Locale.JAPAN, xmlControl);
        type = rb.getString("type");
        // Expect fullwidth "XML"
        if (!type.equals("\uff38\uff2d\uff2c")) {
            throw new RuntimeException("Locale.JAPAN: type: got " + type
                                       + ", expected \uff38\uff2d\uff2c (fullwidth XML)");
        }
    }

    private static class XMLResourceBundle extends ResourceBundle {
        private Properties props;

        XMLResourceBundle(InputStream stream) throws IOException {
            props = new Properties();
            props.loadFromXML(stream);
        }

        protected Object handleGetObject(String key) {
            if (key == null) {
                throw new NullPointerException();
            }
            return props.get(key);
        }

        public Enumeration<String> getKeys() {
            // Not implemented
            return null;
        }
    }
}
