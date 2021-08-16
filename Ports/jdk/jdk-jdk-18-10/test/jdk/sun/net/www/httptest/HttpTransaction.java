/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import sun.net.www.MessageHeader;

/**
 * This class encapsulates a HTTP request received and a response to be
 * generated in one transaction. It provides methods for examaining the
 * request from the client, and for building and sending a reply.
 */

public class HttpTransaction {

    String command;
    URI requesturi;
    TestHttpServer.Server server;
    MessageHeader reqheaders, reqtrailers;
    String reqbody;
    byte[] rspbody;
    MessageHeader rspheaders, rsptrailers;
    SelectionKey  key;
    int rspbodylen;
    boolean rspchunked;

    HttpTransaction (TestHttpServer.Server server, String command,
                        URI requesturi, MessageHeader headers,
                        String body, MessageHeader trailers, SelectionKey  key) {
        this.command = command;
        this.requesturi = requesturi;
        this.reqheaders = headers;
        this.reqbody = body;
        this.reqtrailers = trailers;
        this.key = key;
        this.server = server;
    }

    /**
     * Get the value of a request header whose name is specified by the
     * String argument.
     *
     * @param key the name of the request header
     * @return the value of the header or null if it does not exist
     */
    public String getRequestHeader (String key) {
        return reqheaders.findValue (key);
    }

    /**
     * Get the value of a response header whose name is specified by the
     * String argument.
     *
     * @param key the name of the response header
     * @return the value of the header or null if it does not exist
     */
    public String getResponseHeader (String key) {
        return rspheaders.findValue (key);
    }

    /**
     * Get the request URI
     *
     * @return the request URI
     */
    public URI getRequestURI () {
        return requesturi;
    }

    public String toString () {
        StringBuffer buf = new StringBuffer();
        buf.append ("Request from: ").append (key.channel().toString()).append("\r\n");
        buf.append ("Command: ").append (command).append("\r\n");
        buf.append ("Request URI: ").append (requesturi).append("\r\n");
        buf.append ("Headers: ").append("\r\n");
        buf.append (reqheaders.toString()).append("\r\n");
        buf.append ("Body: ").append (reqbody).append("\r\n");
        buf.append ("---------Response-------\r\n");
        buf.append ("Headers: ").append("\r\n");
        if (rspheaders != null) {
            buf.append (rspheaders.toString()).append("\r\n");
        }
        String rbody = rspbody == null? "": new String (rspbody);
        buf.append ("Body: ").append (rbody).append("\r\n");
        return new String (buf);
    }

    /**
     * Get the value of a request trailer whose name is specified by
     * the String argument.
     *
     * @param key the name of the request trailer
     * @return the value of the trailer or null if it does not exist
     */
    public String getRequestTrailer (String key) {
        return reqtrailers.findValue (key);
    }

    /**
     * Add a response header to the response. Multiple calls with the same
     * key value result in multiple header lines with the same key identifier
     * @param key the name of the request header to add
     * @param val the value of the header
     */
    public void addResponseHeader (String key, String val) {
        if (rspheaders == null)
            rspheaders = new MessageHeader ();
        rspheaders.add (key, val);
    }

    /**
     * Set a response header. Searches for first header with named key
     * and replaces its value with val
     * @param key the name of the request header to add
     * @param val the value of the header
     */
    public void setResponseHeader (String key, String val) {
        if (rspheaders == null)
            rspheaders = new MessageHeader ();
        rspheaders.set (key, val);
    }

    /**
     * Add a response trailer to the response. Multiple calls with the same
     * key value result in multiple trailer lines with the same key identifier
     * @param key the name of the request trailer to add
     * @param val the value of the trailer
     */
    public void addResponseTrailer (String key, String val) {
        if (rsptrailers == null)
            rsptrailers = new MessageHeader ();
        rsptrailers.add (key, val);
    }

    /**
     * Get the request method
     *
     * @return the request method
     */
    public String getRequestMethod (){
        return command;
    }

    /**
     * Perform an orderly close of the TCP connection associated with this
     * request. This method guarantees that any response already sent will
     * not be reset (by this end). The implementation does a shutdownOutput()
     * of the TCP connection and for a period of time consumes and discards
     * data received on the reading side of the connection. This happens
     * in the background. After the period has expired the
     * connection is completely closed.
     */

