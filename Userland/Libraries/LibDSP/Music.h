/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibAudio/Sample.h>
#include <LibDSP/Envelope.h>

namespace DSP {

using Sample = Audio::Sample;

constexpr Sample const SAMPLE_OFF = { 0.0, 0.0 };

struct RollNote {
    constexpr u32 length() const { return (off_sample - on_sample) + 1; }

    u32 on_sample;
    u32 off_sample;
    u8 pitch;
    i8 velocity;

    constexpr Envelope to_envelope(u32 time, u32 attack_samples, u32 decay_samples, u32 release_samples) const
    {
        i64 time_since_end = static_cast<i64>(time) - static_cast<i64>(off_sample);
        // We're before the end of this note.
        if (time_since_end < 0) {
            i64 time_since_start = static_cast<i64>(time) - static_cast<i64>(on_sample);
            if (time_since_start < 0)
                return {};

            if (time_since_start < attack_samples) {
                if (attack_samples == 0)
                    return Envelope::from_attack(0);
                return Envelope::from_attack(static_cast<double>(time_since_start) / static_cast<double>(attack_samples));
            }
            if (time_since_start < attack_samples + decay_samples) {
                if (decay_samples == 0)
                    return Envelope::from_decay(0);
                return Envelope::from_decay(static_cast<double>(time_since_start - attack_samples) / static_cast<double>(decay_samples));
            }
            // This is a note-dependent value!
            u32 sustain_samples = length() - attack_samples - decay_samples;
            return Envelope::from_sustain(static_cast<double>(time_since_start - attack_samples - decay_samples) / static_cast<double>(sustain_samples));
        }

        // Overshot the release time
        if (time_since_end > release_samples)
            return {};
        return Envelope::from_release(static_cast<double>(time_since_end) / static_cast<double>(release_samples));
    }

    constexpr bool is_playing(u32 time) const { return on_sample <= time && time <= off_sample; }
    constexpr bool is_playing_during(u32 start_time, u32 end_time) const
    {
        // There are three scenarios for a playing note.
        return
            // 1. The note ends within our time frame.
            (this->off_sample >= start_time && this->off_sample < end_time)
            // 2. The note starts within our time frame.
            || (this->on_sample >= start_time && this->on_sample < end_time)
            // 3. The note starts before our time frame and ends after it.
            || (this->on_sample < start_time && this->off_sample >= end_time);
    }

    constexpr bool overlaps_with(RollNote const& other) const
    {
        // Notes don't overlap if one is completely before or behind the other.
        return !((this->on_sample < other.on_sample && this->off_sample < other.on_sample)
            || (this->on_sample >= other.off_sample && this->off_sample >= other.off_sample));
    }
};

enum class SignalType : u8 {
    Invalid,
    Sample,
    Note
};

// Perfect hashing for note (MIDI) values. This just uses the note value as the hash itself.
class PerfectNoteHashTraits : Traits<u8> {
public:
    static constexpr bool equals(u8 const& a, u8 const& b) { return a == b; }
    static constexpr unsigned hash(u8 value)
    {
        return static_cast<unsigned>(value);
    }
};

// Equal temperament, A = 440Hz
// We calculate note frequencies relative to A4:
// 440.0 * pow(pow(2.0, 1.0 / 12.0), N)
// Where N is the note distance from A.
constexpr Array<double, 84> note_frequencies = {
    // Octave 1
    32.703195662574764,
    34.647828872108946,
    36.708095989675876,
    38.890872965260044,
    41.203444614108669,
    43.653528929125407,
    46.249302838954222,
    48.99942949771858,
    51.913087197493056,
    54.999999999999915,
    58.270470189761156,
    61.735412657015416,
    // Octave 2
    65.406391325149571,
    69.295657744217934,
    73.416191979351794,
    77.781745930520117,
    82.406889228217381,
    87.307057858250872,
    92.4986056779085,
    97.998858995437217,
    103.82617439498618,
    109.99999999999989,
    116.54094037952237,
    123.4708253140309,
    // Octave 3
    130.8127826502992,
    138.59131548843592,
    146.83238395870364,
    155.56349186104035,
    164.81377845643485,
    174.61411571650183,
    184.99721135581709,
    195.99771799087452,
    207.65234878997245,
    219.99999999999989,
    233.08188075904488,
    246.94165062806198,
    // Octave 4
    261.62556530059851,
    277.18263097687202,
    293.66476791740746,
    311.12698372208081,
    329.62755691286986,
    349.22823143300383,
    369.99442271163434,
    391.99543598174927,
    415.30469757994513,
    440,
    466.16376151808993,
    493.88330125612413,
    // Octave 5
    523.25113060119736,
    554.36526195374427,
    587.32953583481526,
    622.25396744416196,
    659.25511382574007,
    698.456462866008,
    739.98884542326903,
    783.99087196349899,
    830.60939515989071,
    880.00000000000034,
    932.32752303618031,
    987.76660251224882,
    // Octave 6
    1046.5022612023952,
    1108.7305239074892,
    1174.659071669631,
    1244.5079348883246,
    1318.5102276514808,
    1396.9129257320169,
    1479.977690846539,
    1567.9817439269987,
    1661.2187903197821,
    1760.000000000002,
    1864.6550460723618,
    1975.5332050244986,
    // Octave 7
    2093.0045224047913,
    2217.4610478149793,
    2349.3181433392633,
    2489.0158697766506,
    2637.020455302963,
    2793.8258514640347,
    2959.9553816930793,
    3135.9634878539991,
    3322.437580639566,
    3520.0000000000055,
    3729.3100921447249,
    3951.0664100489994,
};

using RollNotes = Array<Optional<RollNote>, note_frequencies.size()>;

constexpr size_t const notes_per_octave = 12;
constexpr double const middle_c = note_frequencies[36];

struct Signal : public Variant<FixedArray<Sample>, RollNotes> {
    using Variant::Variant;
    AK_MAKE_NONCOPYABLE(Signal);
    AK_MAKE_DEFAULT_MOVABLE(Signal);

public:
    ALWAYS_INLINE SignalType type() const
    {
        if (has<FixedArray<Sample>>())
            return SignalType::Sample;
        if (has<RollNotes>())
            return SignalType::Note;
        return SignalType::Invalid;
    }
};

}
