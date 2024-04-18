/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibAudio/Queue.h>
#include <LibGfx/Color.h>

namespace Music {

// CD quality
// - Stereo
// - 16 bit
// - 44,100 samples/sec
// - 1,411.2 kbps

constexpr int sample_count = Audio::AUDIO_BUFFER_SIZE * 10;

constexpr double sample_rate = 44100;

// Headroom for the synth
constexpr double volume_factor = 0.8;

enum Direction {
    Down,
    Up,
};

enum KeyColor {
    White,
    Black,
};

constexpr KeyColor key_pattern[] = {
    White,
    Black,
    White,
    Black,
    White,
    White,
    Black,
    White,
    Black,
    White,
    Black,
    White,
};

Color const note_pressed_color(64, 64, 255);
Color const column_playing_color(128, 128, 255);

Color const left_wave_colors[] = {
    // Sine
    {
        255,
        192,
        0,
    },
    // Triangle
    {
        35,
        171,
        35,
    },
    // Square
    {
        128,
        160,
        255,
    },
    // Saw
    {
        240,
        100,
        128,
    },
    // Noise
    {
        197,
        214,
        225,
    },
    // RecordedSample
    {
        227,
        39,
        39,
    },
};

// HACK: make the display code shut up for now
constexpr int RecordedSample = 5;

Color const right_wave_colors[] = {
    // Sine
    {
        255,
        223,
        0,
    },
    // Triangle
    {
        35,
        171,
        90,
    },
    // Square
    {
        139,
        128,
        255,
    },
    // Saw
    {
        240,
        100,
        220,
    },
    // Noise
    {
        197,
        223,
        225,
    },
    // RecordedSample
    {
        227,
        105,
        39,
    },
};

constexpr int notes_per_octave = 12;
constexpr int white_keys_per_octave = 7;
constexpr int black_keys_per_octave = 5;
constexpr int octave_min = 1;
constexpr int octave_max = 7;

constexpr int volume_max = 1000;

constexpr double beats_per_minute = 60;
constexpr int beats_per_bar = 4;
constexpr int notes_per_beat = 4;
constexpr int roll_length = (sample_rate / (beats_per_minute / 60)) * beats_per_bar;

constexpr StringView note_names[] = {
    "C"sv,
    "C#"sv,
    "D"sv,
    "D#"sv,
    "E"sv,
    "F"sv,
    "F#"sv,
    "G"sv,
    "G#"sv,
    "A"sv,
    "A#"sv,
    "B"sv,
};

// Equal temperament, A = 440Hz
// We calculate note frequencies relative to A4:
// 440.0 * pow(pow(2.0, 1.0 / 12.0), N)
// Where N is the note distance from A.
constexpr double note_frequencies[] = {
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
constexpr int note_count = sizeof(note_frequencies) / sizeof(double);

constexpr double middle_c = note_frequencies[36];

}

using namespace Music;
