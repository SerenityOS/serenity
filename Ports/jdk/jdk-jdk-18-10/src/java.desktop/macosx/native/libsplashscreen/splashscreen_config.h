/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* platform-dependent definitions */

#ifndef SPLASHSCREEN_CONFIG_H
#define SPLASHSCREEN_CONFIG_H

#include <sys/types.h>
#include <sys/unistd.h>
#include <signal.h>
#include <inttypes.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <pthread.h>
#include <Cocoa/Cocoa.h>

typedef uint32_t rgbquad_t;
typedef uint16_t word_t;
typedef uint8_t byte_t;


// Actually the following Rect machinery is unused since we don't use shapes
typedef int RECT_T;

#define RECT_EQ_X(r1,r2)        ((r1) == (r2))
#define RECT_SET(r,xx,yy,ww,hh) ;
#define RECT_INC_HEIGHT(r)      ;

#define SPLASHCTL_QUIT          'Q'
#define SPLASHCTL_UPDATE        'U'
#define SPLASHCTL_RECONFIGURE   'R'

#define INLINE static

#endif
