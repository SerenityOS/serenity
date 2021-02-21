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

#include "TypingTutorWidget.h"

TypingTutorWidget::TypingTutorWidget()
{
    set_fill_with_background_color(true);

    set_fixed_size({ TypingTutorCanvasWidget::s_width, TypingTutorCanvasWidget::s_height + 23 });

    set_layout<GUI::VerticalBoxLayout>();
    m_canvas = add<TypingTutorCanvasWidget>();

    auto& data_widget = add<GUI::Widget>();

    data_widget.set_layout<GUI::HorizontalBoxLayout>();
    data_widget.set_fixed_height(20);
    auto& text_input = data_widget.add<GUI::TextBox>();
    text_input.set_fixed_width(120);

    m_score_label = data_widget.add<GUI::Label>();
    m_cpm_label = data_widget.add<GUI::Label>();
    m_lives_label = data_widget.add<GUI::Label>();

    text_input.on_return_pressed = [&]() {
        if (m_canvas->delete_word_if_correct(text_input.text()))
            text_input.clear();
        else
            text_input.select_all();
    };

    start_timer(1000);
}

TypingTutorWidget::~TypingTutorWidget()
{
}

void TypingTutorWidget::game_over()
{
    if (m_canvas->is_game_over()) {
        stop_timer();
        auto result = GUI::MessageBox::show(window(), "Game Over! Do you want to restart?", window()->title(), GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
        if (result == GUI::MessageBox::ExecResult::ExecYes) {
            reset();
            return;
        }

        window()->close();
    }
}

void TypingTutorWidget::reset()
{
    stop_timer();
    start_timer(1000);
    m_score_label->set_text("Time: 0 s");
    m_cpm_label->set_text("Speed: 0 cpm");
    m_lives_label->set_text("Lives: 0");
    m_canvas->reset();
}

void TypingTutorWidget::timer_event(Core::TimerEvent&)
{
    m_score_label->set_text(String::formatted("Time: {} s", (size_t)m_canvas->elapsed_time()));
    m_cpm_label->set_text(String::formatted("Speed: {:.2f} cpm", m_canvas->speed()));
    m_lives_label->set_text(String::formatted("Lives: {}", m_canvas->lives()));
    game_over();
}
