/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibDSP/FeedFilter.h>
#include <LibDSP/Transport.h>

namespace DSP {

// A simple filter that adjusts a specific frequency band up or down.
// (A chaining of these filters is what we use for parametric equalizers.)
// Note that the most generic band adjustment filter allows you to set a global gain offset as well as the bandwidth gain.
// We don't need this; both are fixed to 0dB and therefore the bandwidth is defined to be the total range of frequencies that get boosted or attenuated.
// Adopted from https://8void.files.wordpress.com/2017/11/orfanidis.pdf
class BandAdjustmentFilter : public SampleFeedFilter<3> {
public:
    BandAdjustmentFilter(NonnullRefPtr<Transport> transport)
        : m_transport(move(transport))
    {
    }

    // Frequency around which the band adjustment is centered.
    void set_center_frequency(float frequency);
    // Gain adjustment at the center frequency (absolute multiplier).
    void set_gain(float gain);
    // Gain adjustment at the center frequency (decibels).
    void set_gain_db(float gain_db);
    // Ratio of center frequency to bandwidth; the (multiplicative) Q factor is more intuitive than a frequency bandwidth.
    void set_q(float q_factor);

    float center_frequency() const;
    float gain() const;
    float gain_db() const;
    float q() const;

private:
    void recompute_coefficients();

    float m_center_frequency { 1000.0f };
    float m_q { 1.0f };
    float m_gain { 1.0f };

    // Needed for requesting sample rate, as the bilinear transformed digital transfer function of the filter (don't ask) depends on the Nyquist frequency.
    NonnullRefPtr<Transport> m_transport;
};

}
