/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "PianoWidget.h"
#include <AK/Queue.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GPainter.h>
#include <math.h>

PianoWidget::PianoWidget()
{
    set_font(Font::default_fixed_width_font());
}

PianoWidget::~PianoWidget()
{
}

void PianoWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), Color::Black);

    render_wave(painter);
    render_piano(painter);
    render_knobs(painter);
    render_roll(painter);
}

void PianoWidget::fill_audio_buffer(uint8_t* stream, int len)
{
    if (++m_time == m_tick) {
        m_time = 0;
        change_roll_column();
    }

    m_sample_count = len / sizeof(Sample);
    memset(stream, 0, len);

    auto* sst = (Sample*)stream;
    for (int i = 0; i < m_sample_count; ++i) {
        static const double volume = 1800;
        for (size_t n = 0; n < (sizeof(m_note_on) / sizeof(u8)); ++n) {
            if (!m_note_on[n])
                continue;
            double val = 0;
            switch (m_wave_type) {
            case WaveType::Sine:
                val = ((volume * m_power[n]) * w_sine(n));
                break;
            case WaveType::Saw:
                val = ((volume * m_power[n]) * w_saw(n));
                break;
            case WaveType::Square:
                val = ((volume * m_power[n]) * w_square(n));
                break;
            case WaveType::Triangle:
                val = ((volume * m_power[n]) * w_triangle(n));
                break;
            case WaveType::Noise:
                val = ((volume * m_power[n]) * w_noise());
                break;
            }
            sst[i].left += val;
        }
        sst[i].right = sst[i].left;
    }

    // Decay pressed notes.
    if (m_decay_enabled) {
        for (size_t n = 0; n < (sizeof(m_note_on) / sizeof(u8)); ++n) {
            if (m_note_on[n])
                m_power[n] *= 0.965;
        }
    }

    static Queue<Sample*> delay_frames;
    static const int delay_length_in_frames = m_tick * 4;

    if (m_delay_enabled) {
        if (delay_frames.size() >= delay_length_in_frames) {
            auto* to_blend = delay_frames.dequeue();
            for (int i = 0; i < m_sample_count; ++i) {
                sst[i].left += to_blend[i].left * 0.333333;
                sst[i].right += to_blend[i].right * 0.333333;
            }
            delete[] to_blend;
        }
        Sample* frame = new Sample[m_sample_count];
        memcpy(frame, sst, m_sample_count * sizeof(Sample));

        delay_frames.enqueue(frame);
    }

    ASSERT(len <= 2048 * (int)sizeof(Sample));
    memcpy(m_back_buffer, (Sample*)stream, len);
    swap(m_front_buffer, m_back_buffer);
}

double PianoWidget::w_sine(size_t n)
{
    double pos = note_frequency[n] / 44100.0;
    double sin_step = pos * 2 * M_PI;
    double w = sin(m_sin_pos[n]);
    m_sin_pos[n] += sin_step;
    return w;
}

static inline double hax_floor(double t)
{
    return (int)t;
}

double PianoWidget::w_saw(size_t n)
{
    double saw_step = note_frequency[n] / 44100.0;
    double t = m_saw_pos[n];
    double w = (0.5 - (t - hax_floor(t))) * 2;
    //printf("w: %g, step: %g\n", w, saw_step);
    m_saw_pos[n] += saw_step;
    return w;
}

double PianoWidget::w_square(size_t n)
{
    double pos = note_frequency[n] / 44100.0;
    double square_step = pos * 2 * M_PI;
    double w = sin(m_square_pos[n]);
    if (w > 0)
        w = 1;
    else
        w = -1;
    //printf("w: %g, step: %g\n", w, square_step);
    m_square_pos[n] += square_step;
    return w;
}

double PianoWidget::w_triangle(size_t n)
{
    double triangle_step = note_frequency[n] / 44100.0;
    double t = m_triangle_pos[n];
    double w = fabs(fmod((4 * t) + 1, 4) - 2) - 1;
    m_triangle_pos[n] += triangle_step;
    return w;
}

double PianoWidget::w_noise()
{
    return (((double)rand() / RAND_MAX) * 2.0) - 1.0;
}

int PianoWidget::octave_base() const
{
    return (m_octave - m_octave_min) * 12;
}

struct KeyDefinition {
    int index;
    PianoKey piano_key;
    String label;
    KeyCode key_code;
};

