/*
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ColorFilter.h"
#include <AK/NonnullOwnPtr.h>
#include <AK/StringView.h>

namespace Gfx {
class ColorBlindnessFilter : public ColorFilter {
public:
    ColorBlindnessFilter(
        double red_in_red_band,
        double green_in_red_band,
        double blue_in_red_band,
        double red_in_green_band,
        double green_in_green_band,
        double blue_in_green_band,
        double red_in_blue_band,
        double green_in_blue_band,
        double blue_in_blue_band)
        : m_red_in_red_band(red_in_red_band)
        , m_green_in_red_band(green_in_red_band)
        , m_blue_in_red_band(blue_in_red_band)
        , m_red_in_green_band(red_in_green_band)
        , m_green_in_green_band(green_in_green_band)
        , m_blue_in_green_band(blue_in_green_band)
        , m_red_in_blue_band(red_in_blue_band)
        , m_green_in_blue_band(green_in_blue_band)
        , m_blue_in_blue_band(blue_in_blue_band)
    {
    }

    virtual ~ColorBlindnessFilter() = default;
    virtual StringView class_name() const override { return "ColorBlindnessFilter"sv; }

    static NonnullOwnPtr<ColorBlindnessFilter> create_protanopia();
    static NonnullOwnPtr<ColorBlindnessFilter> create_protanomaly();
    static NonnullOwnPtr<ColorBlindnessFilter> create_deuteranopia();
    static NonnullOwnPtr<ColorBlindnessFilter> create_deuteranomaly();
    static NonnullOwnPtr<ColorBlindnessFilter> create_tritanopia();
    static NonnullOwnPtr<ColorBlindnessFilter> create_tritanomaly();
    static NonnullOwnPtr<ColorBlindnessFilter> create_achromatopsia();
    static NonnullOwnPtr<ColorBlindnessFilter> create_achromatomaly();

protected:
    Color convert_color(Color original) override;

private:
    double m_red_in_red_band;
    double m_green_in_red_band;
    double m_blue_in_red_band;
    double m_red_in_green_band;
    double m_green_in_green_band;
    double m_blue_in_green_band;
    double m_red_in_blue_band;
    double m_green_in_blue_band;
    double m_blue_in_blue_band;
};
}
