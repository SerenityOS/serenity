/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef MTLComposite_h_Included
#define MTLComposite_h_Included

#import <Metal/Metal.h>

#include <jni.h>

#define FLT_EPS (0.001f)
#define FLT_LT(x,y) ((x) < (y) - FLT_EPS)
#define FLT_GE(x,y) ((x) >= (y) - FLT_EPS)
#define FLT_LE(x,y) ((x) <= (y) + FLT_EPS)
#define FLT_GT(x,y) ((x) > (y) + FLT_EPS)

/**
 * The MTLComposite class represents composite mode
 * */

@interface MTLComposite : NSObject
- (id)init;
- (BOOL)isEqual:(MTLComposite *)other; // used to compare requested with cached
- (void)copyFrom:(MTLComposite *)other; // used to save cached

- (void)setRule:(jint)rule; // sets extraAlpha=1
- (void)setRule:(jint)rule extraAlpha:(jfloat)extraAlpha;
- (void)reset;

- (void)setXORComposite:(jint)color;
- (void)setAlphaComposite:(jint)rule;


- (jint)getCompositeState;
- (jint)getRule;
- (jint)getXorColor;
- (jfloat)getExtraAlpha;

- (NSString *)getDescription; // creates autorelease string
@end

#endif // MTLComposite_h_Included
