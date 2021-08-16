/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

import java.io.Serial;
import java.util.EventObject;

/**
 * {@code FlavorEvent} is used to notify interested parties that available
 * {@link DataFlavor}s have changed in the {@link Clipboard} (the event source).
 *
 * @author Alexander Gerasimov
 * @see FlavorListener
 * @since 1.5
 */
public class FlavorEvent extends EventObject {

    /**
     * Use serialVersionUID from JDK 1.5 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5842664112252414548L;

    /**
     * Constructs a {@code FlavorEvent} object.
     *
     * @param  source the {@code Clipboard} that is the source of the event
     * @throws IllegalArgumentException if the {@code source} is {@code null}
     */
    public FlavorEvent(Clipboard source) {
        super(source);
    }
}
