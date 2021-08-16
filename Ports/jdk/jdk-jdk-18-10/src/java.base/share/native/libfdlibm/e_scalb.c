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
 * __ieee754_scalb(x, fn) is provide for
 * passing various standard test suite. One
 * should use scalbn() instead.
 */

#include "fdlibm.h"

#ifdef _SCALB_INT
#ifdef __STDC__
        double __ieee754_scalb(double x, int fn)
#else
        double __ieee754_scalb(x,fn)
        double x; int fn;
#endif
#else
#ifdef __STDC__
        double __ieee754_scalb(double x, double fn)
#else
        double __ieee754_scalb(x,fn)
        double x, fn;
#endif
#endif
{
#ifdef _SCALB_INT
        return scalbn(x,fn);
#else
        if (isnan(x)||isnan(fn)) return x*fn;
        if (!finite(fn)) {
            if(fn>0.0) return x*fn;
            else       return x/(-fn);
        }
        if (rint(fn)!=fn) return (fn-fn)/(fn-fn);
        if ( fn > 65000.0) return scalbn(x, 65000);
        if (-fn > 65000.0) return scalbn(x,-65000);
        return scalbn(x,(int)fn);
#endif
}