const KeyDefinition key_definitions[] = {
    { 0, K_C1, "A", KeyCode::Key_A },
    { 1, K_D1, "S", KeyCode::Key_S },
    { 2, K_E1, "D", KeyCode::Key_D },
    { 3, K_F1, "F", KeyCode::Key_F },
    { 4, K_G1, "G", KeyCode::Key_G },
    { 5, K_A1, "H", KeyCode::Key_H },
    { 6, K_B1, "J", KeyCode::Key_J },
    { 7, K_C2, "K", KeyCode::Key_K },
    { 8, K_D2, "L", KeyCode::Key_L },
    { 9, K_E2, ";", KeyCode::Key_Semicolon },
    { 10, K_F2, "'", KeyCode::Key_Apostrophe },
    { 11, K_G2, "r", KeyCode::Key_Return },
    { 0, K_Db1, "W", KeyCode::Key_W },
    { 1, K_Eb1, "E", KeyCode::Key_E },
    { 3, K_Gb1, "T", KeyCode::Key_T },
    { 4, K_Ab1, "Y", KeyCode::Key_Y },
    { 5, K_Bb1, "U", KeyCode::Key_U },
    { 7, K_Db2, "O", KeyCode::Key_O },
    { 8, K_Eb2, "P", KeyCode::Key_P },
    { 10, K_Gb2, "]", KeyCode::Key_RightBracket },
};

void PianoWidget::note(KeyCode key_code, SwitchNote switch_note)
{
    for (auto& kd : key_definitions) {
        if (kd.key_code == key_code) {
            note(kd.piano_key, switch_note);
            return;
        }
    }
}

void PianoWidget::note(PianoKey piano_key, SwitchNote switch_note)
{
    int n = octave_base() + piano_key;

    if (switch_note == On) {
        if (m_note_on[n] == 0) {
            m_sin_pos[n] = 0;
            m_square_pos[n] = 0;
            m_saw_pos[n] = 0;
            m_triangle_pos[n] = 0;
        }
        ++m_note_on[n];
        m_power[n] = 1;
    } else {
        if (m_note_on[n] > 1) {
            --m_note_on[n];
        } else if (m_note_on[n] == 1) {
            --m_note_on[n];
            m_power[n] = 0;
        }
    }
}

void PianoWidget::keydown_event(GKeyEvent& event)
{
    if (keys[event.key()])
        return;
    keys[event.key()] = true;

    switch (event.key()) {
    case KeyCode::Key_C:

        if (++m_wave_type == InvalidWave)
            m_wave_type = 0;
        break;
    case KeyCode::Key_V:
        m_delay_enabled = !m_delay_enabled;
        break;
    case KeyCode::Key_B:
        m_decay_enabled = !m_decay_enabled;
        break;
    case KeyCode::Key_Z:
        if (m_octave > m_octave_min)
            --m_octave;
        memset(m_note_on, 0, sizeof(m_note_on));
        break;
    case KeyCode::Key_X:
        if (m_octave < m_octave_max)
            ++m_octave;
        memset(m_note_on, 0, sizeof(m_note_on));
        break;
    default:
        note((KeyCode)event.key(), On);
    }

    update();
}

void PianoWidget::keyup_event(GKeyEvent& event)
{
    keys[event.key()] = false;
    note((KeyCode)event.key(), Off);
    update();
}

void PianoWidget::mousedown_event(GMouseEvent& event)
{
    m_mouse_pressed = true;

    m_piano_key_under_mouse = find_key_for_relative_position(event.x() - x(), event.y() - y());
    if (m_piano_key_under_mouse) {
        note(m_piano_key_under_mouse, On);
        update();
        return;
    }

    RollNote* roll_note_under_mouse = find_roll_note_for_relative_position(event.x() - x(), event.y() - y());
    if (roll_note_under_mouse)
        roll_note_under_mouse->pressed = !roll_note_under_mouse->pressed;
    update();
}

void PianoWidget::mouseup_event(GMouseEvent&)
{
    m_mouse_pressed = false;

    note(m_piano_key_under_mouse, Off);
    update();
}

void PianoWidget::mousemove_event(GMouseEvent& event)
{
    if (!m_mouse_pressed)
        return;

    PianoKey mouse_was_over = m_piano_key_under_mouse;

    m_piano_key_under_mouse = find_key_for_relative_position(event.x() - x(), event.y() - y());

    if (m_piano_key_under_mouse == mouse_was_over)
        return;

    if (mouse_was_over)
        note(mouse_was_over, Off);
    if (m_piano_key_under_mouse)
        note(m_piano_key_under_mouse, On);
    update();
}

