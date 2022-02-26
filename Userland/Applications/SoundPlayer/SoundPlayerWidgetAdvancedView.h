/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Common.h"
#include "PlaybackManager.h"
#include "Player.h"
#include "VisualizationWidget.h"
#include <AK/NonnullRefPtr.h>
#include <LibAudio/ConnectionFromClient.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Widget.h>

class SoundPlayerWidgetAdvancedView final : public GUI::Widget
    , public Player {
    C_OBJECT(SoundPlayerWidgetAdvancedView)

public:
    void set_nonlinear_volume_slider(bool nonlinear);
    void set_playlist_visible(bool visible);

    template<typename T>
    void set_visualization()
    {
        m_visualization->remove_from_parent();
        update();
        auto new_visualization = T::construct();
        m_player_view->insert_child_before(new_visualization, *static_cast<Core::Object*>(m_playback_progress_slider.ptr()));
        m_visualization = new_visualization;
    }

    virtual void play_state_changed(PlayState) override;
    virtual void loop_mode_changed(LoopMode) override;
    virtual void shuffle_mode_changed(ShuffleMode) override;
    virtual void time_elapsed(int) override;
    virtual void file_name_changed(StringView) override;
    virtual void playlist_loaded(StringView, bool) override;
    virtual void audio_load_error(StringView path, StringView error_reason) override;
    virtual void volume_changed(double) override;
    virtual void mute_changed(bool) override;
    virtual void total_samples_changed(int) override;
    virtual void sound_buffer_played(RefPtr<Audio::Buffer>, int sample_rate, int samples_played) override;

protected:
    void keydown_event(GUI::KeyEvent&) override;

private:
    SoundPlayerWidgetAdvancedView(GUI::Window&, Audio::ConnectionFromClient&);

    void sync_previous_next_buttons();

    void drop_event(GUI::DropEvent& event) override;
    GUI::Window& m_window;

    RefPtr<GUI::HorizontalSplitter> m_splitter;
    RefPtr<GUI::Widget> m_player_view;
    RefPtr<PlaylistWidget> m_playlist_widget;
    RefPtr<VisualizationWidget> m_visualization;

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
    RefPtr<GUI::HorizontalSlider> m_volume_slider;
    RefPtr<GUI::Label> m_timestamp_label;

    bool m_nonlinear_volume_slider;
};
