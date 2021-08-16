/*
 * Copyright (c) 1998, 2001, Oracle and/or its affiliates. All rights reserved.
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

/* ilogb(double x)
 * return the binary exponent of non-zero x
 * ilogb(0) = 0x80000001
 * ilogb(inf/NaN) = 0x7fffffff (no signal is raised)
 */

#include "fdlibm.h"

#ifdef __STDC__
        int ilogb(double x)
#else
        int ilogb(x)
        double x;
#endif
{
        int hx,lx,ix;

        hx  = (__HI(x))&0x7fffffff;     /* high word of x */
        if(hx<0x00100000) {
            lx = __LO(x);
            if((hx|lx)==0)
                return 0x80000001;      /* ilogb(0) = 0x80000001 */
            else                        /* subnormal x */
                if(hx==0) {
                    for (ix = -1043; lx>0; lx<<=1) ix -=1;
                } else {
                    for (ix = -1022,hx<<=11; hx>0; hx<<=1) ix -=1;
                }
            return ix;
        }
        else if (hx<0x7ff00000) return (hx>>20)-1023;
        else return 0x7fffffff;
}
