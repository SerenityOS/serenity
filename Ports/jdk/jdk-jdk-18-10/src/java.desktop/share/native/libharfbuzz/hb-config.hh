/*
 * Copyright © 2019  Facebook, Inc.
 *
 *  This is part of HarfBuzz, a text shaping library.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE COPYRIGHT HOLDER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE COPYRIGHT HOLDER HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Facebook Author(s): Behdad Esfahbod
 */

#ifndef HB_CONFIG_HH
#define HB_CONFIG_HH

#if 0 /* Make test happy. */
#include "hb.hh"
#endif

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif


#ifdef HB_TINY
#define HB_LEAN
#define HB_MINI
#define HB_NO_MT
#define HB_NO_UCD_UNASSIGNED
#ifndef NDEBUG
#define NDEBUG
#endif
#ifndef __OPTIMIZE_SIZE__
#define __OPTIMIZE_SIZE__
#endif
#endif

#ifdef HB_LEAN
#define HB_DISABLE_DEPRECATED
#define HB_NDEBUG
#define HB_NO_ATEXIT
#define HB_NO_BUFFER_MESSAGE
#define HB_NO_BUFFER_SERIALIZE
#define HB_NO_BITMAP
#define HB_NO_CFF
#define HB_NO_COLOR
#define HB_NO_DRAW
#define HB_NO_ERRNO
#define HB_NO_FACE_COLLECT_UNICODES
#define HB_NO_GETENV
#define HB_NO_HINTING
#define HB_NO_LANGUAGE_PRIVATE_SUBTAG
#define HB_NO_LAYOUT_FEATURE_PARAMS
#define HB_NO_LAYOUT_COLLECT_GLYPHS
#define HB_NO_LAYOUT_UNUSED
#define HB_NO_MATH
#define HB_NO_META
#define HB_NO_METRICS
#define HB_NO_MMAP
#define HB_NO_NAME
#define HB_NO_OPEN
#define HB_NO_SETLOCALE
#define HB_NO_OT_FONT_GLYPH_NAMES
#define HB_NO_OT_SHAPE_FRACTIONS
#define HB_NO_STYLE
#define HB_NO_SUBSET_LAYOUT
#define HB_NO_VAR
#endif

#ifdef HB_MINI
#define HB_NO_AAT
#define HB_NO_LEGACY
#endif


/* Closure of options. */

#ifdef HB_DISABLE_DEPRECATED
#define HB_IF_NOT_DEPRECATED(x)
#else
#define HB_IF_NOT_DEPRECATED(x) x
#endif

#ifdef HB_NO_AAT
#define HB_NO_OT_NAME_LANGUAGE_AAT
#define HB_NO_AAT_SHAPE
#endif

#ifdef HB_NO_BITMAP
#define HB_NO_OT_FONT_BITMAP
#endif

#ifdef HB_NO_CFF
#define HB_NO_OT_FONT_CFF
#define HB_NO_SUBSET_CFF
#endif

#ifdef HB_NO_GETENV
#define HB_NO_UNISCRIBE_BUG_COMPATIBLE
#endif

#ifdef HB_NO_LEGACY
#define HB_NO_CMAP_LEGACY_SUBTABLES
#define HB_NO_FALLBACK_SHAPE
#define HB_NO_OT_KERN
#define HB_NO_OT_LAYOUT_BLACKLIST
#define HB_NO_OT_SHAPE_FALLBACK
#endif

#ifdef HB_NO_NAME
#define HB_NO_OT_NAME_LANGUAGE
#endif

#ifdef HB_NO_OT
#define HB_NO_OT_FONT
#define HB_NO_OT_LAYOUT
#define HB_NO_OT_TAG
#define HB_NO_OT_SHAPE
#endif

#ifdef HB_NO_OT_SHAPE
#define HB_NO_AAT_SHAPE
#endif

#ifdef HB_NO_OT_SHAPE_FALLBACK
#define HB_NO_OT_SHAPE_COMPLEX_ARABIC_FALLBACK
#define HB_NO_OT_SHAPE_COMPLEX_HEBREW_FALLBACK
#define HB_NO_OT_SHAPE_COMPLEX_THAI_FALLBACK
#define HB_NO_OT_SHAPE_COMPLEX_VOWEL_CONSTRAINTS
#endif

#ifdef NDEBUG
#ifndef HB_NDEBUG
#define HB_NDEBUG
#endif
#endif

#ifdef __OPTIMIZE_SIZE__
#ifndef HB_OPTIMIZE_SIZE
#define HB_OPTIMIZE_SIZE
#endif
#endif

#ifdef HAVE_CONFIG_OVERRIDE_H
#include "config-override.h"
#endif


#endif /* HB_CONFIG_HH */
