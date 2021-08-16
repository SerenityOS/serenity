/*
* Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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


import java.io.File;
import java.io.FileReader;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

/* @test
   @bug 8078268
   @summary  javax.swing.text.html.parser.Parser parseScript incorrectly optimized
   @author Mikhail Cherkasov
   @run main bug8078268
*/
public class bug8078268 {
    static volatile boolean parsingDone = false;
    static volatile Exception exception;

    public static void main(String[] args) throws Exception {
        long timeout = 10_000;
        long s = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                HTMLEditorKit htmlKit = new HTMLEditorKit();
                Document doc = htmlKit.createDefaultDocument();
                try {
                    htmlKit.read(new FileReader(getDirURL() + "slowparse.html"), doc, 0);
                    parsingDone = true;
                } catch (Exception e) {
                    exception = e;
                }
            }
        });
        while (!parsingDone && exception == null && System.currentTimeMillis() - s < timeout) {
            Thread.sleep(200);
        }
        final long took = System.currentTimeMillis() - s;
        if (exception != null) {
            throw exception;
        }
        if (took > timeout) {
            throw new RuntimeException("Parsing takes too long.");
        }
    }

    static String getDirURL() {
        return new File(System.getProperty("test.src", ".")).getAbsolutePath() +
                File.separator;
    }
}
