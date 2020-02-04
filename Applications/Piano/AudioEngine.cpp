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
}

AudioEngine::~AudioEngine()
{
}

void AudioEngine::fill_buffer(FixedArray<Sample>& buffer)
{
    memset(buffer.data(), 0, buffer_size);

    for (size_t i = 0; i < buffer.size(); ++i) {
        for (size_t note = 0; note < note_count; ++note) {
            if (!m_note_on[note])
                continue;
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

    if (m_decay) {
        for (size_t note = 0; note < note_count; ++note) {
            if (m_note_on[note]) {
                m_power[note] -= m_decay / 100.0;
                if (m_power[note] < 0)
                    m_power[note] = 0;
            }
        }
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

    if (++m_time == m_tick)
        m_time = 0;

    memcpy(m_back_buffer_ptr->data(), buffer.data(), buffer_size);
    swap(m_front_buffer_ptr, m_back_buffer_ptr);
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

void AudioEngine::set_note(int note, Switch switch_note)
{
    ASSERT(note >= 0 && note < note_count);

    if (switch_note == On) {
        if (m_note_on[note] == 0) {
            m_pos[note] = 0;
            m_power[note] = 1;
        }
        ++m_note_on[note];
    } else {
        if (m_note_on[note] >= 1) {
            if (m_note_on[note] == 1)
                m_power[note] = 0;
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

void AudioEngine::set_decay(int decay)
{
    ASSERT(decay >= 0);
    m_decay = decay;
}

void AudioEngine::set_delay(int delay)
{
    ASSERT(delay >= 0);
    m_delay_buffers.clear();
    m_delay = delay;
}
