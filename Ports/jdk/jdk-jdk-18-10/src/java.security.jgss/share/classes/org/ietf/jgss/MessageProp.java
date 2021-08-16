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
 * This is a utility class used within the per-message GSSContext
 * methods to convey per-message properties.<p>
 *
 * When used with the GSSContext interface's wrap and getMIC methods, an
 * instance of this class is used to indicate the desired
 * Quality-of-Protection (QOP) and to request if confidentiality services
 * are to be applied to caller supplied data (wrap only).  To request
 * default QOP, the value of 0 should be used for QOP.<p>
 *
 * When used with the unwrap and verifyMIC methods of the GSSContext
 * interface, an instance of this class will be used to indicate the
 * applied QOP and confidentiality services over the supplied message.
 * In the case of verifyMIC, the confidentiality state will always be
 * <code>false</code>.  Upon return from these methods, this object will also
 * contain any supplementary status values applicable to the processed
 * token.  The supplementary status values can indicate old tokens, out
 * of sequence tokens, gap tokens or duplicate tokens.
 *
 * @see GSSContext#wrap
 * @see GSSContext#unwrap
 * @see GSSContext#getMIC
 * @see GSSContext#verifyMIC
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */
public class MessageProp {

    private boolean privacyState;
    private int qop;
    private boolean dupToken;
    private boolean oldToken;
    private boolean unseqToken;
    private boolean gapToken;
    private int minorStatus;
    private String minorString;

   /**
    * Constructor which sets the desired privacy state. The QOP value used
    * is 0.
    *
    * @param privState the privacy (i.e. confidentiality) state
    */
    public MessageProp(boolean privState) {
        this(0, privState);
    }

    /**
     * Constructor which sets the values for the qop and privacy state.
     *
     * @param qop the QOP value
     * @param privState the privacy (i.e. confidentiality) state
     */
    public MessageProp(int qop, boolean privState) {
        this.qop = qop;
        this.privacyState = privState;
        resetStatusValues();
    }

    /**
     * Retrieves the QOP value.
     *
     * @return an int representing the QOP value
     * @see #setQOP
     */
    public int getQOP() {
        return qop;
    }

    /**
     * Retrieves the privacy state.
     *
     * @return true if the privacy (i.e., confidentiality) state is true,
     * false otherwise.
     * @see #setPrivacy
     */
    public boolean getPrivacy() {

        return (privacyState);
    }

    /**
     * Sets the QOP value.
     *
     * @param qop the int value to set the QOP to
     * @see #getQOP
     */
    public void setQOP(int qop) {
        this.qop = qop;
    }


    /**
     * Sets the privacy state.
     *
     * @param privState true is the privacy (i.e., confidentiality) state
     * is true, false otherwise.
     * @see #getPrivacy
     */
    public void setPrivacy(boolean privState) {

        this.privacyState = privState;
    }


    /**
     * Tests if this is a duplicate of an earlier token.
     *
     * @return true if this is a duplicate, false otherwise.
     */
    public boolean isDuplicateToken() {
        return dupToken;
    }

    /**
     * Tests if this token's validity period has expired, i.e., the token
     * is too old to be checked for duplication.
     *
     * @return true if the token's validity period has expired, false
     * otherwise.
     */
    public boolean isOldToken() {
        return oldToken;
    }

    /**
     * Tests if a later token had already been processed.
     *
     * @return true if a later token had already been processed, false otherwise.
     */
    public boolean isUnseqToken() {
        return unseqToken;
    }

    /**
     * Tests if an expected token was not received, i.e., one or more
     * predecessor tokens have not yet been successfully processed.
     *
     * @return true if an expected per-message token was not received,
     * false otherwise.
     */
    public boolean isGapToken() {
        return gapToken;
    }

    /**
     * Retrieves the minor status code that the underlying mechanism might
     * have set for this per-message operation.
     *
     * @return the int minor status
     */
    public int getMinorStatus(){
        return minorStatus;
    }

    /**
     * Retrieves a string explaining the minor status code.
     *
     * @return a String corresponding to the minor status
     * code. <code>null</code> will be returned when no minor status code
     * has been set.
     */
    public String getMinorString(){
        return minorString;
    }

    /**
     * This method sets the state for the supplementary information flags
     * and the minor status in MessageProp.  It is not used by the
     * application but by the GSS implementation to return this information
     * to the caller of a per-message context method.
     *
     * @param duplicate true if the token was a duplicate of an earlier
     * token, false otherwise
     * @param old true if the token's validity period has expired, false
     * otherwise
     * @param unseq true if a later token has already been processed, false
     * otherwise
     * @param gap true if one or more predecessor tokens have not yet been
     * successfully processed, false otherwise
     * @param minorStatus the int minor status code for the per-message
     * operation
     * @param  minorString the textual representation of the minorStatus value
     */
   public void setSupplementaryStates(boolean duplicate,
                  boolean old, boolean unseq, boolean gap,
                  int minorStatus, String minorString) {
       this.dupToken = duplicate;
       this.oldToken = old;
       this.unseqToken = unseq;
       this.gapToken = gap;
       this.minorStatus = minorStatus;
       this.minorString = minorString;
    }

    /**
     * Resets the supplementary status values to false.
     */
    private void resetStatusValues() {
        dupToken = false;
        oldToken = false;
        unseqToken = false;
        gapToken = false;
        minorStatus = 0;
        minorString = null;
    }
}
