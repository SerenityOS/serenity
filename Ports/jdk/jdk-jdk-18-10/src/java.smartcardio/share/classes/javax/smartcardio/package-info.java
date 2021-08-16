/*
 *  Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/**
 * Java&#x2122; Smart Card I/O API.
 *
 * This specification describes the Java Smart Card I/O API defined by
 * <a href="http://jcp.org/en/jsr/detail?id=268">JSR 268</a>.
 *
 * It defines a Java API for communication with Smart Cards
 * using ISO/IEC 7816-4 APDUs. It thereby allows Java applications to interact with
 * applications running on the Smart Card, to store and retrieve data
 * on the card, etc.
 *
 * <p>
 * The API is defined by classes in the package
 * {@code javax.smartcardio}. They can be classified as follows:
 *
 * <dl>
 * <dt>Classes describing the corresponding Smart Card structures
 * <dd>
 * <a href="ATR.html">ATR</a>,
 * <a href="CommandAPDU.html">CommandAPDU</a>,
 * <a href="ResponseAPDU.html">ResponseAPDU</a>
 *
 * <dt>Factory to obtain implementations
 * <dd>
 * <a href="TerminalFactory.html">TerminalFactory</a>
 *
 * <dt>Main classes for card and terminal functions
 * <dd>
 * <a href="CardTerminals.html">CardTerminals</a>,
 * <a href="CardTerminal.html">CardTerminal</a>,
 * <a href="Card.html">Card</a>,
 * <a href="CardChannel.html">CardChannel</a>
 *
 * <dt>Supporting permission and exception classes
 * <dd>
 * <a href="CardPermission.html">CardPermission</a>,
 * <a href="CardException.html">CardException</a>,
 * <a href="CardNotPresentException.html">CardNotPresentException</a>
 *
 * <dt>Service provider interface, not accessed directly by applications
 * <dd>
 * <a href="TerminalFactorySpi.html">TerminalFactorySpi</a>
 *
 * </dl>
 *
 *
 * <h2>API Example</h2>
 *
 * A simple example of using the API is:
 * <pre>
 *      // show the list of available terminals
 *      TerminalFactory factory = TerminalFactory.getDefault();
 *      List&lt;CardTerminal&gt; terminals = factory.terminals().list();
 *      System.out.println("Terminals: " + terminals);
 *      // get the first terminal
 *      CardTerminal terminal = terminals.get(0);
 *      // establish a connection with the card
 *      Card card = terminal.connect("T=0");
 *      System.out.println("card: " + card);
 *      CardChannel channel = card.getBasicChannel();
 *      ResponseAPDU r = channel.transmit(new CommandAPDU(c1));
 *      System.out.println("response: " + toString(r.getBytes()));
 *      // disconnect
 *      card.disconnect(false);
 * </pre>
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
package javax.smartcardio;
