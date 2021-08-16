/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides the classes for implementing networking applications.
 *
 * <p> The java.net package can be roughly divided in two sections:</p>
 * <ul>
 *     <li><p><i>A Low Level API</i>, which deals with the
 *               following abstractions:</p>
 *     <ul>
 *       <li><p><i>Addresses</i>, which are networking identifiers,
 *              like IP addresses.</p></li>
 *       <li><p><i>Sockets</i>, which are basic bidirectional data communication
 *              mechanisms.</p></li>
 *       <li><p><i>Interfaces</i>, which describe network interfaces. </p></li>
 *     </ul></li>
 *     <li> <p><i>A High Level API</i>, which deals with the following
 *          abstractions:</p>
 *     <ul>
 *       <li><p><i>URIs</i>, which represent
 *               Universal Resource Identifiers.</p></li>
 *       <li><p><i>URLs</i>, which represent
 *               Universal Resource Locators.</p></li>
 *       <li><p><i>Connections</i>, which represents connections to the resource
 *               pointed to by <i>URLs</i>.</p></li>
 *       </ul></li>
 * </ul>
 * <h2>Addresses</h2>
 * <p>Addresses are used throughout the java.net APIs as either host
 *    identifiers, or socket endpoint identifiers.</p>
 * <p>The {@link java.net.InetAddress} class is the abstraction representing an
 *    IP (Internet Protocol) address.  It has two subclasses:
 * <ul>
 *       <li>{@link java.net.Inet4Address} for IPv4 addresses.</li>
 *       <li>{@link java.net.Inet6Address} for IPv6 addresses.</li>
 * </ul>
 * <p>But, in most cases, there is no need to deal directly with the subclasses,
 *    as the InetAddress abstraction should cover most of the needed
 *    functionality.</p>
 * <h3><b>About IPv6</b></h3>
 * <p>Not all systems have support for the IPv6 protocol, and while the Java
 *    networking stack will attempt to detect it and use it transparently when
 *    available, it is also possible to disable its use with a system property.
 *    In the case where IPv6 is not available, or explicitly disabled,
 *    Inet6Address are not valid arguments for most networking operations any
 *    more. While methods like {@link java.net.InetAddress#getByName} are
 *    guaranteed not to return an Inet6Address when looking up host names, it
 *    is possible, by passing literals, to create such an object. In which
 *    case, most methods, when called with an Inet6Address will throw an
 *    Exception.</p>
 * <h2>Sockets</h2>
 * <p>Sockets are means to establish a communication link between machines over
 *    the network. The java.net package provides 4 kinds of Sockets:</p>
 * <ul>
 *       <li>{@link java.net.Socket} is a TCP client API, and will typically
 *            be used to {@linkplain java.net.Socket#connect(SocketAddress)
 *            connect} to a remote host.</li>
 *       <li>{@link java.net.ServerSocket} is a TCP server API, and will
 *            typically {@linkplain java.net.ServerSocket#accept accept}
 *            connections from client sockets.</li>
 *       <li>{@link java.net.DatagramSocket} is a UDP endpoint API and is used
 *            to {@linkplain java.net.DatagramSocket#send send} and
 *            {@linkplain java.net.DatagramSocket#receive receive}
 *            {@linkplain java.net.DatagramPacket datagram packets}.</li>
 *       <li>{@link java.net.MulticastSocket} is a subclass of
 *            {@code DatagramSocket} used when dealing with multicast
 *            groups.</li>
 * </ul>
 * <p>Sending and receiving with TCP sockets is done through InputStreams and
 *    OutputStreams which can be obtained via the
 *    {@link java.net.Socket#getInputStream} and
 *    {@link java.net.Socket#getOutputStream} methods.</p>
 * <h2>Interfaces</h2>
 * <p>The {@link java.net.NetworkInterface} class provides APIs to browse and
 *    query all the networking interfaces (e.g. ethernet connection or PPP
 *    endpoint) of the local machine. It is through that class that you can
 *    check if any of the local interfaces is configured to support IPv6.</p>
 * <p>Note, all conforming implementations must support at least one
 *    {@code NetworkInterface} object, which must either be connected to a
 *    network, or be a "loopback" interface that can only communicate with
 *    entities on the same machine.</p>
 *
 * <h2>High level API</h2>
 * <p>A number of classes in the java.net package do provide for a much higher
 *    level of abstraction and allow for easy access to resources on the
 *    network. The classes are:
 * <ul>
 *       <li>{@link java.net.URI} is the class representing a
 *            Universal Resource Identifier, as specified in RFC 2396.
 *            As the name indicates, this is just an Identifier and doesn't
 *            provide directly the means to access the resource.</li>
 *       <li>{@link java.net.URL} is the class representing a
 *            Universal Resource Locator, which is both an older concept for
 *            URIs and a means to access the resources.</li>
 *       <li>{@link java.net.URLConnection} is created from a URL and is the
 *            communication link used to access the resource pointed by the
 *            URL. This abstract class will delegate most of the work to the
 *            underlying protocol handlers like http or https.</li>
 *       <li>{@link java.net.HttpURLConnection} is a subclass of URLConnection
 *            and provides some additional functionalities specific to the
 *            HTTP protocol. This API has been superseded by the newer
 *            {@linkplain java.net.http HTTP Client API}.</li>
 * </ul>
 * <p>The recommended usage is to use {@link java.net.URI} to identify
 *    resources, then convert it into a {@link java.net.URL} when it is time to
 *    access the resource. From that URL, you can either get the
 *    {@link java.net.URLConnection} for fine control, or get directly the
 *    InputStream.
 * <p>Here is an example:</p>
 * <pre>
 * URI uri = new URI("http://www.example.com/");
 * URL url = uri.toURL();
 * InputStream in = url.openStream();
 * </pre>
 * <h2>Protocol Handlers</h2>
 * As mentioned, URL and URLConnection rely on protocol handlers which must be
 * present, otherwise an Exception is thrown. This is the major difference with
 * URIs which only identify resources, and therefore don't need to have access
 * to the protocol handler. So, while it is possible to create an URI with any
 * kind of protocol scheme (e.g. {@code myproto://myhost.mydomain/resource/}),
 * a similar URL will try to instantiate the handler for the specified protocol;
 * if it doesn't exist an exception will be thrown.
 * <p>By default the protocol handlers are loaded dynamically from the default
 *    location. It is, however, possible to deploy additional protocols handlers
 *    as {@link java.util.ServiceLoader services}. Service providers of type
 *    {@linkplain java.net.spi.URLStreamHandlerProvider} are located at
 *    runtime, as specified in the {@linkplain
 *    java.net.URL#URL(String,String,int,String) URL constructor}.
 * <h2>Additional Specification</h2>
 * <ul>
 *       <li><a href="doc-files/net-properties.html">
 *            Networking System Properties</a></li>
 * </ul>
 *
 * @since 1.0
 */
package java.net;
