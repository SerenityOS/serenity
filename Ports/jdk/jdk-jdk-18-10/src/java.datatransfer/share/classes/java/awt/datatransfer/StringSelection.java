/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.StringReader;

/**
 * A {@code Transferable} which implements the capability required to transfer a
 * {@code String}.
 * <p>
 * This {@code Transferable} properly supports {@code DataFlavor.stringFlavor}
 * and all equivalent flavors. Support for {@code DataFlavor.plainTextFlavor}
 * and all equivalent flavors is <b>deprecated</b>. No other {@code DataFlavor}s
 * are supported.
 *
 * @see DataFlavor#stringFlavor
 * @see DataFlavor#plainTextFlavor
 * @since 1.1
 */
public class StringSelection implements Transferable, ClipboardOwner {

    private static final int STRING = 0;
    private static final int PLAIN_TEXT = 1;

    @SuppressWarnings("deprecation")
    private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor,
        DataFlavor.plainTextFlavor // deprecated
    };

    private String data;

    /**
     * Creates a {@code Transferable} capable of transferring the specified
     * {@code String}.
     *
     * @param  data the string to be transferred
     */
    public StringSelection(String data) {
        this.data = data;
    }

    /**
     * Returns an array of flavors in which this {@code Transferable} can
     * provide the data. {@code DataFlavor.stringFlavor} is properly supported.
     * Support for {@code DataFlavor.plainTextFlavor} is <b>deprecated</b>.
     *
     * @return an array of length two, whose elements are
     *         {@code DataFlavor.stringFlavor} and
     *         {@code DataFlavor.plainTextFlavor}
     */
    public DataFlavor[] getTransferDataFlavors() {
        // returning flavors itself would allow client code to modify
        // our internal behavior
        return flavors.clone();
    }

    /**
     * Returns whether the requested flavor is supported by this
     * {@code Transferable}.
     *
     * @param  flavor the requested flavor for the data
     * @return {@code true} if {@code flavor} is equal to
     *         {@code DataFlavor.stringFlavor} or
     *         {@code DataFlavor.plainTextFlavor}; {@code false} if
     *         {@code flavor} is not one of the above flavors
     * @throws NullPointerException if {@code flavor} is {@code null}
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        // JCK Test StringSelection0003: if 'flavor' is null, throw NPE
        for (int i = 0; i < flavors.length; i++) {
            if (flavor.equals(flavors[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@code Transferable}'s data in the requested
     * {@code DataFlavor} if possible. If the desired flavor is
     * {@code DataFlavor.stringFlavor}, or an equivalent flavor, the
     * {@code String} representing the selection is returned. If the desired
     * flavor is {@code DataFlavor.plainTextFlavor}, or an equivalent flavor, a
     * {@code Reader} is returned.
     * <br>
     * <b>Note:</b> The behavior of this method for
     * {@code DataFlavor.plainTextFlavor}
     * and equivalent {@code DataFlavor}s is inconsistent with the definition of
     * {@code DataFlavor.plainTextFlavor}.
     *
     * @param  flavor the requested flavor for the data
     * @return the data in the requested flavor, as outlined above
     * @throws UnsupportedFlavorException if the requested data flavor is not
     *         equivalent to either {@code DataFlavor.stringFlavor} or
     *         {@code DataFlavor.plainTextFlavor}
     * @throws IOException if an IOException occurs while retrieving the data.
     *         By default, StringSelection never throws this exception, but a
     *         subclass may.
     * @throws NullPointerException if {@code flavor} is {@code null}
     * @see java.io.Reader
     */
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        // JCK Test StringSelection0007: if 'flavor' is null, throw NPE
        if (flavor.equals(flavors[STRING])) {
            return (Object)data;
        } else if (flavor.equals(flavors[PLAIN_TEXT])) {
            return new StringReader(data == null ? "" : data);
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
