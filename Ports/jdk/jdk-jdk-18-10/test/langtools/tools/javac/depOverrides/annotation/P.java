/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

// combinations of methods defined in a base class
// and overridden in subtypes

// class should compile with no warnings

class P {
    @Deprecated public void pDep_qDep_rDep() { }
    @Deprecated public void pDep_qDep_rUnd() { }
    @Deprecated public void pDep_qDep_rInh() { }
    @Deprecated public void pDep_qUnd_rDep() { }
    @Deprecated public void pDep_qUnd_rUnd() { }
    @Deprecated public void pDep_qUnd_rInh() { }
    @Deprecated public void pDep_qInh_rDep() { }
    @Deprecated public void pDep_qInh_rUnd() { }
    @Deprecated public void pDep_qInh_rInh() { }
                public void pUnd_qDep_rDep() { }
                public void pUnd_qDep_rUnd() { }
                public void pUnd_qDep_rInh() { }
                public void pUnd_qUnd_rDep() { }
                public void pUnd_qUnd_rUnd() { }
                public void pUnd_qUnd_rInh() { }
                public void pUnd_qInh_rDep() { }
                public void pUnd_qInh_rUnd() { }
                public void pUnd_qInh_rInh() { }
}
