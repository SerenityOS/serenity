/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedList.h>
#include <LibDSP/Music.h>
#include <LibDSP/Processor.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Transport.h>

namespace DSP::Synthesizers {

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

    Waveform wave() const { return m_waveform.value(); }

private:
    virtual void process_impl(Signal const&, Signal&) override;

    double volume_from_envelope(Envelope const&) const;
    double wave_position(u32 sample_time, u8 note);
    double samples_per_cycle(u8 note) const;
    double sin_position(u32 sample_time, u8 note) const;
    double triangle_position(u32 sample_time, u8 note) const;
    double square_position(u32 sample_time, u8 note) const;
    double saw_position(u32 sample_time, u8 note) const;
    double noise_position(u32 sample_time, u8 note);

    ProcessorEnumParameter<Waveform> m_waveform;
    ProcessorRangeParameter m_attack;
    ProcessorRangeParameter m_decay;
    ProcessorRangeParameter m_sustain;
    ProcessorRangeParameter m_release;

    RollNotes m_playing_notes;
    Array<double, note_frequencies.size()> last_random;
};

}
