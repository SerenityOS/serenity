/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibDSP/Processor.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Transport.h>

namespace DSP::Effects {

// A simple digital delay effect using a delay buffer.
// This is based on Piano's old built-in delay.
class Delay : public EffectProcessor {
public:
    Delay(NonnullRefPtr<Transport>);

private:
    virtual void process_impl(Signal const&, Signal&) override;
    void handle_delay_time_change();

    ProcessorRangeParameter m_delay_decay;
    ProcessorRangeParameter m_delay_time;
    ProcessorRangeParameter m_dry_gain;

    Vector<Sample> m_delay_buffer;
    size_t m_delay_index { 0 };
};

// A simple effect that applies volume, mute and pan to its input signal.
// Convenient for attenuating signals in the middle of long chains.
class Mastering : public EffectProcessor {
public:
    Mastering(NonnullRefPtr<Transport>);

    // The mastering processor can be used by the track and therefore needs to be able to write to a fixed array directly.
    // Otherwise, Track needs to do more unnecessary sample data copies.
    void process_to_fixed_array(Signal const&, FixedArray<Sample>&);

private:
    virtual void process_impl(Signal const&, Signal&) override;

    ProcessorRangeParameter m_pan;
    ProcessorRangeParameter m_volume;
    ProcessorBooleanParameter m_muted;
};

}
