/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

package org.ietf.jgss;

/**
 * This exception is thrown whenever a GSS-API error occurs, including
 * any mechanism specific error.  It may contain both the major and the
 * minor GSS-API status codes.  Major error codes are those defined at the
 * GSS-API level in this class. Minor error codes are mechanism specific
 * error codes that can provide additional information. The underlying
 * mechanism implementation is responsible for setting appropriate minor
 * status codes when throwing this exception.  Aside from delivering the
 * numeric error codes to the caller, this class performs the mapping from
 * their numeric values to textual representations.
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */
public class GSSException extends Exception {

    private static final long serialVersionUID = -2706218945227726672L;

    /**
     * Channel bindings mismatch.
     */
    public static final int BAD_BINDINGS = 1; //start with 1

    /**
     * Unsupported mechanism requested.
     */
    public static final int BAD_MECH = 2;

    /**
     * Invalid name provided.
     */
    public static final int BAD_NAME = 3;

    /**
     * Name of unsupported type provided.
     */
    public static final int BAD_NAMETYPE = 4;

    /**
     * Invalid status code.
     */
    /*
     * This is meant to be thrown by display_status which displays
     * major/minor status when an incorrect status type is passed in to it!
     */
    public static final int BAD_STATUS = 5;

    /**
     * Token had invalid integrity check.
     */
    public static final int BAD_MIC = 6;

    /**
     * Security context expired.
     */
    public static final int CONTEXT_EXPIRED = 7;

    /**
     * Expired credentials.
     */
    public static final int CREDENTIALS_EXPIRED  = 8;

    /**
     * Defective credentials.
     *
     */
    public static final int DEFECTIVE_CREDENTIAL = 9;

    /**
     * Defective token.
     *
     */
    public static final int DEFECTIVE_TOKEN = 10;

    /**
     * General failure, unspecified at GSS-API level.
     */
    public static final int FAILURE = 11;

    /**
     * Invalid security context.
     */
    public static final int NO_CONTEXT = 12;

    /**
     * Invalid credentials.
     */
    public static final int NO_CRED = 13;

    /**
     * Unsupported QOP value.
     */
    public static final int BAD_QOP = 14;

    /**
     * Operation unauthorized.
     */
    public static final int UNAUTHORIZED = 15;

    /**
     * Operation unavailable.
     */
    public static final int UNAVAILABLE = 16;

    /**
     * Duplicate credential element requested.
     */
    public static final int DUPLICATE_ELEMENT = 17;

    /**
     * Name contains multi-mechanism elements.
     */
    public static final int NAME_NOT_MN = 18;

    /**
     * The token was a duplicate of an earlier token.
     * This is a fatal error code that may occur during
     * context establishment.  It is not used to indicate
     * supplementary status values. The MessageProp object is
     * used for that purpose.
     */
    public static final int DUPLICATE_TOKEN = 19;

    /**
     * The token's validity period has expired.  This is a
     * fatal error code that may occur during context establishment.
     * It is not used to indicate supplementary status values.
     * The MessageProp object is used for that purpose.
     */
    public static final int OLD_TOKEN = 20;


    /**
     * A later token has already been processed.  This is a
     * fatal error code that may occur during context establishment.
     * It is not used to indicate supplementary status values.
     * The MessageProp object is used for that purpose.
     */
    public static final int UNSEQ_TOKEN = 21;


    /**
     * An expected per-message token was not received.  This is a
     * fatal error code that may occur during context establishment.
     * It is not used to indicate supplementary status values.
     * The MessageProp object is used for that purpose.
     */
    public static final int GAP_TOKEN = 22;


    private static String[] messages = {
        "Channel binding mismatch", // BAD_BINDINGS
        "Unsupported mechanism requested", // BAD_MECH
        "Invalid name provided", // BAD_NAME
        "Name of unsupported type provided", //BAD_NAMETYPE
        "Invalid input status selector", // BAD_STATUS
        "Token had invalid integrity check", // BAD_SIG
        "Specified security context expired", // CONTEXT_EXPIRED
        "Expired credentials detected", // CREDENTIALS_EXPIRED
        "Defective credential detected", // DEFECTIVE_CREDENTIAL
        "Defective token detected", // DEFECTIVE_TOKEN
        "Failure unspecified at GSS-API level", // FAILURE
        "Security context init/accept not yet called or context deleted",
                                                // NO_CONTEXT
        "No valid credentials provided", // NO_CRED
        "Unsupported QOP value", // BAD_QOP
        "Operation unauthorized", // UNAUTHORIZED
        "Operation unavailable", // UNAVAILABLE
        "Duplicate credential element requested", //DUPLICATE_ELEMENT
        "Name contains multi-mechanism elements", // NAME_NOT_MN
        "The token was a duplicate of an earlier token", //DUPLICATE_TOKEN
        "The token's validity period has expired", //OLD_TOKEN
        "A later token has already been processed", //UNSEQ_TOKEN
        "An expected per-message token was not received", //GAP_TOKEN
    };

