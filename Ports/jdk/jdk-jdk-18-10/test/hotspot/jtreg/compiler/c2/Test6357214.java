/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6357214
 * @summary Hotspot server compiler gets integer comparison wrong
 *
 * @run main/othervm/timeout=60 -DshowAll=ffo -DeventID=444 compiler.c2.Test6357214
 */

package compiler.c2;

// The test hangs after few iterations before the fix. So it fails if timeout.
public class Test6357214 {
    static class MyResult {
        public boolean next() {
            return true;
        }

        public String getString(String in) {
            if (in.equals("id"))
                return "idFoo";
            if (in.equals("contentKey"))
                return "ckFoo";
            return "Foo";
        }

        public int getInt(String in) {
            if (in.equals("processingComplete"))
                return 0;
            return 1;
        }

        public byte[] getBytes(String in) {
            byte[] arr = null;
            if (in.equals("content")) {
                arr = new byte[65536];
                byte j = 32;
                for (int i=0; i<65536; i++) {
                    arr[i] = j;
                    if (++j == 127)
                        j=32;
                }
            }
            return arr;
        }
    }

    public static volatile boolean bollocks = true;
    public String create(String context) throws Exception {

        //
        // Extract HTTP parameters
        //

        boolean showAll = System.getProperty("showAll") != null;
          String eventID = System.getProperty("eventID");
          String eventContentKey = System.getProperty("cKey");
        //
        // Build ContentStaging query based on eventID or eventContentKey
        //

        String sql = "select id, processingComplete, contentKey, content "
                   + "from   ContentStaging cs, ContentStagingKey csk "
                   + "where  cs.eventContentKey = csk.eventContentKey ";

        if (eventID != null) {
            sql += "and id = " + eventID;
        }
        else if (eventContentKey != null) {
            sql += "and cs.eventContentKey = '"
                +  eventContentKey
                +  "' having id = max(id)";
        }
        else {
            throw new Exception("Need eventID or eventContentKey");
        }

        //
        // This factory builds a static panel, there is no JSP
        //

        StringBuffer html = new StringBuffer();

        try {

                MyResult result = new MyResult();
            if (result.next()) {

                eventID = result.getString("id");
                int processingComplete = result.getInt("processingComplete");
                String contentKey = result.getString("contentKey");
                byte[] bytes = result.getBytes("content");

                //
                // Print content status and associated controls
                //

                html.append("<br/><font class=\"small\">");
                html.append("Status: ");
                switch (processingComplete) {
                    case  0 :
                    case  1 : html.append("PENDING"); break;
                    case  2 : html.append(contentKey); break;
                    case  3 : html.append(eventID); break;
                    default : html.append("UNKNONW");
                }
                html.append("</font><br/>");

                //
                // Print at most 20Kb of content unless "showAll" is set
                //

                int limit = showAll ? Integer.MAX_VALUE : 1024 * 20;
                System.out.println(limit);
                html.append("<pre>");
                for (int i = 0; bytes != null && i < bytes.length; i++) {
                    char c = (char) bytes[i];
                    switch (c) {
                        case '<' : html.append("&lt;");  break;
                        case '>' : html.append("&gt;");  break;
                        case '&' : html.append("&amp;"); break;
                        default  : html.append(c);
                    }

                    if (i > limit) {
                        while (bollocks);
                        // System.out.println("i is " + i);
                        // System.out.println("limit is " + limit);
                        html.append("...\n</pre>");
                        html.append(eventID);
                        html.append("<pre>");
                        break;
                    }
                }
                html.append("</pre>");
            }
        }
        catch (Exception exception) {
            throw exception;
        }
        finally {
            html.append("Oof!!");
        }
        String ret = html.toString();
        System.out.println("Returning string length = "+ ret.length());
        return ret;
    }

    public static void main(String[] args) throws Exception {
                int length=0;

                for (int i = 0; i < 100; i++) {
                        length = new Test6357214().create("boo").length();
                        System.out.println(length);
                }
    }
}

