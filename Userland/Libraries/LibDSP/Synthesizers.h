/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LibDSP/Music.h"
#include <AK/SinglyLinkedList.h>
#include <LibDSP/Processor.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Transport.h>

namespace LibDSP::Synthesizers {

enum Waveform : u8 {
    Sine,
    Triangle,
    Square,
    Saw,
    Noise,
};

struct PitchedEnvelope : Envelope {
    constexpr PitchedEnvelope() = default;
    constexpr PitchedEnvelope(double envelope, u8 note)
        : Envelope(envelope)
        , note(note)
    {
    }
    constexpr PitchedEnvelope(Envelope envelope, u8 note)
        : Envelope(envelope)
        , note(note)
    {
    }

    u8 note;
};

class Classic : public SynthesizerProcessor {
public:
    Classic(NonnullRefPtr<Transport>);

    static Envelope compute_envelope(RollNote&);

    Waveform wave() const { return m_waveform.value(); }

private:
    virtual Signal process_impl(Signal const&) override;

    double volume_from_envelope(Envelope const&);
    double wave_position(u8 note);
    double samples_per_cycle(u8 note);
    double sin_position(u8 note);
    double triangle_position(u8 note);
    double square_position(u8 note);
    double saw_position(u8 note);
    double noise_position(u8 note);
    double get_random_from_seed(u64 note);

    ProcessorEnumParameter<Waveform> m_waveform;
    ProcessorRangeParameter m_attack;
    ProcessorRangeParameter m_decay;
    ProcessorRangeParameter m_sustain;
    ProcessorRangeParameter m_release;

    RollNotes m_playing_notes;
    Array<double, note_count> last_random;
};

}
