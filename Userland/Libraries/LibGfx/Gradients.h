/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Point.h>

namespace Gfx {

struct GradientStop {
    Color color;
    float percentage;
};

struct LinearGradient {
    AK::Vector<GradientStop> stops;
    Point<float> p1;
    Point<float> p2;

    Color operator()(Point<float> p) const
    {
        auto d = p2 - p1;
        auto o = p - p1;
        auto percentage = o.dot(d) / d.dot(d);
        auto i_next = stops.find_if([percentage](auto&& stop) { return stop.percentage >= percentage; });
        if (i_next == stops.end()) {
            return stops.last().color;
        } else if (i_next == stops.begin()) {
            return stops.first().color;
        } else {
            auto& left = *(i_next - 1);
            auto& right = *i_next;
            auto weight = (percentage - left.percentage) / (right.percentage - left.percentage);
            return left.color.interpolate(right.color, weight);
        }
    }
};

}
