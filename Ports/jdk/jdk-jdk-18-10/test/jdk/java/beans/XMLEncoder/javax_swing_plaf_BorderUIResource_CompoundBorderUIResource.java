/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6402062 6487891
 * @summary Tests CompoundBorderUIResource encoding
 * @run main/othervm -Djava.security.manager=allow javax_swing_plaf_BorderUIResource_CompoundBorderUIResource
 * @author Sergey Malenkov
 */

import javax.swing.border.CompoundBorder;
import javax.swing.plaf.BorderUIResource.CompoundBorderUIResource;

public final class javax_swing_plaf_BorderUIResource_CompoundBorderUIResource extends AbstractTest<CompoundBorderUIResource> {
    public static void main(String[] args) {
        new javax_swing_plaf_BorderUIResource_CompoundBorderUIResource().test(true);
    }

    protected CompoundBorderUIResource getObject() {
        return new CompoundBorderUIResource(null, new CompoundBorderUIResource(null, null));
    }

    protected CompoundBorderUIResource getAnotherObject() {
        return null; // TODO: could not update property
        // return new CompoundBorderUIResource(null, null);
    }
}
