/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.callback;

/**
 * <p> An application implements a {@code CallbackHandler} and passes
 * it to underlying security services so that they may interact with
 * the application to retrieve specific authentication data,
 * such as usernames and passwords, or to display certain information,
 * such as error and warning messages.
 *
 * <p> CallbackHandlers are implemented in an application-dependent fashion.
 * For example, implementations for an application with a graphical user
 * interface (GUI) may pop up windows to prompt for requested information
 * or to display error messages.  An implementation may also choose to obtain
 * requested information from an alternate source without asking the end user.
 *
 * <p> Underlying security services make requests for different types
 * of information by passing individual Callbacks to the
 * {@code CallbackHandler}.  The {@code CallbackHandler}
 * implementation decides how to retrieve and display information
 * depending on the Callbacks passed to it.  For example,
 * if the underlying service needs a username and password to
 * authenticate a user, it uses a {@code NameCallback} and
 * {@code PasswordCallback}.  The {@code CallbackHandler}
 * can then choose to prompt for a username and password serially,
 * or to prompt for both in a single window.
 *
 * <p> A default {@code CallbackHandler} class implementation
 * may be specified by setting the value of the
 * {@code auth.login.defaultCallbackHandler} security property.
 *
 * <p> If the security property is set to the fully qualified name of a
 * {@code CallbackHandler} implementation class,
 * then a {@code LoginContext} will load the specified
 * {@code CallbackHandler} and pass it to the underlying LoginModules.
 * The {@code LoginContext} only loads the default handler
 * if it was not provided one.
 *
 * <p> All default handler implementations must provide a public
 * zero-argument constructor.
 *
 * @since 1.4
 * @see java.security.Security security properties
 */
public interface CallbackHandler {

    /**
     * <p> Retrieve or display the information requested in the
     * provided Callbacks.
     *
     * <p> The {@code handle} method implementation checks the
     * instance(s) of the {@code Callback} object(s) passed in
     * to retrieve or display the requested information.
     * The following example is provided to help demonstrate what an
     * {@code handle} method implementation might look like.
     * This example code is for guidance only.  Many details,
     * including proper error handling, are left out for simplicity.
     *
     * <pre>{@code
     * public void handle(Callback[] callbacks)
     * throws IOException, UnsupportedCallbackException {
     *
     *   for (int i = 0; i < callbacks.length; i++) {
     *      if (callbacks[i] instanceof TextOutputCallback) {
     *
     *          // display the message according to the specified type
     *          TextOutputCallback toc = (TextOutputCallback)callbacks[i];
     *          switch (toc.getMessageType()) {
     *          case TextOutputCallback.INFORMATION:
     *              System.out.println(toc.getMessage());
     *              break;
     *          case TextOutputCallback.ERROR:
     *              System.out.println("ERROR: " + toc.getMessage());
     *              break;
     *          case TextOutputCallback.WARNING:
     *              System.out.println("WARNING: " + toc.getMessage());
     *              break;
     *          default:
     *              throw new IOException("Unsupported message type: " +
     *                                  toc.getMessageType());
     *          }
     *
     *      } else if (callbacks[i] instanceof NameCallback) {
     *
     *          // prompt the user for a username
     *          NameCallback nc = (NameCallback)callbacks[i];
     *
     *          // ignore the provided defaultName
     *          System.err.print(nc.getPrompt());
     *          System.err.flush();
     *          nc.setName((new BufferedReader
     *                  (new InputStreamReader(System.in))).readLine());
     *
     *      } else if (callbacks[i] instanceof PasswordCallback) {
     *
     *          // prompt the user for sensitive information
     *          PasswordCallback pc = (PasswordCallback)callbacks[i];
     *          System.err.print(pc.getPrompt());
     *          System.err.flush();
     *          pc.setPassword(readPassword(System.in));
     *
     *      } else {
     *          throw new UnsupportedCallbackException
     *                  (callbacks[i], "Unrecognized Callback");
     *      }
     *   }
     * }
     *
     * // Reads user password from given input stream.
     * private char[] readPassword(InputStream in) throws IOException {
     *    // insert code to read a user password from the input stream
     * }
     * }</pre>
     *
     * @param callbacks an array of {@code Callback} objects provided
     *          by an underlying security service which contains
     *          the information requested to be retrieved or displayed.
     *
     * @exception java.io.IOException if an input or output error occurs.
     *
     * @exception UnsupportedCallbackException if the implementation of this
     *          method does not support one or more of the Callbacks
     *          specified in the {@code callbacks} parameter.
     */
    void handle(Callback[] callbacks)
    throws java.io.IOException, UnsupportedCallbackException;
}
