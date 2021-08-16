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

#include "MTLComposite.h"
#include "sun_java2d_SunGraphics2D.h"
#include "java_awt_AlphaComposite.h"

@implementation MTLComposite {
    jint _compState;
    jint _compositeRule;
    jint _xorPixel;
    jfloat _extraAlpha;
}

- (id)init {
    self = [super init];
    if (self) {
        _compositeRule = -1;
        _compState = -1;
        _xorPixel = 0;
        _extraAlpha = 1;
    }
    return self;
}

- (BOOL)isEqual:(MTLComposite *)other {
    if (self == other)
        return YES;

    if (_compState == other->_compState) {
        if (_compState == sun_java2d_SunGraphics2D_COMP_XOR) {
            return _xorPixel == other->_xorPixel;
        }

        if (_compState == sun_java2d_SunGraphics2D_COMP_ALPHA) {
            return _extraAlpha == other->_extraAlpha
                   && _compositeRule == other->_compositeRule;
        }
    }

    return NO;
}

- (void)copyFrom:(MTLComposite *)other {
    _extraAlpha = other->_extraAlpha;
    _compositeRule = other->_compositeRule;
    _compState = other->_compState;
    _xorPixel = other->_xorPixel;
}

- (void)setRule:(jint)rule {
    _extraAlpha = 1.f;
    _compositeRule = rule;
}

- (void)setRule:(jint)rule extraAlpha:(jfloat)extraAlpha {
    _compState = sun_java2d_SunGraphics2D_COMP_ALPHA;
    _extraAlpha = extraAlpha;
    _compositeRule = rule;
}

- (void)reset {
    _compState = sun_java2d_SunGraphics2D_COMP_ISCOPY;
    _compositeRule = java_awt_AlphaComposite_SRC;
    _extraAlpha = 1.f;
}

- (jint)getRule {
    return _compositeRule;
}

- (NSString *)getDescription {
    const char * result = "";
    switch (_compositeRule) {
        case java_awt_AlphaComposite_CLEAR:
        {
            result = "CLEAR";
        }
            break;
        case java_awt_AlphaComposite_SRC:
        {
            result = "SRC";
        }
            break;
        case java_awt_AlphaComposite_DST:
        {
            result = "DST";
        }
            break;
        case java_awt_AlphaComposite_SRC_OVER:
        {
            result = "SRC_OVER";
        }
            break;
        case java_awt_AlphaComposite_DST_OVER:
        {
            result = "DST_OVER";
        }
            break;
        case java_awt_AlphaComposite_SRC_IN:
        {
            result = "SRC_IN";
        }
            break;
        case java_awt_AlphaComposite_DST_IN:
        {
            result = "DST_IN";
        }
            break;
        case java_awt_AlphaComposite_SRC_OUT:
        {
            result = "SRC_OUT";
        }
            break;
        case java_awt_AlphaComposite_DST_OUT:
        {
            result = "DST_OUT";
        }
            break;
        case java_awt_AlphaComposite_SRC_ATOP:
        {
            result = "SRC_ATOP";
        }
            break;
        case java_awt_AlphaComposite_DST_ATOP:
        {
            result = "DST_ATOP";
        }
            break;
        case java_awt_AlphaComposite_XOR:
        {
            result = "XOR";
        }
            break;
        default:
            result = "UNKNOWN";
            break;
    }
    const double epsilon = 0.001f;
    if (fabs(_extraAlpha - 1.f) > epsilon) {
        return [NSString stringWithFormat:@"%s [%1.2f]", result, _extraAlpha];
    }
    return [NSString stringWithFormat:@"%s", result];
}

- (void)setAlphaComposite:(jint)rule {
    _compState = sun_java2d_SunGraphics2D_COMP_ALPHA;
    [self setRule:rule];
}


- (jint)getCompositeState {
    return _compState;
}


-(void)setXORComposite:(jint)color {
    _compState = sun_java2d_SunGraphics2D_COMP_XOR;
    _xorPixel = color;
}

-(jint)getXorColor {
    return _xorPixel;
}

- (jfloat)getExtraAlpha {
    return _extraAlpha;
}

@end
