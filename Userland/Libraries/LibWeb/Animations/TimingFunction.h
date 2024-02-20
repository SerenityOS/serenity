/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Vector.h>

namespace Web::CSS {
class EasingStyleValue;
}

namespace Web::Animations {

// https://www.w3.org/TR/css-easing-1/#the-linear-easing-function
struct LinearTimingFunction {
    double operator()(double t, bool) const;
};

// https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions
struct CubicBezierTimingFunction {
    double x1;
    double y1;
    double x2;
    double y2;

    struct CachedSample {
        double x;
        double y;
        double t;
    };

    mutable Vector<CachedSample, 64> m_cached_x_samples = {};

    double operator()(double input_progress, bool) const;
};

// https://www.w3.org/TR/css-easing-1/#step-easing-functions
struct StepsTimingFunction {
    size_t number_of_steps;
    bool jump_at_start;
    bool jump_at_end;

    double operator()(double input_progress, bool before_flag) const;
};

struct TimingFunction {
    static TimingFunction from_easing_style_value(CSS::EasingStyleValue const&);

    Variant<LinearTimingFunction, CubicBezierTimingFunction, StepsTimingFunction> function;

    double operator()(double input_progress, bool before_flag) const;
};

static TimingFunction linear_timing_function { LinearTimingFunction {} };
// NOTE: Magic values from <https://www.w3.org/TR/css-easing-1/#valdef-cubic-bezier-easing-function-ease>
static TimingFunction ease_timing_function { CubicBezierTimingFunction { 0.25, 0.1, 0.25, 1.0 } };
static TimingFunction ease_in_timing_function { CubicBezierTimingFunction { 0.42, 0.0, 1.0, 1.0 } };
static TimingFunction ease_out_timing_function { CubicBezierTimingFunction { 0.0, 0.0, 0.58, 1.0 } };
static TimingFunction ease_in_out_timing_function { CubicBezierTimingFunction { 0.42, 0.0, 0.58, 1.0 } };

}