   /**
    * The major code for this exception
    *
    * @serial
    */
    private int major;

   /**
    * The minor code for this exception
    *
    * @serial
    */
    private int minor = 0;

   /**
    * The text string for minor code
    *
    * @serial
    */
    private String minorMessage = null;

   /**
    * Alternate text string for major code
    *
    * @serial
    */

    private String majorString = null;

    /**
     *  Creates a GSSException object with a specified major code.
     *
     * @param majorCode the The GSS error code for the problem causing this
     * exception to be thrown.
     */
    public GSSException (int majorCode) {

        if (validateMajor(majorCode))
            major = majorCode;
        else
            major = FAILURE;
    }

    /**
     * Construct a GSSException object with a specified major code and a
     * specific major string for it.
     *
     * @param majorCode the fatal error code causing this exception.
     * @param majorString an expicit message to be included in this exception
     */
    GSSException (int majorCode, String majorString) {

        if (validateMajor(majorCode))
            major = majorCode;
        else
            major = FAILURE;
        this.majorString = majorString;
    }


    /**
     * Creates a GSSException object with the specified major code, minor
     * code, and minor code textual explanation.  This constructor is to be
     * used when the exception is originating from the underlying mechanism
     * level. It allows the setting of both the GSS code and the mechanism
     * code.
     *
     * @param majorCode the GSS error code for the problem causing this
     * exception to be thrown.
     * @param minorCode the mechanism level error code for the problem
     * causing this exception to be thrown.
     * @param minorString the textual explanation of the mechanism error
     * code.
     */
    public GSSException (int majorCode, int minorCode, String minorString) {

        if (validateMajor(majorCode))
            major = majorCode;
        else
            major = FAILURE;

        minor = minorCode;
        minorMessage = minorString;
    }

    /**
     * Returns the GSS-API level major error code for the problem causing
     * this exception to be thrown. Major error codes are
     * defined at the mechanism independent GSS-API level in this
     * class. Mechanism specific error codes that might provide more
     * information are set as the minor error code.
     *
     * @return int the GSS-API level major error code causing this exception
     * @see #getMajorString
     * @see #getMinor
     * @see #getMinorString
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the mechanism level error code for the problem causing this
     * exception to be thrown. The minor code is set by the underlying
     * mechanism.
     *
     * @return int the mechanism error code; 0 indicates that it has not
     * been set.
     * @see #getMinorString
     * @see #setMinor
     */
    public int  getMinor(){
        return minor;
    }

    /**
     * Returns a string explaining the GSS-API level major error code in
     * this exception.
     *
     * @return String explanation string for the major error code
     * @see #getMajor
     * @see #toString
     */
    public String getMajorString() {

        if (majorString != null)
            return majorString;
        else
            return messages[major - 1];
    }


    /**
     * Returns a string explaining the mechanism specific error code.
     * If the minor status code is 0, then no mechanism level error details
     * will be available.
     *
     * @return String a textual explanation of mechanism error code
     * @see #getMinor
     * @see #getMajorString
     * @see #toString
     */
    public String getMinorString() {

        return minorMessage;
    }


    /**
     * Used by the exception thrower to set the mechanism
     * level minor error code and its string explanation.  This is used by
     * mechanism providers to indicate error details.
     *
     * @param minorCode the mechanism specific error code
     * @param message textual explanation of the mechanism error code
     * @see #getMinor
     */
    public void setMinor(int minorCode, String message) {

        minor = minorCode;
        minorMessage = message;
    }


    /**
     * Returns a textual representation of both the major and the minor
     * status codes.
     *
     * @return a String with the error descriptions
     */
    public String toString() {
        return ("GSSException: " + getMessage());
    }

    /**
     * Returns a textual representation of both the major and the minor
     * status codes.
     *
     * @return a String with the error descriptions
     */
    public String getMessage() {
        if (minor == 0)
            return (getMajorString());

        return (getMajorString()
                + " (Mechanism level: " + getMinorString() + ")");
    }


    /*
     * Validates the major code in the proper range.
     */
    private boolean validateMajor(int major) {

        if (major > 0 && major <= messages.length)
            return (true);

        return (false);
    }
}