void PianoWidget::render_wave(GPainter& painter)
{
    Color wave_color;
    switch (m_wave_type) {
    case WaveType::Sine:
        wave_color = Color(255, 192, 0);
        break;
    case WaveType::Saw:
        wave_color = Color(240, 100, 128);
        break;
    case WaveType::Square:
        wave_color = Color(128, 160, 255);
        break;
    case WaveType::Triangle:
        wave_color = Color(35, 171, 35);
        break;
    case WaveType::Noise:
        wave_color = Color(197, 214, 225);
        break;
    }

    int prev_x = 0;
    int prev_y = m_height / 2;
    for (int x = 0; x < m_sample_count; ++x) {
        double val = m_front_buffer[x].left;
        val /= 32768;
        val *= m_height;
        int y = ((m_height / 8) - 8) + val;
        if (x == 0)
            painter.set_pixel({ x, y }, wave_color);
        else
            painter.draw_line({ prev_x, prev_y }, { x, y }, wave_color);
        prev_x = x;
        prev_y = y;
    }
}

static int white_key_width = 22;
static int white_key_height = 60;
static int black_key_width = 16;
static int black_key_height = 35;
static int black_key_stride = white_key_width - black_key_width;
static int black_key_offset = white_key_width - black_key_width / 2;

Rect PianoWidget::define_piano_key_rect(int index, PianoKey n) const
{
    Rect rect;
    int stride = 0;
    int offset = 0;
    if (is_white(n)) {
        rect.set_width(white_key_width);
        rect.set_height(white_key_height);
    } else {
        rect.set_width(black_key_width);
        rect.set_height(black_key_height);
        stride = black_key_stride;
        offset = black_key_offset;
    }
    rect.set_x(offset + index * rect.width() + (index * stride));
    rect.set_y(m_height - white_key_height);
    return rect;
}

PianoKey PianoWidget::find_key_for_relative_position(int x, int y) const
{
    // here we iterate backwards because we want to try to match the black
    // keys first, which are defined last
    for (int i = (sizeof(key_definitions) / sizeof(KeyDefinition)) - 1; i >= 0; i--) {
        auto& kd = key_definitions[i];

        auto rect = define_piano_key_rect(kd.index, kd.piano_key);

        if (rect.contains(x, y))
            return kd.piano_key;
    }

    return K_None;
}

void PianoWidget::render_piano_key(GPainter& painter, int index, PianoKey n, const StringView& text)
{
    Color color;
    if (m_note_on[octave_base() + n]) {
        color = Color(64, 64, 255);
    } else {
        if (is_white(n))
            color = Color::White;
        else
            color = Color::Black;
    }

    auto rect = define_piano_key_rect(index, n);

    painter.fill_rect(rect, color);
    painter.draw_rect(rect, Color::Black);

    Color text_color;
    if (is_white(n)) {
        text_color = Color::Black;
    } else {
        text_color = Color::White;
    }
    Rect r(rect.x(), rect.y() + rect.height() / 2, rect.width(), rect.height() / 2);
    painter.draw_text(r, text, TextAlignment::Center, text_color);
}

void PianoWidget::render_piano(GPainter& painter)
{
    for (auto& kd : key_definitions)
        render_piano_key(painter, kd.index, kd.piano_key, kd.label);
}

static int knob_width = 100;

void PianoWidget::render_knob(GPainter& painter, const Rect& rect, bool state, const StringView& text)
{
    Color text_color;
    if (state) {
        painter.fill_rect(rect, Color(0, 200, 0));
        text_color = Color::Black;
    } else {
        painter.draw_rect(rect, Color(180, 0, 0));
        text_color = Color(180, 0, 0);
    }
    painter.draw_text(rect, text, TextAlignment::Center, text_color);
}

