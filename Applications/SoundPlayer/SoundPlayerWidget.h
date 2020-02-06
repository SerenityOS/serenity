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

#include "PlaybackManager.h"
#include "SampleWidget.h"
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GSlider.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class SoundPlayerWidget final : public GUI::Widget {
    C_OBJECT(SoundPlayerWidget)
public:
    virtual ~SoundPlayerWidget() override;
    void open_file(String path);
    void hide_scope(bool);
    PlaybackManager& manager() { return m_manager; }

private:
    explicit SoundPlayerWidget(GUI::Window&, NonnullRefPtr<Audio::ClientConnection>);

    void update_position(const int position);
    void update_ui();
    int normalize_rate(int) const;
    int denormalize_rate(int) const;

    class Slider final : public GUI::Slider {
        C_OBJECT(Slider)
    public:
        virtual ~Slider() override;
        Function<void(int)> on_knob_released;
        void set_value(int value)
        {
            if (!knob_dragging())
                GUI::Slider::set_value(value);
        }

    protected:
        Slider(Orientation orientation, GUI::Widget* parent)
            : GUI::Slider(orientation, parent)
        {
        }

        virtual void mouseup_event(GUI::MouseEvent& event) override
        {
            if (on_knob_released && is_enabled())
                on_knob_released(value());

            GUI::Slider::mouseup_event(event);
        }
    };

    GUI::Window& m_window;
    NonnullRefPtr<Audio::ClientConnection> m_connection;
    PlaybackManager m_manager;
    float m_sample_ratio;
    RefPtr<GUI::Label> m_status;
    RefPtr<GUI::Label> m_elapsed;
    RefPtr<GUI::Label> m_remaining;
    RefPtr<Slider> m_slider;
    RefPtr<SampleWidget> m_sample_widget;
    RefPtr<GraphicsBitmap> m_play_icon { GraphicsBitmap::load_from_file("/res/icons/16x16/play.png") };
    RefPtr<GraphicsBitmap> m_pause_icon { GraphicsBitmap::load_from_file("/res/icons/16x16/pause.png") };
    RefPtr<GUI::Button> m_play;
    RefPtr<GUI::Button> m_stop;
};
