/*
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*-
 *      mailto stream opener
 */

package sun.net.www.protocol.mailto;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.io.*;
import sun.net.www.*;
//import sun.net.www.protocol.news.ArticlePoster;
import sun.net.smtp.SmtpClient;

/** open an nntp input stream given a URL */
public class Handler extends URLStreamHandler {

/*
//     private String decodePercent(String s) {
//      if (s==null || s.indexOf('%') < 0)
//          return s;
//      int limit = s.length();
//      char d[] = new char[limit];
//      int dp = 0;
//      for (int sp = 0; sp < limit; sp++) {
//          int c = s.charAt(sp);
//          if (c == '%' && sp + 2 < limit) {
//              int s1 = s.charAt(sp + 1);
//              int s2 = s.charAt(sp + 2);
//              if ('0' <= s1 && s1 <= '9')
//                  s1 = s1 - '0';
//              else if ('a' <= s1 && s1 <= 'f')
//                  s1 = s1 - 'a' + 10;
//              else if ('A' <= s1 && s1 <= 'F')
//                  s1 = s1 - 'A' + 10;
//              else
//                  s1 = -1;
//              if ('0' <= s2 && s2 <= '9')
//                  s2 = s2 - '0';
//              else if ('a' <= s2 && s2 <= 'f')
//                  s2 = s2 - 'a' + 10;
//              else if ('A' <= s2 && s2 <= 'F')
//                  s2 = s2 - 'A' + 10;
//              else
//                  s2 = -1;
//              if (s1 >= 0 && s2 >= 0) {
//                  c = (s1 << 4) | s2;
//                  sp += 2;
//              }
//          }
//          d[dp++] = (char) c;
//      }
//      return new String(d, 0, dp);
//     }

//     public InputStream openStream(URL u) {
//          String dest = u.file;
//          String subj = "";
//          int lastsl = dest.lastIndexOf('/');
//          if (lastsl >= 0) {
//              int st = dest.charAt(0) == '/' ? 1 : 0;
//              if (lastsl > st)
//                  subj = dest.substring(st, lastsl);
//              dest = dest.substring(lastsl + 1);
//          }
//          if (u.postData != null) {
//              ArticlePoster.MailTo("Posted form",
//                                   decodePercent(dest),
//                                   u.postData);
//          }
//          else
//              ArticlePoster.MailTo(decodePercent(subj), decodePercent(dest));
//      return null;
//     }
    */

    public synchronized URLConnection openConnection(URL u) {
        return new MailToURLConnection(u);
    }

    /**
     * This method is called to parse the string spec into URL u for a
     * mailto protocol.
     *
     * @param   u the URL to receive the result of parsing the spec
     * @param   spec the URL string to parse
     * @param   start the character position to start parsing at.  This is
     *          just past the ':'.
     * @param   limit the character position to stop parsing at.
     */
    public void parseURL(URL u, String spec, int start, int limit) {

        String protocol = u.getProtocol();
        String host = "";
        int port = u.getPort();
        String file = "";

        if (start < limit) {
            file = spec.substring(start, limit);
        }
        /*
         * Let's just make sure we DO have an Email address in the URL.
         */
        boolean nogood = false;
        if (file == null || file.isEmpty())
            nogood = true;
        else {
            boolean allwhites = true;
            for (int i = 0; i < file.length(); i++)
                if (!Character.isWhitespace(file.charAt(i)))
                    allwhites = false;
            if (allwhites)
                nogood = true;
        }
        if (nogood)
            throw new RuntimeException("No email address");
        setURLHandler(u, protocol, host, port, file, null);
    }

    /**
     * This method is used to suppress the deprecated warning
     *
     * @param   u the URL to receive the result of parsing the spec
     * @param   spec the URL string to parse
     * @param   start the character position to start parsing at.  This is
     *          just past the ':'.
     * @param   limit the character position to stop parsing at.
     */
    @SuppressWarnings("deprecation")
    private void setURLHandler(URL u, String protocol, String host, int port, String file, String ref) {
        setURL(u,protocol,host,port,file,null);
    }
}
