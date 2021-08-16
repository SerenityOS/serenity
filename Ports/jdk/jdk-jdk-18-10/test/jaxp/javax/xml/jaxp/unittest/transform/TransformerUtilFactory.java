/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package transform;

import transform.util.DOMUtil;
import transform.util.SAXUtil;
import transform.util.StAXUtil;
import transform.util.StreamUtil;
import transform.util.TransformerUtil;

public class TransformerUtilFactory {

    public final static String DOM = "dom";

    public final static String SAX = "sax";

    public final static String StAX = "stax";

    public final static String STREAM = "stream";

    /** Creates a new instance of TransformerUtilFactory */
    private TransformerUtilFactory() {
    }

    public static TransformerUtil getUtil(String type) throws Exception {
        if (type.equals(DOM)) {
            return DOMUtil.getInstance();
        } else if (type.equals(STREAM))
            return StreamUtil.getInstance();
        else if (type.equals(SAX))
            return SAXUtil.getInstance();
        else if (type.equals(StAX))
            return StAXUtil.getInstance();
        else
            return null;
    }
}
