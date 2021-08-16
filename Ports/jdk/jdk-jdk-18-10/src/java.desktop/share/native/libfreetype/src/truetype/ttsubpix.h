/****************************************************************************
 *
 * ttsubpix.h
 *
 *   TrueType Subpixel Hinting.
 *
 * Copyright (C) 2010-2020 by
 * David Turner, Robert Wilhelm, and Werner Lemberg.
 *
 * This file is part of the FreeType project, and may only be used,
 * modified, and distributed under the terms of the FreeType project
 * license, LICENSE.TXT.  By continuing to use, modify, or distribute
 * this file you indicate that you have read the license and
 * understand and accept it fully.
 *
 */


#ifndef TTSUBPIX_H_
#define TTSUBPIX_H_

#include "ttobjs.h"
#include "ttinterp.h"


FT_BEGIN_HEADER


#ifdef TT_SUPPORT_SUBPIXEL_HINTING_INFINALITY

  /**************************************************************************
   *
   * ID flags to identify special functions at FDEF and runtime.
   *
   */
#define SPH_FDEF_INLINE_DELTA_1       0x0000001
#define SPH_FDEF_INLINE_DELTA_2       0x0000002
#define SPH_FDEF_DIAGONAL_STROKE      0x0000004
#define SPH_FDEF_VACUFORM_ROUND_1     0x0000008
#define SPH_FDEF_TTFAUTOHINT_1        0x0000010
#define SPH_FDEF_SPACING_1            0x0000020
#define SPH_FDEF_SPACING_2            0x0000040
#define SPH_FDEF_TYPEMAN_STROKES      0x0000080
#define SPH_FDEF_TYPEMAN_DIAGENDCTRL  0x0000100


  /**************************************************************************
   *
   * Tweak flags that are set for each glyph by the below rules.
   *
   */
#define SPH_TWEAK_ALLOW_X_DMOVE                   0x0000001UL
#define SPH_TWEAK_ALWAYS_DO_DELTAP                0x0000002UL
#define SPH_TWEAK_ALWAYS_SKIP_DELTAP              0x0000004UL
#define SPH_TWEAK_COURIER_NEW_2_HACK              0x0000008UL
#define SPH_TWEAK_DEEMBOLDEN                      0x0000010UL
#define SPH_TWEAK_DO_SHPIX                        0x0000020UL
#define SPH_TWEAK_EMBOLDEN                        0x0000040UL
#define SPH_TWEAK_MIAP_HACK                       0x0000080UL
#define SPH_TWEAK_NORMAL_ROUND                    0x0000100UL
#define SPH_TWEAK_NO_ALIGNRP_AFTER_IUP            0x0000200UL
#define SPH_TWEAK_NO_CALL_AFTER_IUP               0x0000400UL
#define SPH_TWEAK_NO_DELTAP_AFTER_IUP             0x0000800UL
#define SPH_TWEAK_PIXEL_HINTING                   0x0001000UL
#define SPH_TWEAK_RASTERIZER_35                   0x0002000UL
#define SPH_TWEAK_ROUND_NONPIXEL_Y_MOVES          0x0004000UL
#define SPH_TWEAK_SKIP_IUP                        0x0008000UL
#define SPH_TWEAK_SKIP_NONPIXEL_Y_MOVES           0x0010000UL
#define SPH_TWEAK_SKIP_OFFPIXEL_Y_MOVES           0x0020000UL
#define SPH_TWEAK_TIMES_NEW_ROMAN_HACK            0x0040000UL
#define SPH_TWEAK_SKIP_NONPIXEL_Y_MOVES_DELTAP    0x0080000UL


  FT_LOCAL( FT_Bool )
  sph_test_tweak( TT_Face               face,
                  const FT_String*      family,
                  FT_UInt               ppem,
                  const FT_String*      style,
                  FT_UInt               glyph_index,
                  const SPH_TweakRule*  rule,
                  FT_UInt               num_rules );

  FT_LOCAL( FT_UInt )
  sph_test_tweak_x_scaling( TT_Face           face,
                            const FT_String*  family,
                            FT_UInt           ppem,
                            const FT_String*  style,
                            FT_UInt           glyph_index );

  FT_LOCAL( void )
  sph_set_tweaks( TT_Loader  loader,
                  FT_UInt    glyph_index );


  /* These macros are defined absent a method for setting them */
#define SPH_OPTION_BITMAP_WIDTHS           FALSE
#define SPH_OPTION_SET_SUBPIXEL            TRUE
#define SPH_OPTION_SET_GRAYSCALE           FALSE
#define SPH_OPTION_SET_COMPATIBLE_WIDTHS   FALSE
#define SPH_OPTION_SET_RASTERIZER_VERSION  38

#endif /* TT_SUPPORT_SUBPIXEL_HINTING_INFINALITY */


FT_END_HEADER

#endif /* TTSUBPIX_H_ */


/* END */
