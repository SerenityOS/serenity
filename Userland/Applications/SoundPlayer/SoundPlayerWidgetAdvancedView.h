/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
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

#include "BarsVisualizationWidget.h"
#include "Common.h"
#include "PlaybackManager.h"
#include "Player.h"
#include <AK/NonnullRefPtr.h>
#include <LibAudio/ClientConnection.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Widget.h>

class SoundPlayerWidgetAdvancedView final : public GUI::Widget
    , public Player {
    C_OBJECT(SoundPlayerWidgetAdvancedView)

public:
    explicit SoundPlayerWidgetAdvancedView(GUI::Window& window, PlayerState& state);
    ~SoundPlayerWidgetAdvancedView() override;

    void open_file(StringView path) override;
    void read_playlist(StringView path);
    void play() override;
    void set_nonlinear_volume_slider(bool nonlinear);
    void set_playlist_visible(bool visible);
    void try_fill_missing_info(Vector<M3UEntry>& entries, StringView playlist_p);

    template<typename T>
    void set_visualization()
    {
        m_visualization->remove_from_parent();
        update();
        auto new_visualization = T::construct();
        m_player_view->insert_child_before(new_visualization, *static_cast<Core::Object*>(m_playback_progress_slider.ptr()));
        m_visualization = new_visualization;
    }

private:
    void drop_event(GUI::DropEvent& event) override;
    GUI::Window& m_window;

    RefPtr<GUI::HorizontalSplitter> m_splitter;
    RefPtr<GUI::Widget> m_player_view;
    RefPtr<PlaylistWidget> m_playlist_widget;
    RefPtr<GUI::Widget> m_visualization;

    RefPtr<Gfx::Bitmap> m_play_icon;
    RefPtr<Gfx::Bitmap> m_pause_icon;
    RefPtr<Gfx::Bitmap> m_stop_icon;
    RefPtr<Gfx::Bitmap> m_back_icon;
    RefPtr<Gfx::Bitmap> m_next_icon;

    RefPtr<GUI::Button> m_play_button;
    RefPtr<GUI::Button> m_stop_button;
    RefPtr<GUI::Button> m_back_button;
    RefPtr<GUI::Button> m_next_button;
    RefPtr<AutoSlider> m_playback_progress_slider;
    RefPtr<GUI::Label> m_volume_label;

    bool m_nonlinear_volume_slider;
};
