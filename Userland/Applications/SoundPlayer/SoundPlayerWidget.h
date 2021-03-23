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

#include "Common.h"
#include "PlaybackManager.h"
#include "Player.h"
#include "SampleWidget.h"
#include <AK/NonnullRefPtr.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class SoundPlayerWidget final : public GUI::Widget
    , public Player {
    C_OBJECT(SoundPlayerWidget)
public:
    ~SoundPlayerWidget() override;
    void open_file(StringView path) override;
    void play() override;
    void hide_scope(bool);

private:
    explicit SoundPlayerWidget(GUI::Window& window, PlayerState& state);

    void drop_event(GUI::DropEvent&) override;

    void update_position(const int position);
    void update_ui();
    int normalize_rate(int) const;
    int denormalize_rate(int) const;

    GUI::Window& m_window;

    float m_sample_ratio { 1.0 };
    RefPtr<GUI::Label> m_status;
    RefPtr<GUI::Label> m_elapsed;
    RefPtr<GUI::Label> m_remaining;
    RefPtr<Slider> m_slider;
    RefPtr<SampleWidget> m_sample_widget;
    RefPtr<Gfx::Bitmap> m_play_icon { Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png") };
    RefPtr<Gfx::Bitmap> m_pause_icon { Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png") };
    RefPtr<GUI::Button> m_play;
    RefPtr<GUI::Button> m_stop;
};
