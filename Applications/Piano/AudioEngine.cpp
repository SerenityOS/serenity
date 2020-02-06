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

#include "AudioEngine.h"
#include <limits>
#include <math.h>

AudioEngine::AudioEngine()
{
    set_sustain_impl(1000);
    set_attack(5);
    set_decay(1000);
    set_release(5);
}

AudioEngine::~AudioEngine()
{
}

void AudioEngine::fill_buffer(FixedArray<Sample>& buffer)
{
    memset(buffer.data(), 0, buffer_size);

    if (m_time == 0)
        set_notes_from_roll();

    for (size_t i = 0; i < buffer.size(); ++i) {
        for (size_t note = 0; note < note_count; ++note) {
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

            double val = 0;
            switch (m_wave) {
            case Wave::Sine:
                val = (volume * m_power[note]) * sine(note);
                break;
            case Wave::Saw:
                val = (volume * m_power[note]) * saw(note);
                break;
            case Wave::Square:
                val = (volume * m_power[note]) * square(note);
                break;
            case Wave::Triangle:
                val = (volume * m_power[note]) * triangle(note);
                break;
            case Wave::Noise:
                val = (volume * m_power[note]) * noise();
                break;
            default:
                ASSERT_NOT_REACHED();
            }
            buffer[i].left += val;
        }
        buffer[i].right = buffer[i].left;
    }

    if (m_delay) {
        if (m_delay_buffers.size() >= m_delay) {
            auto to_blend = m_delay_buffers.dequeue();
            for (size_t i = 0; i < to_blend->size(); ++i) {
                buffer[i].left += (*to_blend)[i].left * 0.333333;
                buffer[i].right += (*to_blend)[i].right * 0.333333;
            }
        }

        auto delay_buffer = make<FixedArray<Sample>>(buffer.size());
        memcpy(delay_buffer->data(), buffer.data(), buffer_size);
        m_delay_buffers.enqueue(move(delay_buffer));
    }

    if (++m_time == m_tick) {
        m_time = 0;
        update_roll();
    }

    memcpy(m_back_buffer_ptr->data(), buffer.data(), buffer_size);
    swap(m_front_buffer_ptr, m_back_buffer_ptr);
}

void AudioEngine::reset()
{
    memset(m_front_buffer.data(), 0, buffer_size);
    memset(m_back_buffer.data(), 0, buffer_size);
    m_front_buffer_ptr = &m_front_buffer;
    m_back_buffer_ptr = &m_back_buffer;

    m_delay_buffers.clear();

    memset(m_note_on, 0, sizeof(m_note_on));
    memset(m_power, 0, sizeof(m_power));
    memset(m_envelope, 0, sizeof(m_envelope));

    m_time = 0;
    m_current_column = 0;
    m_previous_column = horizontal_notes - 1;
}

// All of the information for these waves is on Wikipedia.

double AudioEngine::sine(size_t note)
{
    double pos = note_frequencies[note] / sample_rate;
    double sin_step = pos * 2 * M_PI;
    double w = sin(m_pos[note]);
    m_pos[note] += sin_step;
    return w;
}

double AudioEngine::saw(size_t note)
{
    double saw_step = note_frequencies[note] / sample_rate;
    double t = m_pos[note];
    double w = (0.5 - (t - floor(t))) * 2;
    m_pos[note] += saw_step;
    return w;
}

double AudioEngine::square(size_t note)
{
    double pos = note_frequencies[note] / sample_rate;
    double square_step = pos * 2 * M_PI;
    double w = sin(m_pos[note]) >= 0 ? 1 : -1;
    m_pos[note] += square_step;
    return w;
}

double AudioEngine::triangle(size_t note)
{
    double triangle_step = note_frequencies[note] / sample_rate;
    double t = m_pos[note];
    double w = fabs(fmod((4 * t) + 1, 4) - 2) - 1;
    m_pos[note] += triangle_step;
    return w;
}

double AudioEngine::noise() const
{
    double random_percentage = static_cast<double>(rand()) / RAND_MAX;
    double w = (random_percentage * 2) - 1;
    return w;
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

void AudioEngine::set_note(int note, Switch switch_note)
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

    ASSERT(m_note_on[note] != std::numeric_limits<u8>::max());
    ASSERT(m_power[note] >= 0);
}

void AudioEngine::set_note_current_octave(int note, Switch switch_note)
{
    set_note(note + octave_base(), switch_note);
}

void AudioEngine::set_roll_note(int y, int x, Switch switch_note)
{
    ASSERT(x >= 0 && x < horizontal_notes);
    ASSERT(y >= 0 && y < note_count);

    m_roll_notes[y][x] = switch_note;

    if (x == m_current_column && switch_note == Off) // If you turn off a note that is playing.
        set_note((note_count - 1) - y, Off);
}

void AudioEngine::update_roll()
{
    if (++m_current_column == horizontal_notes)
        m_current_column = 0;
    if (++m_previous_column == horizontal_notes)
        m_previous_column = 0;
}

void AudioEngine::set_notes_from_roll()
{
    for (int note = 0; note < note_count; ++note) {
        if (m_roll_notes[note][m_previous_column] == On)
            set_note((note_count - 1) - note, Off);
        if (m_roll_notes[note][m_current_column] == On)
            set_note((note_count - 1) - note, On);
    }
}

void AudioEngine::set_octave(Direction direction)
{
    if (direction == Up) {
        if (m_octave < octave_max)
            ++m_octave;
    } else {
        if (m_octave > octave_min)
            --m_octave;
    }
}

void AudioEngine::set_wave(int wave)
{
    ASSERT(wave >= first_wave && wave <= last_wave);
    m_wave = wave;
}

void AudioEngine::set_wave(Direction direction)
{
    if (direction == Up) {
        if (++m_wave > last_wave)
            m_wave = first_wave;
    } else {
        if (--m_wave < first_wave)
            m_wave = last_wave;
    }
}

void AudioEngine::set_attack(int attack)
{
    ASSERT(attack >= 0);
    m_attack = attack;
    m_attack_step = calculate_step(1, m_attack);
}

void AudioEngine::set_decay(int decay)
{
    ASSERT(decay >= 0);
    m_decay = decay;
    m_decay_step = calculate_step(1 - m_sustain_level, m_decay);
}

void AudioEngine::set_sustain_impl(int sustain)
{
    ASSERT(sustain >= 0);
    m_sustain = sustain;
    m_sustain_level = sustain / 1000.0;
}

void AudioEngine::set_sustain(int sustain)
{
    set_sustain_impl(sustain);
    set_decay(m_decay);
}

void AudioEngine::set_release(int release)
{
    ASSERT(release >= 0);
    m_release = release;
}

void AudioEngine::set_delay(int delay)
{
    ASSERT(delay >= 0);
    m_delay_buffers.clear();
    m_delay = delay;
}
