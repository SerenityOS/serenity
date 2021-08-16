/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4160200
 * @summary Make sure URLConnection.getContentHandler
 *     can handle MIME types with attributes
 * @library /test/lib
 * @modules java.base/sun.net.www java.base/sun.net.www.content.text
 */
import java.net.*;
import java.io.*;
import sun.net.www.content.text.*;
import sun.net.www.MessageHeader;
import static java.net.Proxy.NO_PROXY;

import jdk.test.lib.net.URIBuilder;

public class HandleContentTypeWithAttrs {

    URL url;

    public HandleContentTypeWithAttrs (int port) throws Exception {

        // Request echo.html from myHttpServer.
        // In the header of the response, we make
        // the content type have some attributes.
        url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path("/echo.html")
                .toURL();
        URLConnection urlConn = url.openConnection(NO_PROXY);

        // the method getContent() calls the method
        // getContentHandler(). With the fix, the method
        // getContentHandler() gets the correct content
        // handler for our response -  it should be the
        // PlainText conten handler; without the fix,
        // it gets the UnknownContent handler.
        // So based on what the getContent()
        // returns, we know whether getContentHandler()
        // can handle content type with attributes or not.
        Object obj = urlConn.getContent();

        if (!(obj instanceof PlainTextInputStream))
            throw new Exception("Cannot get the correct content handler.");
    }

    public static void main(String [] argv) throws Exception {
        // Start myHttpServer
        myHttpServer testServer = new myHttpServer();
        testServer.startServer(0);
        int serverPort = testServer.getServerLocalPort();
        new HandleContentTypeWithAttrs(serverPort);
    }
}

// myHttpServer is pretty much like
// sun.net.www.httpd.BasicHttpServer.
// But we only need it to handle one
// simple request.
class myHttpServer implements Runnable, Cloneable {

    /** Socket for communicating with client. */
    public Socket clientSocket = null;
    private Thread serverInstance;
    private ServerSocket serverSocket;

    /** Stream for printing to the client. */
    public PrintStream clientOutput;

    /** Buffered stream for reading replies from client. */
    public InputStream clientInput;

    static URL defaultContext;

    /** Close an open connection to the client. */
    public void close() throws IOException {
        clientSocket.close();
        clientSocket = null;
        clientInput = null;
        clientOutput = null;
    }

    final public void run() {
        if (serverSocket != null) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            try {
                // wait for incoming request
                Socket ns = serverSocket.accept();
                myHttpServer n = (myHttpServer)clone();
                n.serverSocket = null;
                n.clientSocket = ns;
                new Thread(n).start();
            } catch(Exception e) {
                System.out.print("Server failure\n");
                e.printStackTrace();
            } finally {
                try { serverSocket.close(); } catch(IOException unused) {}
            }
        } else {
            try {
                clientOutput = new PrintStream(
                    new BufferedOutputStream(clientSocket.getOutputStream()),
                                             false);
                clientInput = new BufferedInputStream(
                                             clientSocket.getInputStream());
                serviceRequest();

            } catch(Exception e) {
                // System.out.print("Service handler failure\n");
                // e.printStackTrace();
            } finally {
                try { close(); }  catch(IOException unused) {}
            }
        }
    }

    /** Start a server on port <i>port</i>.  It will call serviceRequest()
        for each new connection. */
    final public void startServer(int port) throws IOException {
        serverSocket = new ServerSocket(port, 50,
                InetAddress.getLoopbackAddress());
        serverInstance = new Thread(this);
        serverInstance.start();
    }

    final public int getServerLocalPort() throws Exception {
        if (serverSocket != null) {
            return serverSocket.getLocalPort();
        }
        throw new Exception("serverSocket is null");
    }

    MessageHeader mh;

    final public void serviceRequest() {
        //totalConnections++;
        try {
            mh = new MessageHeader(clientInput);
            String cmd = mh.findValue(null);
            // if (cmd == null) {
            //  error("Missing command " + mh);
            //  return;
            // }
            int fsp = cmd.indexOf(' ');
            // if (fsp < 0) {
            //  error("Syntax error in command: " + cmd);
            //  return;
            //  }
            String k = cmd.substring(0, fsp);
            int nsp = cmd.indexOf(' ', fsp + 1);
            String p1, p2;
            if (nsp > 0) {
                p1 = cmd.substring(fsp + 1, nsp);
                p2 = cmd.substring(nsp + 1);
            } else {
                p1 = cmd.substring(fsp + 1);
                p2 = null;
            }
            // expectsMime = p2 != null;
            if (k.equalsIgnoreCase("get"))
                getRequest(new URL(defaultContext, p1), p2);
            else {
                //      error("Unknown command: " + k + " (" + cmd + ")");
                return;
            }
        } catch(IOException e) {
            // totally ignore IOException.  They're usually client crashes.
        } catch(Exception e) {
            //  error("Exception: " + e);
            e.printStackTrace();
        }
    }

    /** Satisfy one get request.  It is invoked with the clientInput and
        clientOutput streams initialized.  This method handles one client
        connection. When it is done, it can simply exit. The default
        server just echoes it's input. */
    protected void getRequest(URL u, String param) {
        try {
            if (u.getFile().equals("/echo.html")) {
               startHtml("Echo reply");
               clientOutput.print("<p>URL was " + u.toExternalForm() + "\n");
               clientOutput.print("<p>Socket was " + clientSocket + "\n<p><pre>");
               mh.print(clientOutput);
           }
        } catch(Exception e) {
            System.out.print("Failed on "+u.getFile()+"\n");
            e.printStackTrace();
        }
    }
     /**
      * Clone this object;
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    public myHttpServer () {
        try {
            defaultContext = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .path("/")
                    .toURL();
        } catch(Exception e) {
            System.out.println("Failed to construct default URL context: "
                               + e);
            e.printStackTrace();
        }
    }

    // Make the content type have some attributes
    protected void startHtml(String title) {
        clientOutput.print("HTTP/1.0 200 Document follows\n" +
                           "Server: Java/" + getClass().getName() + "\n" +
                           "Content-type: text/plain; charset=Shift_JIS \n\n");
    }

}