    public void orderlyClose () {
        try {
            server.orderlyCloseChannel (key);
        } catch (IOException e) {
            System.out.println (e);
        }
    }

    /**
     * Do an immediate abortive close of the TCP connection associated
     * with this request.
     */
    public void abortiveClose () {
        try {
            server.abortiveCloseChannel(key);
        } catch (IOException e) {
            System.out.println (e);
        }
    }

    /**
     * Get the SocketChannel associated with this request
     *
     * @return the socket channel
     */
    public SocketChannel channel() {
        return (SocketChannel) key.channel();
    }

    /**
     * Get the request entity body associated with this request
     * as a single String.
     *
     * @return the entity body in one String
     */
    public String getRequestEntityBody (){
        return reqbody;
    }

    /**
     * Set the entity response body with the given string
     * The content length is set to the length of the string
     * @param body the string to send in the response
     */
    public void setResponseEntityBody (String body){
        rspbody = body.getBytes();
        rspbodylen = body.length();
        rspchunked = false;
        addResponseHeader ("Content-length", Integer.toString (rspbodylen));
    }
    /**
     * Set the entity response body with the given byte[]
     * The content length is set to the gven length
     * @param body the string to send in the response
     */
    public void setResponseEntityBody (byte[] body, int len){
        rspbody = body;
        rspbodylen = len;
        rspchunked = false;
        addResponseHeader ("Content-length", Integer.toString (rspbodylen));
    }


    /**
     * Set the entity response body by reading the given inputstream
     *
     * @param is the inputstream from which to read the body
     */
    public void setResponseEntityBody (InputStream is) throws IOException {
        byte[] buf = new byte [2048];
        byte[] total = new byte [2048];
        int total_len = 2048;
        int c, len=0;
        while ((c=is.read (buf)) != -1) {
            if (len+c > total_len) {
                byte[] total1 = new byte [total_len * 2];
                System.arraycopy (total, 0, total1, 0, len);
                total = total1;
                total_len = total_len * 2;
            }
            System.arraycopy (buf, 0, total, len, c);
            len += c;
        }
        setResponseEntityBody (total, len);
    }

    /* chunked */

    /**
     * Set the entity response body with the given array of strings
     * The content encoding is set to "chunked" and each array element
     * is sent as one chunk.
     * @param body the array of string chunks to send in the response
     */
    public void setResponseEntityBody (String[] body) {
        StringBuffer buf = new StringBuffer ();
        int len = 0;
        for (int i=0; i<body.length; i++) {
            String chunklen = Integer.toHexString (body[i].length());
            len += body[i].length();
            buf.append (chunklen).append ("\r\n");
            buf.append (body[i]).append ("\r\n");
        }
        buf.append ("0\r\n");
        rspbody = new String (buf).getBytes();
        rspbodylen = rspbody.length;
        rspchunked = true;
        addResponseHeader ("Transfer-encoding", "chunked");
    }

    /**
     * Send the response with the current set of response parameters
     * but using the response code and string tag line as specified
     * @param rCode the response code to send
     * @param rTag the response string to send with the response code
     */
    public void sendResponse (int rCode, String rTag) throws IOException {
        OutputStream os = new TestHttpServer.NioOutputStream(channel());
        PrintStream ps = new PrintStream (os);
        ps.print ("HTTP/1.1 " + rCode + " " + rTag + "\r\n");
        if (rspheaders != null) {
            rspheaders.print (ps);
        } else {
            ps.print ("\r\n");
        }
        ps.flush ();
        if (rspbody != null) {
            os.write (rspbody, 0, rspbodylen);
            os.flush();
        }
        if (rsptrailers != null) {
            rsptrailers.print (ps);
        } else if (rspchunked) {
            ps.print ("\r\n");
        }
        ps.flush();
    }

    /* sends one byte less than intended */

    public void sendPartialResponse (int rCode, String rTag)throws IOException {
        OutputStream os = new TestHttpServer.NioOutputStream(channel());
        PrintStream ps = new PrintStream (os);
        ps.print ("HTTP/1.1 " + rCode + " " + rTag + "\r\n");
        ps.flush();
        if (rspbody != null) {
            os.write (rspbody, 0, rspbodylen-1);
            os.flush();
        }
        if (rsptrailers != null) {
            rsptrailers.print (ps);
        }
        ps.flush();
    }
}
