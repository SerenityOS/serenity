
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
 * wrapper scalb(double x, double fn) is provide for
 * passing various standard test suite. One
 * should use scalbn() instead.
 */

#include "fdlibm.h"

#include <errno.h>

#ifdef __STDC__
#ifdef _SCALB_INT
        double scalb(double x, int fn)          /* wrapper scalb */
#else
        double scalb(double x, double fn)       /* wrapper scalb */
#endif
#else
        double scalb(x,fn)                      /* wrapper scalb */
#ifdef _SCALB_INT
        double x; int fn;
#else
        double x,fn;
#endif
#endif
{
#ifdef _IEEE_LIBM
        return __ieee754_scalb(x,fn);
#else
        double z;
        z = __ieee754_scalb(x,fn);
        if(_LIB_VERSION == _IEEE_) return z;
        if(!(finite(z)||isnan(z))&&finite(x)) {
            return __kernel_standard(x,(double)fn,32); /* scalb overflow */
        }
        if(z==0.0&&z!=x) {
            return __kernel_standard(x,(double)fn,33); /* scalb underflow */
        }
#ifndef _SCALB_INT
        if(!finite(fn)) errno = ERANGE;
#endif
        return z;
#endif
}
