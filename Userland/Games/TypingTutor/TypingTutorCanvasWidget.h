/*
 * Copyright (c) 2021, Davide Carella <carelladavide1@gmail.com>.
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

#include <LibCore/File.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <stdio.h>
#include <time.h>

class TypingTutorCanvasWidget final : public GUI::Widget {
    C_OBJECT(TypingTutorCanvasWidget);

public:
    static constexpr size_t s_width = 500;
    static constexpr size_t s_height = 510;

    static constexpr u8 s_frame_rate = 60;
    static constexpr u8 s_increase_difficulty_frequency = 60;

    virtual ~TypingTutorCanvasWidget() override;

    struct Word {
        float x { 0 };
        int y { 0 };
        float velocity { 0 };
        StringView value;
    };

    const String& wordlist_path() { return m_wordlist_path; }
    void set_wordlist_path(const String& path);

    bool delete_word_if_correct(String attempt);

    float speed();
    float elapsed_time();
    u8 lives() { return m_lives; }

    bool is_game_over() { return m_lives <= 0; }
    void reset();

private:
    String m_wordlist_path;
    bool m_wordlist_loaded;

    Vector<Word> m_wave;
    Vector<String> m_wordlist;
    size_t m_frame_count;
    u8 m_current_wave_size;
    u8 m_wave_frequency;
    u8 m_next_wave_countdown;
    u8 m_max_velocity;
    size_t m_total_characters_wrote;
    u8 m_lives;

    TypingTutorCanvasWidget();

    void load_wordlist();

    void generate_new_wave();
    void draw_word(const Word&, Gfx::Painter&);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
};
