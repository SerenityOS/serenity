/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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


/**
 * Plain text file handler
 *
 * This class provides an example of a a replacement content handler for
 * the text/plain content type.  It reads the content of the URL, and prepends
 * an additional message at the beginning.
 *
 * Note that the only restrictions on the package/class names are:
 * 1) the package must end in the major type of the content type (such as
 *    text, image, application, etc).
 * 2) the class name must be named with the subtype of the content type (for
 *    content type "text/plain", this would be "plain" as in this example; for
 *    content type "image/gif", the class name would be "gif", and the package
 *    name must end with ".image".
 * 3) the class must be a subclass of ContentHandler.
 * 4) It must define the getContent function.
 */
package COM.foo.content.text;

import java.net.ContentHandler;
import java.io.InputStream;
import java.net.URLConnection;
import java.io.IOException;

public class plain extends ContentHandler {
    /**
     * Returns one of several object types (this set may change in future
     * versions):
     * 1) instance of Thread:
     *    Invoke the thread to launch an external viewer.
     * 2) instance of InputStream:
     *    Bring up the "Save to disk" dialog page to allow the content
     *    to be saved to disk.
     * 3) instance of InputStreamImageSource:
     *    Load the image into HotJava in an image viewer page.
     * 4) instance of String:
     *    Go to a new page with the string as the plain text content
     *    of that page.
     */
    public Object getContent(URLConnection uc) {
        try {
            InputStream is = uc.getInputStream();
            StringBuffer sb = new StringBuffer();
            int c;

            sb.append("[Content of " + uc.getURL() + "]\n\n");
            sb.append("[This opening message brought to you by your plain/text\n");
            sb.append("content handler. To remove this content handler, delete the\n");
            sb.append("COM.foo.content.text directory from your class path and\n");
            sb.append("the java.content.handler.pkgs property from your HotJava\n");
            sb.append("properties file.]\n");
            sb.append("----------------------------------------------------------------\n\n");

            // Read the characters from the source, accumulate them into the string buffer.
            // (Not the most efficient, but simplest for this example.)
            while ((c = is.read()) >= 0) {
                sb.append((char)c);
            }

            // Tidy up
            is.close();

            // Return the resulting string to our client (we're case 4 above)
            return sb.toString();
        } catch (IOException e) {
            // For any exception, just return an indication of what went wrong.
            return "Problem reading document: " + uc.getURL();
        }
    }
}
