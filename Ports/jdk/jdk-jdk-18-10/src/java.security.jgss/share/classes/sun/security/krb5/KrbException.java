/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5;

import sun.security.krb5.internal.Krb5;
import sun.security.krb5.internal.KRBError;

public class KrbException extends Exception {

    private static final long serialVersionUID = -4993302876451928596L;

    private int returnCode;
    private KRBError error;

    public KrbException(String s) {
        super(s);
    }

    public KrbException(Throwable cause) {
        super(cause);
    }

    public KrbException(int i) {
        returnCode = i;
    }

    public KrbException(int i, String s) {
        this(s);
        returnCode = i;
    }

    public KrbException(KRBError e) {
        returnCode = e.getErrorCode();
        error = e;
    }

    public KrbException(KRBError e, String s) {
        this(s);
        returnCode = e.getErrorCode();
        error = e;
    }

    public KRBError getError() {
        return error;
    }


    public int returnCode() {
        return returnCode;
    }

    public String returnCodeSymbol() {
        return returnCodeSymbol(returnCode);
    }

    public static String returnCodeSymbol(int i) {
        return "not yet implemented";
    }

    public String returnCodeMessage() {
        return Krb5.getErrorMessage(returnCode);
    }

    public static String errorMessage(int i) {
        return Krb5.getErrorMessage(i);
    }


    public String krbErrorMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("krb_error ").append(returnCode);
        String msg =  getMessage();
        if (msg != null) {
            sb.append(" ");
            sb.append(msg);
        }
        return sb.toString();
    }

    /**
     * Returns messages like:
     * "Integrity check on decrypted field failed (31) - \
     *                         Could not decrypt service ticket"
     * If the error code is 0 then the first half is skipped.
     */
    public String getMessage() {
        StringBuilder message = new StringBuilder();
        int returnCode = returnCode();
        if (returnCode != 0) {
            message.append(returnCodeMessage());
            message.append(" (").append(returnCode()).append(')');
        }
        String consMessage = super.getMessage();
        if (consMessage != null && consMessage.length() != 0) {
            if (returnCode != 0)
                message.append(" - ");
            message.append(consMessage);
        }
        return message.toString();
    }

    public String toString() {
        return ("KrbException: " + getMessage());
    }

    @Override public int hashCode() {
        int result = 17;
        result = 37 * result + returnCode;
        if (error != null) {
            result = 37 * result + error.hashCode();
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof KrbException)) {
            return false;
        }

        KrbException other = (KrbException)obj;
        if (returnCode != other.returnCode) {
            return false;
        }
        return (error == null)?(other.error == null):
            (error.equals(other.error));
    }
}
