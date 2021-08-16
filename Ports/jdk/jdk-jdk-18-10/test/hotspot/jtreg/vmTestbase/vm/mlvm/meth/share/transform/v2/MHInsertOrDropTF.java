/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.meth.share.transform.v2;

import vm.mlvm.meth.share.Argument;

public abstract class MHInsertOrDropTF extends MHBasicUnaryTF {

    protected final int pos;
    protected final Argument[] values;

    protected MHInsertOrDropTF(MHCall target, int pos, Argument[] values) {
        super(target);
        this.pos  = pos;
        this.values = values;
    }

    @Override
    protected String getDescription() {
        StringBuilder sb = new StringBuilder();
        int p = this.pos;
        for ( Argument a : this.values ) {
            sb.append("[").append(p).append("] ").append(a).append(", ");
            ++p;
        }
        if ( sb.length() > 2 )
            sb.setLength(sb.length() - 2);
        return sb.toString();
    }
}
