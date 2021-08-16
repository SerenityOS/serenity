/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.http;

import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;

import sun.net.www.*;
import sun.security.action.GetPropertyAction;

/**
 * This class is used to parse the information in WWW-Authenticate: and Proxy-Authenticate:
 * headers. It searches among multiple header lines and within each header line
 * for the best currently supported scheme. It can also return a HeaderParser
 * containing the challenge data for that particular scheme.
 *
 * Some examples:
 *
 * WWW-Authenticate: Basic realm="foo" Digest realm="bar" NTLM
 *  Note the realm parameter must be associated with the particular scheme.
 *
 * or
 *
 * WWW-Authenticate: Basic realm="foo"
 * WWW-Authenticate: Digest realm="foo",qop="auth",nonce="thisisanunlikelynonce"
 * WWW-Authenticate: NTLM
 *
 * or
 *
 * WWW-Authenticate: Basic realm="foo"
 * WWW-Authenticate: NTLM ASKAJK9893289889QWQIOIONMNMN
 *
 * The last example shows how NTLM breaks the rules of rfc2617 for the structure of
 * the authentication header. This is the reason why the raw header field is used for ntlm.
 *
 * At present, the class chooses schemes in following order :
 *      1. Negotiate (if supported)
 *      2. Kerberos (if supported)
 *      3. Digest
 *      4. NTLM (if supported)
 *      5. Basic
 *
 * This choice can be modified by setting a system property:
 *
 *      -Dhttp.auth.preference="scheme"
 *
 * which in this case, specifies that "scheme" should be used as the auth scheme when offered
 * disregarding the default prioritisation. If scheme is not offered, or explicitly
 * disabled, by {@code disabledSchemes}, then the default priority is used.
 *
 * Attention: when http.auth.preference is set as SPNEGO or Kerberos, it's actually "Negotiate
 * with SPNEGO" or "Negotiate with Kerberos", which means the user will prefer the Negotiate
 * scheme with GSS/SPNEGO or GSS/Kerberos mechanism.
 *
 * This also means that the real "Kerberos" scheme can never be set as a preference.
 */

public class AuthenticationHeader {

    MessageHeader rsp; // the response to be parsed
    HeaderParser preferred;
    String preferred_r; // raw Strings
    private final HttpCallerInfo hci;   // un-schemed, need check

    // When set true, do not use Negotiate even if the response
    // headers suggest so.
    boolean dontUseNegotiate = false;
    static String authPref=null;

    public String toString() {
        return "AuthenticationHeader: prefer " + preferred_r;
    }

    static {
        authPref = GetPropertyAction.privilegedGetProperty("http.auth.preference");

        // http.auth.preference can be set to SPNEGO or Kerberos.
        // In fact they means "Negotiate with SPNEGO" and "Negotiate with
        // Kerberos" separately, so here they are all translated into
        // Negotiate. Read NegotiateAuthentication.java to see how they
        // were used later.

        if (authPref != null) {
            authPref = authPref.toLowerCase();
            if(authPref.equals("spnego") || authPref.equals("kerberos")) {
                authPref = "negotiate";
            }
        }
    }

    String hdrname; // Name of the header to look for

    /**
     * Parses a set of authentication headers and chooses the preferred scheme
     * that is supported for a given host.
     */
    public AuthenticationHeader (String hdrname, MessageHeader response,
            HttpCallerInfo hci, boolean dontUseNegotiate) {
        this(hdrname, response, hci, dontUseNegotiate, Collections.emptySet());
    }

    /**
     * Parses a set of authentication headers and chooses the preferred scheme
     * that is supported for a given host.
     *
     * <p> The {@code disabledSchemes} parameter is a, possibly empty, set of
     * authentication schemes that are disabled.
     */
    public AuthenticationHeader(String hdrname,
                                MessageHeader response,
                                HttpCallerInfo hci,
                                boolean dontUseNegotiate,
                                Set<String> disabledSchemes) {
        this.hci = hci;
        this.dontUseNegotiate = dontUseNegotiate;
        this.rsp = response;
        this.hdrname = hdrname;
        this.schemes = new HashMap<>();
        parse(disabledSchemes);
    }

