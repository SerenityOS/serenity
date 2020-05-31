/*
 * Copyright (c) 2020, Dominic Szablewski <dominic@phoboslab.org>
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

#include "FrameWidget.h"
#include "pl_mpeg.h"

#include <AK/Assertions.h>
#include <LibAudio/ClientConnection.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>


class VideoPlayer final : public GUI::Widget {
    C_OBJECT(VideoPlayer)
public:
    virtual ~VideoPlayer() override;
    void keep_aspect_ratio(bool);
    void zoom(float);
    void fullscreen(bool fullscreen);
    void open_file(String path);
    void on_video_decode(plm_frame_t *frame);
    void on_audio_decode(plm_samples_t *samples);

private:
    VideoPlayer();

    void paint_event(GUI::PaintEvent&) override;
    void timer_event(Core::TimerEvent&) override;

    void resize_to_video_dimensions();

    class Slider final : public GUI::Slider {
        C_OBJECT(Slider)

    public:
        virtual ~Slider() override;
        Function<void(int)> on_change;
        void set_value(int value)
        {
            if (!knob_dragging())
                GUI::Slider::set_value(value);
        }

    protected:
        Slider(Orientation orientation)
            : GUI::Slider(orientation)
        {
        }

        virtual void mousemove_event(GUI::MouseEvent& event) override
        {
            if (on_change && is_enabled() && knob_dragging())
                on_change(value());

            GUI::Slider::mousemove_event(event);
        }

        virtual void mousedown_event(GUI::MouseEvent& event) override
        {
            // Let the slider handle the event first - we might start 
            // dragging here
            GUI::Slider::mousedown_event(event);

            if (is_enabled() && !knob_dragging()) {
                // Directly jump to the clicked position instead of moving
                // forward/backward by 1
                int track_pos = clamp(event.position().x() - inner_rect().x(), 0, inner_rect().width());
                float normalized_pos = (float)track_pos / (float)inner_rect().width();
                set_value(min() + (max() - min()) * normalized_pos);

                if (on_change)
                    on_change(value());
            }
        }
    };

    RefPtr<Gfx::Bitmap> m_play_icon { Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png") };
    RefPtr<Gfx::Bitmap> m_pause_icon { Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png") };

	RefPtr<GUI::Widget> m_control_widget;
    RefPtr<GUI::Button> m_play;
    RefPtr<GUI::Label> m_current_time;
    RefPtr<GUI::Label> m_total_time;
    RefPtr<Slider> m_slider;
    RefPtr<FrameWidget> m_frame_widget;

    Core::ElapsedTimer m_timer;
    RefPtr<Audio::ClientConnection> m_audio_client;

    int m_seek_msec;
    int m_last_label_time;
    bool m_paused;
    float m_zoom;
    bool m_fullscreen;

    plm_t *m_plm;
};
