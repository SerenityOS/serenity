/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto;

/**
 * This exception is thrown when a {@link Cipher} operating in
 * an AEAD mode (such as GCM/CCM) is unable to verify the supplied
 * authentication tag.
 *
 * @since 1.7
 */
public class AEADBadTagException extends BadPaddingException {

    @java.io.Serial
    private static final long serialVersionUID = -488059093241685509L;

    /**
     * Constructs a AEADBadTagException with no detail message.
     */
    public AEADBadTagException() {
        super();
    }

    /**
     * Constructs a AEADBadTagException with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public AEADBadTagException(String msg) {
        super(msg);
    }
}
