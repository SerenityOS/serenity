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

#pragma once

#include "Music.h"
#include <LibGUI/GWidget.h>

class GPainter;

class PianoWidget final : public GWidget {
    C_OBJECT(PianoWidget)
public:
    virtual ~PianoWidget() override;

    void fill_audio_buffer(uint8_t* stream, int len);

private:
    PianoWidget();
    virtual void paint_event(GPaintEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void keyup_event(GKeyEvent&) override;
    virtual void custom_event(CCustomEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;

    double w_sine(size_t);
    double w_saw(size_t);
    double w_square(size_t);
    double w_triangle(size_t);
    double w_noise();

    struct RollNote {
        bool pressed;
        bool playing;
    };

    Rect define_piano_key_rect(int index, PianoKey) const;
    PianoKey find_key_for_relative_position(int x, int y) const;
    Rect define_roll_note_rect(int column, int row) const;
    RollNote* find_roll_note_for_relative_position(int x, int y);

    void render_wave(GPainter&);
    void render_piano_key(GPainter&, int index, PianoKey, const StringView&);
    void render_piano(GPainter&);
    void render_knobs(GPainter&);
    void render_knob(GPainter&, const Rect&, bool state, const StringView&);
    void render_roll_note(GPainter&, int column, int row, PianoKey);
    void render_roll(GPainter&);

    void change_roll_column();

    enum SwitchNote {
        Off,
        On
    };
    void note(KeyCode, SwitchNote);
    void note(PianoKey, SwitchNote);

    int octave_base() const;

    int m_sample_count { 0 };
    Sample m_front[2048] { 0, 0 };
    Sample m_back[2048] { 0, 0 };
    Sample* m_front_buffer { m_front };
    Sample* m_back_buffer { m_back };

#define note_count sizeof(note_frequency) / sizeof(double)

    u8 m_note_on[note_count] { 0 };
    double m_power[note_count];
    double m_sin_pos[note_count];
    double m_square_pos[note_count];
    double m_saw_pos[note_count];
    double m_triangle_pos[note_count];

    int m_octave_min { 1 };
    int m_octave_max { 6 };
    int m_octave { 4 };

    int m_width { 512 };
    int m_height { 512 };

    int m_wave_type { 0 };
    bool m_delay_enabled { false };
    bool m_decay_enabled { false };

    bool keys[256] { false };

    PianoKey m_piano_key_under_mouse { K_None };
    bool m_mouse_pressed { false };

    RollNote m_roll_notes[20][32] { { false, false } };

    int m_time { 0 };
    int m_tick { 10 };
};
