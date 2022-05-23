/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/Math.h>
#include <AK/Random.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <LibAudio/Sample.h>
#include <LibDSP/Envelope.h>
#include <LibDSP/Processor.h>
#include <LibDSP/Synthesizers.h>

namespace LibDSP::Synthesizers {

Classic::Classic(NonnullRefPtr<Transport> transport)
    : LibDSP::SynthesizerProcessor(transport)
    , m_waveform("Waveform"sv, Waveform::Saw)
    , m_attack("Attack"sv, 0.01, 2000, 5, Logarithmic::Yes)
    , m_decay("Decay"sv, 0.01, 20'000, 80, Logarithmic::Yes)
    , m_sustain("Sustain"sv, 0.001, 1, 0.725, Logarithmic::No)
    , m_release("Release", 0.01, 6'000, 120, Logarithmic::Yes)
{
    m_parameters.append(m_waveform);
    m_parameters.append(m_attack);
    m_parameters.append(m_decay);
    m_parameters.append(m_sustain);
    m_parameters.append(m_release);
}

void Classic::process_impl(Signal const& input_signal, [[maybe_unused]] Signal& output_signal)
{
    auto const& in = input_signal.get<RollNotes>();
    auto& output_samples = output_signal.get<FixedArray<Sample>>();

    // Do this for every time step and set the signal accordingly.
    for (size_t sample_index = 0; sample_index < output_samples.size(); ++sample_index) {
        Sample& out = output_samples[sample_index];
        u32 sample_time = m_transport->time() + sample_index;

        SinglyLinkedList<PitchedEnvelope> playing_envelopes;

        // "Press" the necessary notes in the internal representation,
        // and "release" all of the others
        for (u8 i = 0; i < note_frequencies.size(); ++i) {
            if (auto maybe_note = in.get(i); maybe_note.has_value())
                m_playing_notes.set(i, maybe_note.value());

            if (m_playing_notes.contains(i)) {
                Envelope note_envelope = m_playing_notes.get(i)->to_envelope(sample_time, m_attack * m_transport->ms_sample_rate(), m_decay * m_transport->ms_sample_rate(), m_release * m_transport->ms_sample_rate());
                if (!note_envelope.is_active()) {
                    m_playing_notes.remove(i);
                    continue;
                }

                playing_envelopes.append(PitchedEnvelope { note_envelope, i });
            }
        }

        for (auto envelope : playing_envelopes) {
            double volume = volume_from_envelope(envelope);
            double wave = wave_position(envelope.note);
            out += volume * wave;
        }
    }
}

// Linear ADSR envelope with no peak adjustment.
double Classic::volume_from_envelope(Envelope const& envelope) const
{
    switch (static_cast<EnvelopeState>(envelope)) {
    case EnvelopeState::Off:
        return 0;
    case EnvelopeState::Attack:
        return envelope.attack();
    case EnvelopeState::Decay:
        // As we fade from high (1) to low (headroom above the sustain level) here, use 1-decay as the interpolation.
        return (1. - envelope.decay()) * (1. - m_sustain) + m_sustain;
    case EnvelopeState::Sustain:
        return m_sustain;
    case EnvelopeState::Release:
        // Same goes for the release fade from high to low.
        return (1. - envelope.release()) * m_sustain;
    }
    VERIFY_NOT_REACHED();
}

double Classic::wave_position(u8 note)
{
    switch (m_waveform) {
    case Sine:
        return sin_position(note);
    case Triangle:
        return triangle_position(note);
    case Square:
        return square_position(note);
    case Saw:
        return saw_position(note);
    case Noise:
        return noise_position(note);
    }
    VERIFY_NOT_REACHED();
}

double Classic::samples_per_cycle(u8 note) const
{
    return m_transport->sample_rate() / note_frequencies[note];
}

double Classic::sin_position(u8 note) const
{
    double spc = samples_per_cycle(note);
    double cycle_pos = m_transport->time() / spc;
    return AK::sin(cycle_pos * 2 * AK::Pi<double>);
}

// Absolute value of the saw wave "flips" the negative portion into the positive, creating a ramp up and down.
double Classic::triangle_position(u8 note) const
{
    double saw = saw_position(note);
    return AK::fabs(saw) * 2 - 1;
}

// The first half of the cycle period is 1, the other half -1.
double Classic::square_position(u8 note) const
{
    double spc = samples_per_cycle(note);
    double progress = AK::fmod(static_cast<double>(m_transport->time()), spc) / spc;
    return progress >= 0.5 ? -1 : 1;
}

// Modulus creates inverse saw, which we need to flip and scale.
double Classic::saw_position(u8 note) const
{
    double spc = samples_per_cycle(note);
    double unscaled = spc - AK::fmod(static_cast<double>(m_transport->time()), spc);
    return unscaled / (samples_per_cycle(note) / 2.) - 1;
}

// We resample the noise twenty times per cycle.
double Classic::noise_position(u8 note)
{
    double spc = samples_per_cycle(note);
    u32 getrandom_interval = max(static_cast<u32>(spc / 2), 1);
    // Note that this code only works well if the processor is called for every increment of time.
    if (m_transport->time() % getrandom_interval == 0)
        last_random[note] = (get_random<u16>() / static_cast<double>(NumericLimits<u16>::max()) - .5) * 2;
    return last_random[note];
}

}
