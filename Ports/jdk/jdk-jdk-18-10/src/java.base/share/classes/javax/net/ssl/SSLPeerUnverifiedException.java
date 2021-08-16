/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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


package javax.net.ssl;


/**
 * Indicates that the peer's identity has not been verified.
 * <P>
 * When the peer was not able to
 * identify itself (for example; no certificate, the particular
 * cipher suite being used does not support authentication, or no
 * peer authentication was established during SSL handshaking) this
 * exception is thrown.
 *
 * @since 1.4
 * @author David Brownell
 */
public
class SSLPeerUnverifiedException extends SSLException
{
    @java.io.Serial
    private static final long serialVersionUID = -8919512675000600547L;

    /**
     * Constructs an exception reporting that the SSL peer's
     * identity has not been verified.
     *
     * @param reason describes the problem.
     */
    public SSLPeerUnverifiedException(String reason)
    {
        super(reason);
    }
}
