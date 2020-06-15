/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Track.h"
#include <AK/NumericLimits.h>
#include <LibAudio/WavLoader.h>
#include <math.h>

Track::Track(const u32& time)
    : m_time(time)
{
    set_sustain_impl(1000);
    set_attack(5);
    set_decay(1000);
    set_release(5);
}

Track::~Track()
{
}

void Track::fill_sample(Sample& sample)
{
    Audio::Sample new_sample;

    for (size_t note = 0; note < note_count; ++note) {
        if (!m_roll_iters[note].is_end()) {
            if (m_roll_iters[note]->on_sample == m_time) {
                set_note(note, On);
            } else if (m_roll_iters[note]->off_sample == m_time) {
                set_note(note, Off);
                ++m_roll_iters[note];
                if (m_roll_iters[note].is_end())
                    m_roll_iters[note] = m_roll_notes[note].begin();
            }
        }

        switch (m_envelope[note]) {
        case Done:
            continue;
        case Attack:
            m_power[note] += m_attack_step;
            if (m_power[note] >= 1) {
                m_power[note] = 1;
                m_envelope[note] = Decay;
            }
            break;
        case Decay:
            m_power[note] -= m_decay_step;
            if (m_power[note] < m_sustain_level)
                m_power[note] = m_sustain_level;
            break;
        case Release:
            m_power[note] -= m_release_step[note];
            if (m_power[note] <= 0) {
                m_power[note] = 0;
                m_envelope[note] = Done;
                continue;
            }
            break;
        default:
            ASSERT_NOT_REACHED();
        }

        Audio::Sample note_sample;
        switch (m_wave) {
        case Wave::Sine:
            note_sample = sine(note);
            break;
        case Wave::Saw:
            note_sample = saw(note);
            break;
        case Wave::Square:
            note_sample = square(note);
            break;
        case Wave::Triangle:
            note_sample = triangle(note);
            break;
        case Wave::Noise:
            note_sample = noise();
            break;
        case Wave::RecordedSample:
            note_sample = recorded_sample(note);
            break;
        default:
            ASSERT_NOT_REACHED();
        }
        new_sample.left += note_sample.left * m_power[note] * volume;
        new_sample.right += note_sample.right * m_power[note] * volume;
    }

    if (m_delay) {
        new_sample.left += m_delay_buffer[m_delay_index].left * 0.333333;
        new_sample.right += m_delay_buffer[m_delay_index].right * 0.333333;
        m_delay_buffer[m_delay_index].left = new_sample.left;
        m_delay_buffer[m_delay_index].right = new_sample.right;
        if (++m_delay_index >= m_delay_samples)
            m_delay_index = 0;
    }

    sample.left += new_sample.left;
    sample.right += new_sample.right;
}

void Track::reset()
{
    memset(m_delay_buffer.data(), 0, m_delay_buffer.size() * sizeof(Sample));
    m_delay_index = 0;

    memset(m_note_on, 0, sizeof(m_note_on));
    memset(m_power, 0, sizeof(m_power));
    memset(m_envelope, 0, sizeof(m_envelope));
}

String Track::set_recorded_sample(const StringView& path)
{
    Audio::WavLoader wav_loader(path);
    if (wav_loader.has_error())
        return String(wav_loader.error_string());
    auto wav_buffer = wav_loader.get_more_samples(60 * sample_rate * sizeof(Sample)); // 1 minute maximum

    if (!m_recorded_sample.is_empty())
        m_recorded_sample.clear();
    m_recorded_sample.resize(wav_buffer->sample_count());

    double peak = 0;
    for (int i = 0; i < wav_buffer->sample_count(); ++i) {
        double left_abs = fabs(wav_buffer->samples()[i].left);
        double right_abs = fabs(wav_buffer->samples()[i].right);
        if (left_abs > peak)
            peak = left_abs;
        if (right_abs > peak)
            peak = right_abs;
    }

    if (peak) {
        for (int i = 0; i < wav_buffer->sample_count(); ++i) {
            m_recorded_sample[i].left = wav_buffer->samples()[i].left / peak;
            m_recorded_sample[i].right = wav_buffer->samples()[i].right / peak;
        }
    }

    return String::empty();
}

// All of the information for these waves is on Wikipedia.

Audio::Sample Track::sine(size_t note)
{
    double pos = note_frequencies[note] / sample_rate;
    double sin_step = pos * 2 * M_PI;
    double w = sin(m_pos[note]);
    m_pos[note] += sin_step;
    return w;
}

Audio::Sample Track::saw(size_t note)
{
    double saw_step = note_frequencies[note] / sample_rate;
    double t = m_pos[note];
    double w = (0.5 - (t - floor(t))) * 2;
    m_pos[note] += saw_step;
    return w;
}

Audio::Sample Track::square(size_t note)
{
    double pos = note_frequencies[note] / sample_rate;
    double square_step = pos * 2 * M_PI;
    double w = sin(m_pos[note]) >= 0 ? 1 : -1;
    m_pos[note] += square_step;
    return w;
}

Audio::Sample Track::triangle(size_t note)
{
    double triangle_step = note_frequencies[note] / sample_rate;
    double t = m_pos[note];
    double w = fabs(fmod((4 * t) + 1, 4) - 2) - 1;
    m_pos[note] += triangle_step;
    return w;
}

