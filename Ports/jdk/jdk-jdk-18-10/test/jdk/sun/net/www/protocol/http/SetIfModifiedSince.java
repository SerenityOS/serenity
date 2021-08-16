/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4213164 8172253
 * @library /test/lib
 * @summary setIfModifiedSince method in HttpURLConnection sometimes fails
 */
import java.util.*;
import java.io.*;
import java.net.*;

import jdk.test.lib.net.URIBuilder;

public class SetIfModifiedSince implements Runnable {

  ServerSocket serverSock;

  public void run() {
      try {
          Socket s = serverSock.accept();
          InputStream in = s.getInputStream();
          byte b[] = new byte[4096];

          // assume we read the entire http request
          // (bad assumption but okay for test case)
          int nread = in.read(b);

          // check the date format by the position of the comma
          String request = new String(b, 0, nread);
          int pos = request.indexOf("If-Modified-Since:");
          int respCode = 200;
          if (pos != -1) {
              pos += "If-Modified-Since:".length() + 4;
              if (pos < nread) {
                  if (request.charAt(pos) == (char)',') {
                      respCode = 304;
                  }
              }
          }

          OutputStream o = s.getOutputStream();
          if (respCode == 304) {
              o.write( "HTTP/1.1 304 Not Modified".getBytes() );
          } else  {
              o.write( "HTTP/1.1 200 OK".getBytes() );
          }
          o.write( (byte)'\r' );
          o.write( (byte)'\n' );
          o.write( (byte)'\r' );
          o.write( (byte)'\n' );
          o.flush();

      } catch (Exception e) { }
  }


  public SetIfModifiedSince() throws Exception {

      serverSock = new ServerSocket();
      serverSock.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
     int port = serverSock.getLocalPort();

     Thread thr = new Thread(this);
     thr.start();

     Date date = new Date(new Date().getTime()-1440000); // this time yesterday
     URL url;
     HttpURLConnection con;

     //url = new URL(args[0]);
      url = URIBuilder.newBuilder()
              .scheme("http")
              .loopback()
              .port(port)
              .path("/anything")
              .toURL();
     con = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);

     con.setIfModifiedSince(date.getTime());
     con.connect();
     int ret = con.getResponseCode();

     if (ret == 304) {
       System.out.println("Success!");
     } else {
       throw new RuntimeException("Test failed! Http return code using setIfModified method is:"+ret+"\nNOTE:some web servers are not implemented according to RFC, thus a failed test does not necessarily indicate a bug in setIfModifiedSince method");
     }
  }

  public static void main(String args[]) throws Exception {
      new SetIfModifiedSince();
  }
}