    public HttpCallerInfo getHttpCallerInfo() {
        return hci;
    }
    /* we build up a map of scheme names mapped to SchemeMapValue objects */
    static class SchemeMapValue {
        SchemeMapValue (HeaderParser h, String r) {raw=r; parser=h;}
        String raw;
        HeaderParser parser;
    }

    HashMap<String, SchemeMapValue> schemes;

    /* Iterate through each header line, and then within each line.
     * If multiple entries exist for a particular scheme (unlikely)
     * then the last one will be used. The
     * preferred scheme that we support will be used.
     */
    private void parse(Set<String> disabledSchemes) {
        Iterator<String> iter = rsp.multiValueIterator(hdrname);
        while (iter.hasNext()) {
            String raw = iter.next();
            // HeaderParser lower cases everything, so can be used case-insensitively
            HeaderParser hp = new HeaderParser(raw);
            Iterator<String> keys = hp.keys();
            int i, lastSchemeIndex;
            for (i=0, lastSchemeIndex = -1; keys.hasNext(); i++) {
                keys.next();
                if (hp.findValue(i) == null) { /* found a scheme name */
                    if (lastSchemeIndex != -1) {
                        HeaderParser hpn = hp.subsequence (lastSchemeIndex, i);
                        String scheme = hpn.findKey(0);
                        if (!disabledSchemes.contains(scheme))
                            schemes.put(scheme, new SchemeMapValue (hpn, raw));
                    }
                    lastSchemeIndex = i;
                }
            }
            if (i > lastSchemeIndex) {
                HeaderParser hpn = hp.subsequence (lastSchemeIndex, i);
                String scheme = hpn.findKey(0);
                if (!disabledSchemes.contains(scheme))
                    schemes.put(scheme, new SchemeMapValue (hpn, raw));
            }
        }

        /* choose the best of them, the order is
         * negotiate -> kerberos -> digest -> ntlm -> basic
         */
        SchemeMapValue v = null;
        if (authPref == null || (v=schemes.get (authPref)) == null) {

            if(v == null && !dontUseNegotiate) {
                SchemeMapValue tmp = schemes.get("negotiate");
                if(tmp != null) {
                    if(hci == null || !NegotiateAuthentication.isSupported(new HttpCallerInfo(hci, "Negotiate"))) {
                        tmp = null;
                    }
                    v = tmp;
                }
            }

            if(v == null && !dontUseNegotiate) {
                SchemeMapValue tmp = schemes.get("kerberos");
                if(tmp != null) {
                    // the Kerberos scheme is only observed in MS ISA Server. In
                    // fact i think it's a Kerberos-mechnism-only Negotiate.
                    // Since the Kerberos scheme is always accompanied with the
                    // Negotiate scheme, so it seems impossible to reach this
                    // line. Even if the user explicitly set http.auth.preference
                    // as Kerberos, it means Negotiate with Kerberos, and the code
                    // will still tried to use Negotiate at first.
                    //
                    // The only chance this line get executed is that the server
                    // only suggest the Kerberos scheme.
                    if(hci == null || !NegotiateAuthentication.isSupported(new HttpCallerInfo(hci, "Kerberos"))) {
                        tmp = null;
                    }
                    v = tmp;
                }
            }

            if(v == null) {
                if ((v=schemes.get ("digest")) == null) {
                    if (!NTLMAuthenticationProxy.supported
                        || ((v=schemes.get("ntlm"))==null)) {
                        v = schemes.get ("basic");
                    }
                }
            }
        } else {    // authPref != null && it's found in reponses'
            if (dontUseNegotiate && authPref.equals("negotiate")) {
                v = null;
            }
        }

        if (v != null) {
            preferred = v.parser;
            preferred_r = v.raw;
        }
    }

    /**
     * return a header parser containing the preferred authentication scheme (only).
     * The preferred scheme is the strongest of the schemes proposed by the server.
     * The returned HeaderParser will contain the relevant parameters for that scheme
     */
    public HeaderParser headerParser() {
        return preferred;
    }

    /**
     * return the name of the preferred scheme
     */
    public String scheme() {
        if (preferred != null) {
            return preferred.findKey(0);
        } else {
            return null;
        }
    }

    /* return the raw header field for the preferred/chosen scheme */

    public String raw () {
        return preferred_r;
    }

    /**
     * returns true is the header exists and contains a recognised scheme
     */
    public boolean isPresent () {
        return preferred != null;
    }
}