Audio::Sample Track::noise() const
{
    double random_percentage = static_cast<double>(rand()) / RAND_MAX;
    double w = (random_percentage * 2) - 1;
    return w;
}

Audio::Sample Track::recorded_sample(size_t note)
{
    int t = m_pos[note];
    if (t >= static_cast<int>(m_recorded_sample.size()))
        return 0;
    double w_left = m_recorded_sample[t].left;
    double w_right = m_recorded_sample[t].right;
    if (t + 1 < static_cast<int>(m_recorded_sample.size())) {
        double t_fraction = m_pos[note] - t;
        w_left += (m_recorded_sample[t + 1].left - m_recorded_sample[t].left) * t_fraction;
        w_right += (m_recorded_sample[t + 1].right - m_recorded_sample[t].right) * t_fraction;
    }
    double recorded_sample_step = note_frequencies[note] / middle_c;
    m_pos[note] += recorded_sample_step;
    return { w_left, w_right };
}

static inline double calculate_step(double distance, int milliseconds)
{
    if (milliseconds == 0)
        return distance;

    constexpr double samples_per_millisecond = sample_rate / 1000.0;
    double samples = milliseconds * samples_per_millisecond;
    double step = distance / samples;
    return step;
}

void Track::set_note(int note, Switch switch_note)
{
    ASSERT(note >= 0 && note < note_count);

    if (switch_note == On) {
        if (m_note_on[note] == 0) {
            m_pos[note] = 0;
            m_envelope[note] = Attack;
        }
        ++m_note_on[note];
    } else {
        if (m_note_on[note] >= 1) {
            if (m_note_on[note] == 1) {
                m_release_step[note] = calculate_step(m_power[note], m_release);
                m_envelope[note] = Release;
            }
            --m_note_on[note];
        }
    }

    ASSERT(m_note_on[note] != NumericLimits<u8>::max());
    ASSERT(m_power[note] >= 0);
}

void Track::sync_roll(int note)
{
    auto it = m_roll_notes[note].find([&](auto& roll_note) { return roll_note.off_sample > m_time; });
    if (it.is_end())
        m_roll_iters[note] = m_roll_notes[note].begin();
    else
        m_roll_iters[note] = it;
}

void Track::set_roll_note(int note, u32 on_sample, u32 off_sample)
{
    RollNote new_roll_note = { on_sample, off_sample };

    ASSERT(note >= 0 && note < note_count);
    ASSERT(new_roll_note.off_sample < roll_length);
    ASSERT(new_roll_note.length() >= 2);

    for (auto it = m_roll_notes[note].begin(); !it.is_end();) {
        if (it->on_sample > new_roll_note.off_sample) {
            m_roll_notes[note].insert_before(it, new_roll_note);
            sync_roll(note);
            return;
        }
        if (it->on_sample == new_roll_note.on_sample && it->off_sample == new_roll_note.off_sample) {
            if (m_time >= it->on_sample && m_time <= it->off_sample)
                set_note(note, Off);
            m_roll_notes[note].remove(it);
            sync_roll(note);
            return;
        }
        if ((new_roll_note.on_sample == 0 || it->on_sample >= new_roll_note.on_sample - 1) && it->on_sample <= new_roll_note.off_sample) {
            if (m_time >= new_roll_note.off_sample && m_time <= it->off_sample)
                set_note(note, Off);
            m_roll_notes[note].remove(it);
            it = m_roll_notes[note].begin();
            continue;
        }
        if (it->on_sample < new_roll_note.on_sample && it->off_sample >= new_roll_note.on_sample) {
            if (m_time >= new_roll_note.off_sample && m_time <= it->off_sample)
                set_note(note, Off);
            it->off_sample = new_roll_note.on_sample - 1;
            ASSERT(it->length() >= 2);
        }
        ++it;
    }

    m_roll_notes[note].append(new_roll_note);
    sync_roll(note);
}

void Track::set_wave(int wave)
{
    ASSERT(wave >= first_wave && wave <= last_wave);
    m_wave = wave;
}

void Track::set_wave(Direction direction)
{
    if (direction == Up) {
        if (++m_wave > last_wave)
            m_wave = first_wave;
    } else {
        if (--m_wave < first_wave)
            m_wave = last_wave;
    }
}

void Track::set_attack(int attack)
{
    ASSERT(attack >= 0);
    m_attack = attack;
    m_attack_step = calculate_step(1, m_attack);
}

void Track::set_decay(int decay)
{
    ASSERT(decay >= 0);
    m_decay = decay;
    m_decay_step = calculate_step(1 - m_sustain_level, m_decay);
}

void Track::set_sustain_impl(int sustain)
{
    ASSERT(sustain >= 0);
    m_sustain = sustain;
    m_sustain_level = sustain / 1000.0;
}

void Track::set_sustain(int sustain)
{
    set_sustain_impl(sustain);
    set_decay(m_decay);
}

void Track::set_release(int release)
{
    ASSERT(release >= 0);
    m_release = release;
}

void Track::set_delay(int delay)
{
    ASSERT(delay >= 0);
    m_delay = delay;
    m_delay_samples = m_delay == 0 ? 0 : (sample_rate / (beats_per_minute / 60)) / m_delay;
    m_delay_buffer.resize(m_delay_samples);
    memset(m_delay_buffer.data(), 0, m_delay_buffer.size() * sizeof(Sample));
    m_delay_index = 0;
}
