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
// Constants for logarithmic volume. See Sample::linear_to_log
// Corresponds to 60dB
constexpr double DYNAMIC_RANGE = 1000;
constexpr double VOLUME_A = 1 / DYNAMIC_RANGE;
double const VOLUME_B = log(DYNAMIC_RANGE);

// A single sample in an audio buffer.
// Values are floating point, and should range from -1.0 to +1.0
struct Sample {
    constexpr Sample() = default;

    // For mono
    constexpr Sample(double left)
        : left(left)
        , right(left)
    {
    }

    // For stereo
    constexpr Sample(double left, double right)
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
    //
    // Format ranges:
    // - Linear:        0.0 to 1.0
    // - Logarithmic:   0.0 to 1.0

    ALWAYS_INLINE double linear_to_log(double const change)
    {
        // TODO: Add linear slope around 0
        return VOLUME_A * exp(VOLUME_B * change);
    }

    ALWAYS_INLINE double log_to_linear(double const val)
    {
        // TODO: Add linear slope around 0
        return log(val / VOLUME_A) / VOLUME_B;
    }

    ALWAYS_INLINE Sample& log_multiply(double const change)
    {
        double factor = linear_to_log(change);
        left *= factor;
        right *= factor;
        return *this;
    }

    ALWAYS_INLINE Sample log_multiplied(double const volume_change) const
    {
        Sample new_frame { left, right };
        new_frame.log_multiply(volume_change);
        return new_frame;
    }

    ALWAYS_INLINE Sample& log_pan(double const pan)
    {
        left *= linear_to_log(min(pan * -1 + 1.0, 1.0));
        right *= linear_to_log(min(pan + 1.0, 1.0));
        return *this;
    }

    ALWAYS_INLINE Sample log_pan(double const pan) const
    {
        Sample new_frame { left, right };
        new_frame.log_pan(pan);
        return new_frame;
    }

    constexpr Sample& operator*=(double const mult)
    {
        left *= mult;
        right *= mult;
        return *this;
    }

    constexpr Sample operator*(double const mult)
    {
        return { left * mult, right * mult };
    }

    constexpr Sample& operator+=(Sample const& other)
    {
        left += other.left;
        right += other.right;
        return *this;
    }
    constexpr Sample& operator+=(double other)
    {
        left += other;
        right += other;
        return *this;
    }

    constexpr Sample operator+(Sample const& other)
    {
        return { left + other.left, right + other.right };
    }

    double left { 0 };
    double right { 0 };
};

}
