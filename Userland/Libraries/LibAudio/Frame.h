/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>

namespace Audio {
using namespace AK::Exponentials;

// Constants for logarithmic volume. See Frame::operator*
// Corresponds to 60dB
constexpr double DYNAMIC_RANGE = 1000;
constexpr double VOLUME_A = 1 / DYNAMIC_RANGE;
double const VOLUME_B = log(DYNAMIC_RANGE);

// A single sample in an audio buffer.
// Values are floating point, and should range from -1.0 to +1.0
struct Frame {
    constexpr Frame() = default;

    // For mono
    constexpr Frame(double left)
        : left(left)
        , right(left)
    {
    }

    // For stereo
    constexpr Frame(double left, double right)
        : left(left)
        , right(right)
    {
    }

    void clip()
    {
        if (left > 1)
            left = 1;
        else if (left < -1)
            left = -1;

        if (right > 1)
            right = 1;
        else if (right < -1)
            right = -1;
    }

    // Logarithmic scaling, as audio should ALWAYS do.
    // Reference: https://www.dr-lex.be/info-stuff/volumecontrols.html
    // We use the curve `factor = a * exp(b * change)`,
    // where change is the input fraction we want to change by,
    // a = 1/1000, b = ln(1000) = 6.908 and factor is the multiplier used.
    // The value 1000 represents the dynamic range in sound pressure, which corresponds to 60 dB(A).
    // This is a good dynamic range because it can represent all loudness values from
    // 30 dB(A) (barely hearable with background noise)
    // to 90 dB(A) (almost too loud to hear and about the reasonable limit of actual sound equipment).
    ALWAYS_INLINE double log_factor(double const change)
    {
        return VOLUME_A * exp(VOLUME_B * change);
    }

    ALWAYS_INLINE Frame& log_multiply(double const change)
    {
        double factor = log_factor(change);
        left *= factor;
        right *= factor;
        return *this;
    }

    ALWAYS_INLINE Frame log_multiplied(double const volume_change) const
    {
        Frame new_frame { left, right };
        new_frame.log_multiply(volume_change);
        return new_frame;
    }

    ALWAYS_INLINE Frame& log_pan(double const pan)
    {
        left *= log_factor(min(pan * -1 + 1.0, 1.0));
        right *= log_factor(min(pan + 1.0, 1.0));
        return *this;
    }

    ALWAYS_INLINE Frame log_pan(double const pan) const
    {
        Frame new_frame { left, right };
        new_frame.log_pan(pan);
        return new_frame;
    }

    constexpr Frame& operator*=(double const mult)
    {
        left *= mult;
        right *= mult;
        return *this;
    }

    constexpr Frame operator*(double const mult)
    {
        return { left * mult, right * mult };
    }

    constexpr Frame& operator+=(Frame const& other)
    {
        left += other.left;
        right += other.right;
        return *this;
    }
    constexpr Frame& operator+=(double other)
    {
        left += other;
        right += other;
        return *this;
    }

    constexpr Frame operator+(Frame const& other)
    {
        return { left + other.left, right + other.right };
    }

    double left { 0 };
    double right { 0 };
};

}
