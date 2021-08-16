/*
 * Copyright (c) 1994, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown to indicate that the FTP server reported an error.
 * For instance that the requested file doesn't exist or
 * that a command isn't supported.
 * <p>The specific error code can be retreived with {@link #getReplyCode() }.</p>
 * @author      Jonathan Payne
 */
public class FtpProtocolException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 5978077070276545054L;
    private final FtpReplyCode code;

    /**
     * Constructs a new {@code FtpProtocolException} from the
     * specified detail message. The reply code is set to unknow error.
     *
     * @param   detail   the detail message.
     */
    public FtpProtocolException(String detail) {
            super(detail);
            code = FtpReplyCode.UNKNOWN_ERROR;
    }

    /**
     * Constructs a new {@code FtpProtocolException} from the
     * specified response code and exception detail message
     *
     * @param   detail   the detail message.
     * @param   code The {@code FtpRelyCode} received from server.
     */
      public FtpProtocolException(String detail, FtpReplyCode code) {
        super(detail);
        this.code = code;
    }

    /**
     * Gets the reply code sent by the server that led to this exception
     * being thrown.
     *
     * @return The {@link FtpReplyCode} associated with that exception.
     */
    public FtpReplyCode getReplyCode() {
        return code;
    }
}
