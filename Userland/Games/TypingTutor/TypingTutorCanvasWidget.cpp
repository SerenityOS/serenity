/*
 * Copyright (c) 2021, Davide Carella <carelladavide1@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this
 *        list of conditions and the following disclaimer.
 *
 *        2. Redistributions in binary form must reproduce the above copyright notice,
 *        this list of conditions and the following disclaimer in the documentation
 *        and/or other materials provided with the distribution.
 *
 *        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *        AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *        IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *        FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *        DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *                 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "TypingTutorCanvasWidget.h"

static int random_between(int min, int max)
{
    return rand() % (max - min + 1) + min;
}

TypingTutorCanvasWidget::TypingTutorCanvasWidget()
{
    srand(time(NULL));

    set_fixed_size({ s_width, s_height });

    set_font(Gfx::FontDatabase::default_bold_fixed_width_font());

    set_wordlist_path("/res/wordlists/English.txt");

    reset();
}

TypingTutorCanvasWidget::~TypingTutorCanvasWidget()
{
}

void TypingTutorCanvasWidget::set_wordlist_path(const String& path)
{
    if (m_wordlist_path == path)
        return;

    m_wordlist_path = path;
    m_wordlist_loaded = false;
}

void TypingTutorCanvasWidget::load_wordlist()
{
    if (m_wordlist_loaded)
        return;

    m_wordlist.clear();
    auto file_or_error = Core::File::open(wordlist_path(), Core::IODevice::OpenMode::ReadOnly);
    ASSERT(!file_or_error.is_error());

    auto file = file_or_error.release_value();
    while (!file->eof()) {
        auto line = file->read_line();
        if (line.is_empty() || line.is_whitespace())
            continue;

        m_wordlist.append(file->read_line());
    }

    file->close();
    m_wordlist_loaded = true;
}

void TypingTutorCanvasWidget::generate_new_wave()
{
    u8 glyph_height = font().glyph_height();
    u8 max_wave_size = s_height / glyph_height - 1;

    for (size_t i = 0; i < m_current_wave_size; ++i) {
        Word word = {
            .x = (float)random_between(0, 50),
            .y = random_between(1, max_wave_size),
            .velocity = (float)random_between(3 * m_max_velocity / 4, m_max_velocity) / s_frame_rate,
            .value = m_wordlist[random_between(0, m_wordlist.size() - 1)],
        };

        for (size_t j = 0; j < m_wave.size(); ++j) {
            while (word.value == m_wave[j].value)
                word.value = m_wordlist[random_between(0, m_wordlist.size() - 1)];

            if (word.y == m_wave[j].y) {
                size_t word_width = font().width(word.value);
                if (word.x + word_width >= m_wave[j].x) {
                    float random_offset = word.x;
                    word.x = m_wave[j].x - word_width;
                    word.x -= random_offset + 10;
                }

                if (word.velocity > m_wave[j].velocity)
                    word.velocity = m_wave[j].velocity;
            }
        }

        m_wave.append(word);
    }
}

bool TypingTutorCanvasWidget::delete_word_if_correct(String attempt)
{
    for (int i = m_wave.size() - 1; i >= 0; --i) {
        if (m_wave[i].value == attempt) {
            m_total_characters_wrote += attempt.length();
            m_wave.remove(i);
            return true;
        }
    }

    return false;
}

float TypingTutorCanvasWidget::speed()
{
    float t = elapsed_time() / 60;
    if (t == 0)
        return 0;

    return m_total_characters_wrote / t;
}

float TypingTutorCanvasWidget::elapsed_time()
{
    return (float)m_frame_count / s_frame_rate;
}

void TypingTutorCanvasWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Black);

    String wave_info = String::formatted("[ Next wave in: {} s ]", m_next_wave_countdown);
    int text_width = font().width(wave_info);
    u8 glyph_height = font().glyph_height();
    Gfx::IntRect wave_info_rect((int)s_width / 2 - text_width / 2, 0, text_width, glyph_height);
    painter.draw_text(wave_info_rect, wave_info, Gfx::TextAlignment::TopLeft, Color::White);
    painter.draw_line({ 0, (int)glyph_height / 2 }, { (int)s_width / 2 - text_width / 2, glyph_height / 2 }, Color::White);
    painter.draw_line({ (int)s_width / 2 + (int)text_width / 2, (int)glyph_height / 2 }, { s_width, glyph_height / 2 }, Color::White);

    painter.fill_rect({ 0, glyph_height, s_width, s_height }, Color::from_rgb(0x2631ad));

    for (size_t i = 0; i < m_wave.size(); ++i)
        draw_word(m_wave[i], painter);
}

void TypingTutorCanvasWidget::draw_word(const Word& word, Gfx::Painter& painter)
{
    size_t glyph_height = font().glyph_height();
    Gfx::IntRect word_rect((int)word.x, word.y * glyph_height, font().width(word.value), glyph_height);

    Color color = Color::White;
    if (word.x >= (int)(3 * s_width / 4))
        color = Color::from_rgb(0xeb281a);
    else if (word.x >= (int)s_width / 2)
        color = Color::from_rgb(0xeb831a);

    painter.draw_text(word_rect, word.value, Gfx::TextAlignment::TopLeft, color);
}

void TypingTutorCanvasWidget::reset()
{
    stop_timer();
    load_wordlist();
    m_wave.clear();
    m_frame_count = 0;
    m_current_wave_size = 5;
    m_wave_frequency = 15;
    m_next_wave_countdown = 0;
    m_max_velocity = 14;
    m_total_characters_wrote = 0;
    m_lives = 3;
    start_timer(1000 / s_frame_rate);
}

void TypingTutorCanvasWidget::timer_event(Core::TimerEvent&)
{
    if (is_game_over())
        stop_timer();

    if (m_frame_count % s_frame_rate == 0)
        --m_next_wave_countdown;

    if (m_wave.is_empty())
        m_next_wave_countdown = 0;

    if (m_next_wave_countdown == 0) {
        generate_new_wave();
        m_next_wave_countdown = m_wave_frequency;
    }

    if (m_frame_count % (s_frame_rate * s_increase_difficulty_frequency) == 0 && m_current_wave_size < (s_height - 10) / font().glyph_height() && m_wave_frequency > 5) {
        --m_wave_frequency;
        ++m_current_wave_size;
        ++m_max_velocity;
    }

    for (int i = m_wave.size() - 1; i >= 0; --i) {
        m_wave[i].x += m_wave[i].velocity;
        if (m_wave[i].x >= s_width) {
            m_wave.remove(i);
            --m_lives;
        }
    }

    ++m_frame_count;

    update();
}
