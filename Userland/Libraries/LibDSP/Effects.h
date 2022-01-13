/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Processor.h"
#include "ProcessorParameter.h"
#include "Transport.h"
#include <AK/Types.h>

namespace LibDSP::Effects {

// A simple digital delay effect using a delay buffer.
// This is based on Piano's old built-in delay.
class Delay : public EffectProcessor {
public:
    Delay(NonnullRefPtr<Transport>);

private:
    virtual Signal process_impl(Signal const&) override;
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

private:
    virtual Signal process_impl(Signal const&) override;
};

}
