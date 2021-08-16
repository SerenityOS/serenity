/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * <p> Implementations of this interface are passed to a
 * {@code CallbackHandler}, allowing underlying security services
 * the ability to interact with a calling application to retrieve specific
 * authentication data such as usernames and passwords, or to display
 * certain information, such as error and warning messages.
 *
 * <p> {@code Callback} implementations do not retrieve or
 * display the information requested by underlying security services.
 * {@code Callback} implementations simply provide the means
 * to pass such requests to applications, and for applications,
 * if appropriate, to return requested information back to the
 * underlying security services.
 *
 * @since 1.4
 * @see javax.security.auth.callback.CallbackHandler
 * @see javax.security.auth.callback.ChoiceCallback
 * @see javax.security.auth.callback.ConfirmationCallback
 * @see javax.security.auth.callback.LanguageCallback
 * @see javax.security.auth.callback.NameCallback
 * @see javax.security.auth.callback.PasswordCallback
 * @see javax.security.auth.callback.TextInputCallback
 * @see javax.security.auth.callback.TextOutputCallback
 */
public interface Callback { }
