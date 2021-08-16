/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 *  Super-cheesy assertions that aren't efficient when they are turned on, but
 *  are free when turned off (all pre-processor stuff)
 */


#ifndef _JPLISASSERT_H_
#define _JPLISASSERT_H_

#include    <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JPLISASSERT_ENABLEASSERTIONS    (1)


#ifndef JPLISASSERT_ENABLEASSERTIONS
#define JPLISASSERT_ENABLEASSERTIONS    (0)
#endif

#if JPLISASSERT_ENABLEASSERTIONS
#define jplis_assert(x)             JPLISAssertCondition((jboolean)(x), #x, __FILE__, __LINE__)
#define jplis_assert_msg(x, msg)    JPLISAssertConditionWithMessage((jboolean)(x), #x, msg, __FILE__, __LINE__)
#else
#define jplis_assert(x)
#define jplis_assert_msg(x, msg)
#endif

/*
 * Test the supplied condition.
 * If false, print a constructed message including source site info to stderr.
 * If true, do nothing.
 */
extern void
JPLISAssertCondition(   jboolean        condition,
                        const char *    assertionText,
                        const char *    file,
                        int             line);

/*
 * Test the supplied condition.
 * If false, print a constructed message including source site info
 * and the supplied message to stderr.
 * If true, do nothing.
 */
extern void
JPLISAssertConditionWithMessage(    jboolean        condition,
                                    const char *    assertionText,
                                    const char *    message,
                                    const char *    file,
                                    int             line);




#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */


#endif
