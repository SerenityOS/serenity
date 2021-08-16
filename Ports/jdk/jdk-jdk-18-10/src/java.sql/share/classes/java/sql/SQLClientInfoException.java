/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
package java.sql;

import java.util.Map;

/**
 * The subclass of {@link SQLException} is thrown when one or more client info properties
 * could not be set on a {@code Connection}.  In addition to the information provided
 * by {@code SQLException}, a {@code SQLClientInfoException} provides a list of client info
 * properties that were not set.
 *
 * Some databases do not allow multiple client info properties to be set
 * atomically.  For those databases, it is possible that some of the client
 * info properties had been set even though the {@code Connection.setClientInfo}
 * method threw an exception.  An application can use the {@code getFailedProperties}
 * method to retrieve a list of client info properties that were not set.  The
 * properties are identified by passing a
 * {@code Map<String,ClientInfoStatus>} to
 * the appropriate {@code SQLClientInfoException} constructor.
 *
 * @see ClientInfoStatus
 * @see Connection#setClientInfo
 * @since 1.6
 */
public class SQLClientInfoException extends SQLException {

        /**
         * A {@code Map} containing the client info properties that could not be set.
         */
        @SuppressWarnings("serial") // Not statically typed as Serializable
        private Map<String, ClientInfoStatus>   failedProperties;

        /**
     * Constructs a {@code SQLClientInfoException}  Object.
     * The {@code reason},
     * {@code SQLState}, and failedProperties list are initialized to
     * {@code null} and the vendor code is initialized to 0.
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     * @since 1.6
     */
        public SQLClientInfoException() {

                this.failedProperties = null;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given {@code failedProperties}.
     * The {@code reason} and {@code SQLState} are initialized
     * to {@code null} and the vendor code is initialized to 0.
     *
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     *
     * @since 1.6
     */
        public SQLClientInfoException(Map<String, ClientInfoStatus> failedProperties) {

                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with
     * a given {@code cause} and {@code failedProperties}.
     *
     * The {@code reason}  is initialized to {@code null} if
     * {@code cause==null} or to {@code cause.toString()} if
     * {@code cause!=null} and the vendor code is initialized to 0.
     *
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     * @param cause                                     the (which is saved for later retrieval by the {@code getCause()} method); may be null indicating
     *     the cause is non-existent or unknown.
     *
     * @since 1.6
     */
        public SQLClientInfoException(Map<String, ClientInfoStatus> failedProperties,
                                                           Throwable cause) {

                super(cause != null?cause.toString():null);
                initCause(cause);
                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given {@code reason} and {@code failedProperties}.
     * The {@code SQLState} is initialized
     * to {@code null} and the vendor code is initialized to 0.
     *
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     * @param reason                            a description of the exception
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     *
     * @since 1.6
     */
        public SQLClientInfoException(String reason,
                Map<String, ClientInfoStatus> failedProperties) {

                super(reason);
                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given {@code reason}, {@code cause} and
     * {@code failedProperties}.
     * The  {@code SQLState} is initialized
     * to {@code null} and the vendor code is initialized to 0.
     *
     * @param reason                            a description of the exception
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     * @param cause                                     the underlying reason for this {@code SQLException} (which is saved for later retrieval by the {@code getCause()} method); may be null indicating
     *     the cause is non-existent or unknown.
     *
     * @since 1.6
     */
        public SQLClientInfoException(String reason,
                                                           Map<String, ClientInfoStatus> failedProperties,
                                                           Throwable cause) {

                super(reason);
                initCause(cause);
                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given  {@code reason}, {@code SQLState}  and
     * {@code failedProperties}.
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method. The vendor code
     * is initialized to 0.
     *
     * @param reason                    a description of the exception
     * @param SQLState                  an XOPEN or SQL:2003 code identifying the exception
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     *
     * @since 1.6
     */
        public SQLClientInfoException(String reason,
                                                           String SQLState,
                                                           Map<String, ClientInfoStatus> failedProperties) {

                super(reason, SQLState);
                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given  {@code reason}, {@code SQLState}, {@code cause}
     * and {@code failedProperties}.  The vendor code is initialized to 0.
     *
     * @param reason                    a description of the exception
     * @param SQLState                  an XOPEN or SQL:2003 code identifying the exception
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     * @param cause                     the underlying reason for this {@code SQLException} (which is saved for later retrieval by the {@code getCause()} method); may be null indicating
     *     the cause is non-existent or unknown.
     *
     * @since 1.6
     */
        public SQLClientInfoException(String reason,
                                                           String SQLState,
                                                           Map<String, ClientInfoStatus> failedProperties,
                                                           Throwable cause) {

                super(reason, SQLState);
                initCause(cause);
                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given  {@code reason}, {@code SQLState},
     * {@code vendorCode}  and {@code failedProperties}.
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     * @param reason                    a description of the exception
     * @param SQLState                  an XOPEN or SQL:2003 code identifying the exception
     * @param vendorCode                a database vendor-specific exception code
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     *
     * @since 1.6
     */
        public SQLClientInfoException(String reason,
                                                           String SQLState,
                                                           int vendorCode,
                                                           Map<String, ClientInfoStatus> failedProperties) {

                super(reason, SQLState, vendorCode);
                this.failedProperties = failedProperties;
        }

        /**
     * Constructs a {@code SQLClientInfoException} object initialized with a
     * given  {@code reason}, {@code SQLState},
     * {@code cause}, {@code vendorCode} and
     * {@code failedProperties}.
     *
     * @param reason                    a description of the exception
     * @param SQLState                  an XOPEN or SQL:2003 code identifying the exception
     * @param vendorCode                a database vendor-specific exception code
     * @param failedProperties          A Map containing the property values that could not
     *                                  be set.  The keys in the Map
     *                                  contain the names of the client info
     *                                  properties that could not be set and
     *                                  the values contain one of the reason codes
     *                                  defined in {@code ClientInfoStatus}
     * @param cause                     the underlying reason for this {@code SQLException} (which is saved for later retrieval by the {@code getCause()} method); may be null indicating
     *                                  the cause is non-existent or unknown.
     *
     * @since 1.6
     */
        public SQLClientInfoException(String reason,
                                                           String SQLState,
                                                           int vendorCode,
                                                           Map<String, ClientInfoStatus> failedProperties,
                                                           Throwable cause) {

                super(reason, SQLState, vendorCode);
                initCause(cause);
                this.failedProperties = failedProperties;
        }

    /**
     * Returns the list of client info properties that could not be set.  The
     * keys in the Map  contain the names of the client info
     * properties that could not be set and the values contain one of the
     * reason codes defined in {@code ClientInfoStatus}
     *
     * @return Map list containing the client info properties that could
     * not be set
     *
     * @since 1.6
     */
        public Map<String, ClientInfoStatus> getFailedProperties() {

                return this.failedProperties;
        }

    private static final long serialVersionUID = -4319604256824655880L;
}
