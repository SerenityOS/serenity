/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
import java.applet.Applet;
import java.io.*;
import java.net.*;

/**
 * Simple Applet for exposing the Socket constructor
 * bug.
 */
public class SocketImplTest extends Applet {

    static public void main(String[] args) {
        System.setSecurityManager(new SecurityManager());
        SocketImplTest s = new SocketImplTest();
        s.init();
        s.start();
    }


    /**
     * A no-op SocketImpl descendant.
     */
    class MySocketImpl extends SocketImpl {
        protected void accept(SocketImpl impl) throws IOException {
        }

        protected int available(){
            return 0;
        }

        protected void bind(InetAddress host, int port){
        }

        protected void close(){
        }

        protected void connect(InetAddress address, int port){
        }

        protected void connect(String host, int port){
        }

        protected void connect(SocketAddress a, int t) throws IOException {
        }


        protected void create(boolean stream){
        }

        protected InputStream getInputStream(){
            return null;
        }

        protected OutputStream getOutputStream(){
            return null;
        }

        protected void listen(int backlog){
        }

        public Object getOption(int optID){
            return null;
        }

        public void setOption(int optID, Object value){
        }

        protected void sendUrgentData(int i){
        }
    }

    class MyDatagramSocketImpl extends DatagramSocketImpl {
        protected void create() throws SocketException {
        }

        protected void bind(int lport, InetAddress laddr) throws SocketException {
        }

        protected void send(DatagramPacket p) throws IOException {
        }

        protected int peek(InetAddress i) throws IOException {
            return 0;
        }

        protected int peekData(DatagramPacket p) throws IOException {
            return 0;
        }

        protected void receive(DatagramPacket p) throws IOException {
        }

        protected void setTTL(byte ttl) throws IOException {
        }

        protected byte getTTL() throws IOException {
            return 0;
        }

        protected void setTimeToLive(int ttl) throws IOException {
        }

        protected int getTimeToLive() throws IOException {
            return 0;
        }

        protected void join(InetAddress inetaddr) throws IOException {
        }

        protected void leave(InetAddress inetaddr) throws IOException {
        }

        protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException {
        }

        protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException {
        }

        protected void close() {
        }

        public Object getOption(int optID){
            return null;
        }

        public void setOption(int optID, Object value){
        }

    }

    /**
     * A no-op Socket descendant.
     */
    class MySocket extends Socket {
        public MySocket(SocketImpl impl) throws IOException {
            super(impl);
        }
    }

    class MyDatagramSocket extends DatagramSocket {
        public MyDatagramSocket(DatagramSocketImpl impl) {
            super(impl);
        }
    }

    /**
     * Our test case entrypoint. Generates
     * a SecurityException.
     */
    public void init(){
        MySocketImpl socketImpl = new MySocketImpl();
        MyDatagramSocketImpl dgramSocketImpl = new MyDatagramSocketImpl();

        try{
            MySocket socko = new MySocket(socketImpl);
            MyDatagramSocket dsock = new MyDatagramSocket(dgramSocketImpl);
        } catch(IOException ioex){
            System.err.println(ioex);
        } catch(SecurityException sec) {
            throw new RuntimeException("Failed. Creation of socket throwing SecurityException: ");
        }
    }
}
