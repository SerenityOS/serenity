/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
package sun.net.ftp;

/**
 * This class describes a FTP protocol reply code and associates a meaning
 * to the numerical value according to the various RFCs (RFC 959 in
 * particular).
 *
 */
public enum FtpReplyCode {

    RESTART_MARKER(110),
    SERVICE_READY_IN(120),
    DATA_CONNECTION_ALREADY_OPEN(125),
    FILE_STATUS_OK(150),
    COMMAND_OK(200),
    NOT_IMPLEMENTED(202),
    SYSTEM_STATUS(211),
    DIRECTORY_STATUS(212),
    FILE_STATUS(213),
    HELP_MESSAGE(214),
    NAME_SYSTEM_TYPE(215),
    SERVICE_READY(220),
    SERVICE_CLOSING(221),
    DATA_CONNECTION_OPEN(225),
    CLOSING_DATA_CONNECTION(226),
    ENTERING_PASSIVE_MODE(227),
    ENTERING_EXT_PASSIVE_MODE(229),
    LOGGED_IN(230),
    SECURELY_LOGGED_IN(232),
    SECURITY_EXCHANGE_OK(234),
    SECURITY_EXCHANGE_COMPLETE(235),
    FILE_ACTION_OK(250),
    PATHNAME_CREATED(257),
    NEED_PASSWORD(331),
    NEED_ACCOUNT(332),
    NEED_ADAT(334),
    NEED_MORE_ADAT(335),
    FILE_ACTION_PENDING(350),
    SERVICE_NOT_AVAILABLE(421),
    CANT_OPEN_DATA_CONNECTION(425),
    CONNECTION_CLOSED(426),
    NEED_SECURITY_RESOURCE(431),
    FILE_ACTION_NOT_TAKEN(450),
    ACTION_ABORTED(451),
    INSUFFICIENT_STORAGE(452),
    COMMAND_UNRECOGNIZED(500),
    INVALID_PARAMETER(501),
    BAD_SEQUENCE(503),
    NOT_IMPLEMENTED_FOR_PARAMETER(504),
    NOT_LOGGED_IN(530),
    NEED_ACCOUNT_FOR_STORING(532),
    PROT_LEVEL_DENIED(533),
    REQUEST_DENIED(534),
    FAILED_SECURITY_CHECK(535),
    UNSUPPORTED_PROT_LEVEL(536),
    PROT_LEVEL_NOT_SUPPORTED_BY_SECURITY(537),
    FILE_UNAVAILABLE(550),
    PAGE_TYPE_UNKNOWN(551),
    EXCEEDED_STORAGE(552),
    FILE_NAME_NOT_ALLOWED(553),
    PROTECTED_REPLY(631),
    UNKNOWN_ERROR(999);
    private final int value;

    FtpReplyCode(int val) {
        this.value = val;
    }

    /**
     * Returns the numerical value of the code.
     *
     * @return the numerical value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Determines if the code is a Positive Preliminary response.
     * This means beginning with a 1 (which means a value between 100 and 199)
     *
     * @return <code>true</code> if the reply code is a positive preliminary
     *         response.
     */
    public boolean isPositivePreliminary() {
        return value >= 100 && value < 200;
    }

    /**
     * Determines if the code is a Positive Completion response.
     * This means beginning with a 2 (which means a value between 200 and 299)
     *
     * @return <code>true</code> if the reply code is a positive completion
     *         response.
     */
    public boolean isPositiveCompletion() {
        return value >= 200 && value < 300;
    }

    /**
     * Determines if the code is a positive internediate response.
     * This means beginning with a 3 (which means a value between 300 and 399)
     *
     * @return <code>true</code> if the reply code is a positive intermediate
     *         response.
     */
    public boolean isPositiveIntermediate() {
        return value >= 300 && value < 400;
    }

    /**
     * Determines if the code is a transient negative response.
     * This means beginning with a 4 (which means a value between 400 and 499)
     *
     * @return <code>true</code> if the reply code is a transient negative
     *         response.
     */
    public boolean isTransientNegative() {
        return value >= 400 && value < 500;
    }

    /**
     * Determines if the code is a permanent negative response.
     * This means beginning with a 5 (which means a value between 500 and 599)
     *
     * @return <code>true</code> if the reply code is a permanent negative
     *         response.
     */
    public boolean isPermanentNegative() {
        return value >= 500 && value < 600;
    }

    /**
     * Determines if the code is a protected reply response.
     * This means beginning with a 6 (which means a value between 600 and 699)
     *
     * @return <code>true</code> if the reply code is a protected reply
     *         response.
     */
    public boolean isProtectedReply() {
        return value >= 600 && value < 700;
    }

    /**
     * Determines if the code is a syntax related response.
     * This means the second digit is a 0.
     *
     * @return <code>true</code> if the reply code is a syntax related
     *         response.
     */
    public boolean isSyntax() {
        return ((value / 10) - ((value / 100) * 10)) == 0;
    }

    /**
     * Determines if the code is an information related response.
     * This means the second digit is a 1.
     *
     * @return <code>true</code> if the reply code is an information related
     *         response.
     */
    public boolean isInformation() {
        return ((value / 10) - ((value / 100) * 10)) == 1;
    }

    /**
     * Determines if the code is a connection related response.
     * This means the second digit is a 2.
     *
     * @return <code>true</code> if the reply code is a connection related
     *         response.
     */
    public boolean isConnection() {
        return ((value / 10) - ((value / 100) * 10)) == 2;
    }

    /**
     * Determines if the code is an authentication related response.
     * This means the second digit is a 3.
     *
     * @return <code>true</code> if the reply code is an authentication related
     *         response.
     */
    public boolean isAuthentication() {
        return ((value / 10) - ((value / 100) * 10)) == 3;
    }

    /**
     * Determines if the code is an unspecified type of response.
     * This means the second digit is a 4.
     *
     * @return <code>true</code> if the reply code is an unspecified type of
     *         response.
     */
    public boolean isUnspecified() {
        return ((value / 10) - ((value / 100) * 10)) == 4;
    }

    /**
     * Determines if the code is a file system related response.
     * This means the second digit is a 5.
     *
     * @return <code>true</code> if the reply code is a file system related
     *         response.
     */
    public boolean isFileSystem() {
        return ((value / 10) - ((value / 100) * 10)) == 5;
    }

    /**
     * Static utility method to convert a value into a FtpReplyCode.
     *
     * @param v the value to convert
     * @return the <code>FtpReplyCode</code> associated with the value.
     */
    public static FtpReplyCode find(int v) {
        for (FtpReplyCode code : FtpReplyCode.values()) {
            if (code.getValue() == v) {
                return code;
            }
        }
        return UNKNOWN_ERROR;
    }
}
