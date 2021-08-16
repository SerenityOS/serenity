/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright 2003 Wily Technology, Inc.
 */

#include    <jni.h>

#include    "JPLISAssert.h"

/*
 *  Super-cheesy assertions that aren't efficient when they are turned on, but
 *  are free when turned off (all pre-processor stuff)
 */

void
JPLISAssertCondition(   jboolean        condition,
                        const char *    assertionText,
                        const char *    file,
                        int             line) {
    if ( !condition ) {
        fprintf(stderr, "*** java.lang.instrument ASSERTION FAILED ***: \"%s\" at %s line: %d\n",
                                            assertionText,
                                            file,
                                            line);
    }
}


void
JPLISAssertConditionWithMessage(    jboolean        condition,
                                    const char *    assertionText,
                                    const char *    message,
                                    const char *    file,
                                    int             line) {
    if ( !condition ) {
        fprintf(stderr, "*** java.lang.instrument ASSERTION FAILED ***: \"%s\" with message %s at %s line: %d\n",
                                            assertionText,
                                            message,
                                            file,
                                            line);
    }
}
