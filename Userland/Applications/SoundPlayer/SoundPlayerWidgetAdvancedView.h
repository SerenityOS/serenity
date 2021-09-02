/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

protected:
    void keydown_event(GUI::KeyEvent&) override;

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
    size_t m_device_sample_rate { 44100 };
};
