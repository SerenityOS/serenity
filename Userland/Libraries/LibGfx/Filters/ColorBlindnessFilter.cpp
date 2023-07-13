/*
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ColorBlindnessFilter.h"

namespace Gfx {

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_protanopia()
{
    return make<ColorBlindnessFilter>(
        .56, .44, .0,
        .55, .45, .0,
        .0, .24, .76);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_protanomaly()
{
    return make<ColorBlindnessFilter>(
        .82, .18, .0,
        .33, .67, .0,
        .0, .13, .87);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_deuteranopia()
{
    return make<ColorBlindnessFilter>(
        .63, .37, .0,
        .7, .3, .0,
        .0, .3, .7);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_deuteranomaly()
{
    return make<ColorBlindnessFilter>(
        .8, .2, .0,
        .26, .74, .0,
        .0, .15, .85);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_tritanopia()
{
    return make<ColorBlindnessFilter>(
        .95, .05, .0,
        .0, .44, .56,
        .0, .48, .52);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_tritanomaly()
{
    return make<ColorBlindnessFilter>(
        .97, .03, .0,
        .0, .73, .27,
        .0, .18, .82);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_achromatopsia()
{
    return make<ColorBlindnessFilter>(
        .3, .59, .11,
        .3, .59, .11,
        .3, .59, .11);
}

NonnullOwnPtr<ColorBlindnessFilter> ColorBlindnessFilter::create_achromatomaly()
{
    return make<ColorBlindnessFilter>(
        .62, .32, .06,
        .16, .78, .06,
        .16, .32, .52);
}

Color ColorBlindnessFilter::convert_color(Color original)
{
    return Color(
        (u8)(original.red() * m_red_in_red_band + original.green() * m_green_in_red_band + original.blue() * m_blue_in_red_band),
        (u8)(original.red() * m_red_in_green_band + original.green() * m_green_in_green_band + original.blue() * m_blue_in_green_band),
        (u8)(original.red() * m_red_in_blue_band + original.green() * m_green_in_blue_band + original.blue() * m_blue_in_blue_band),
        original.alpha());
}

}
