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

// combinations of methods defined in an interface
// and overridden in subtypes

// class overrides deprecated mthods as shown, but should not give warnings by
// virtue of being deprecated itself

@Deprecated
abstract class B3 extends A implements I {
    @Deprecated public void iDep_aDep_bDep() { }
                public void iDep_aDep_bUnd() { } // potential warning x 2, suppressed
    //          public void iDep_aDep_bInh() { }
    @Deprecated public void iDep_aUnd_bDep() { }
                public void iDep_aUnd_bUnd() { } // potential warning, suppressed
    //          public void iDep_aUnd_bInh() { } // potential warning, suppressed
    @Deprecated public void iDep_aInh_bDep() { }
                public void iDep_aInh_bUnd() { } // potential warning, suppressed
    //          public void iDep_aInh_bInh() { }
    @Deprecated public void iUnd_aDep_bDep() { }
                public void iUnd_aDep_bUnd() { } // potential warning, suppressed
    //          public void iUnd_aDep_bInh() { }
    @Deprecated public void iUnd_aUnd_bDep() { }
                public void iUnd_aUnd_bUnd() { }
    //          public void iUnd_aUnd_bInh() { }
    @Deprecated public void iUnd_aInh_bDep() { }
                public void iUnd_aInh_bUnd() { }
    //          public void iUnd_aInh_bInh() { }
}
