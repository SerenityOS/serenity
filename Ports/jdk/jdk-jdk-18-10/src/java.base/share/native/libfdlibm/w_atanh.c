
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
/*
 * wrapper atanh(x)
 */

#include "fdlibm.h"


#ifdef __STDC__
        double atanh(double x)          /* wrapper atanh */
#else
        double atanh(x)                 /* wrapper atanh */
        double x;
#endif
{
#ifdef _IEEE_LIBM
        return __ieee754_atanh(x);
#else
        double z,y;
        z = __ieee754_atanh(x);
        if(_LIB_VERSION == _IEEE_ || isnan(x)) return z;
        y = fabs(x);
        if(y>=1.0) {
            if(y>1.0)
                return __kernel_standard(x,x,30); /* atanh(|x|>1) */
            else
                return __kernel_standard(x,x,31); /* atanh(|x|==1) */
        } else
            return z;
#endif
}