void PianoWidget::render_knobs(GPainter& painter)
{
    Rect delay_knob_rect(m_width - knob_width - 16, m_height - 50, knob_width, 16);
    render_knob(painter, delay_knob_rect, m_delay_enabled, "V: Delay   ");

    Rect decay_knob_rect(m_width - knob_width - 16, m_height - 30, knob_width, 16);
    render_knob(painter, decay_knob_rect, m_decay_enabled, "B: Decay   ");

    Rect octave_knob_rect(m_width - knob_width - 16 - knob_width - 16, m_height - 50, knob_width, 16);
    auto text = String::format("Z/X: Oct %d ", m_octave);
    int oct_rgb_step = 255 / (m_octave_max + 4);
    int oshade = (m_octave + 4) * oct_rgb_step;
    painter.draw_rect(octave_knob_rect, Color(oshade, oshade, oshade));
    painter.draw_text(octave_knob_rect, text, TextAlignment::Center, Color(oshade, oshade, oshade));

    Rect wave_knob_rect(m_width - knob_width - 16 - knob_width - 16, m_height - 30, knob_width, 16);
    switch (m_wave_type) {
    case WaveType::Sine:
        painter.draw_rect(wave_knob_rect, Color(255, 192, 0));
        painter.draw_text(wave_knob_rect, "C: Sine    ", TextAlignment::Center, Color(255, 192, 0));
        break;
    case WaveType::Saw:
        painter.draw_rect(wave_knob_rect, Color(240, 100, 128));
        painter.draw_text(wave_knob_rect, "C: Sawtooth", TextAlignment::Center, Color(240, 100, 128));
        break;
    case WaveType::Square:
        painter.draw_rect(wave_knob_rect, Color(128, 160, 255));
        painter.draw_text(wave_knob_rect, "C: Square  ", TextAlignment::Center, Color(128, 160, 255));
        break;
    case WaveType::Triangle:
        painter.draw_rect(wave_knob_rect, Color(35, 171, 35));
        painter.draw_text(wave_knob_rect, "C: Triangle", TextAlignment::Center, Color(35, 171, 35));
        break;
    case WaveType::Noise:
        painter.draw_rect(wave_knob_rect, Color(197, 214, 225));
        painter.draw_text(wave_knob_rect, "C: Noise   ", TextAlignment::Center, Color(197, 214, 225));
        break;
    }
}

static int roll_columns = 32;
static int roll_rows = 20;
static int roll_note_size = 512 / roll_columns;
static int roll_height = roll_note_size * roll_rows;
static int roll_y = 512 - white_key_height - roll_height - 16;

Rect PianoWidget::define_roll_note_rect(int column, int row) const
{
    Rect rect;
    rect.set_width(roll_note_size);
    rect.set_height(roll_note_size);
    rect.set_x(column * roll_note_size);
    rect.set_y(roll_y + (row * roll_note_size));

    return rect;
}

PianoWidget::RollNote* PianoWidget::find_roll_note_for_relative_position(int x, int y)
{
    for (int row = 0; row < roll_rows; ++row) {
        for (int column = 0; column < roll_columns; ++column) {
            auto rect = define_roll_note_rect(column, row);
            if (rect.contains(x, y))
                return &m_roll_notes[row][column];
        }
    }

    return nullptr;
}

void PianoWidget::render_roll_note(GPainter& painter, int column, int row, PianoKey key)
{
    Color color;
    auto roll_note = m_roll_notes[row][column];
    if (roll_note.pressed) {
        if (roll_note.playing)
            color = Color(24, 24, 255);
        else
            color = Color(64, 64, 255);
    } else {
        if (roll_note.playing)
            color = Color(104, 104, 255);
        else
            color = is_white(key) ? Color::White : Color::MidGray;
    }

    auto rect = define_roll_note_rect(column, row);

    painter.fill_rect(rect, color);
    painter.draw_rect(rect, Color::Black);
}

void PianoWidget::render_roll(GPainter& painter)
{
    for (int row = 0; row < roll_rows; ++row) {
        PianoKey key = (PianoKey)(roll_rows - row);
        for (int column = 0; column < roll_columns; ++column)
            render_roll_note(painter, column, row, key);
    }
}

void PianoWidget::change_roll_column()
{
    static int current_column = 0;
    static int previous_column = roll_columns - 1;

    for (int row = 0; row < roll_rows; ++row) {
        m_roll_notes[row][previous_column].playing = false;
        if (m_roll_notes[row][previous_column].pressed)
            note((PianoKey)(roll_rows - row), Off);

        m_roll_notes[row][current_column].playing = true;
        if (m_roll_notes[row][current_column].pressed)
            note((PianoKey)(roll_rows - row), On);
    }

    if (++current_column == roll_columns)
        current_column = 0;
    if (++previous_column == roll_columns)
        previous_column = 0;

    update();
}

void PianoWidget::custom_event(CCustomEvent&)
{
    update();
}
