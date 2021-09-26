/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AmpButton.h"
#include "AmpSlider.h"
#include "AmpTimeDisplay.h"
#include "AmpToggleButton.h"
#include "Player.h"
#include "Skin.h"
#include "VisualizationWidget.h"
#include <LibGUI/Widget.h>

class AmpWidget : public GUI::Widget {
    C_OBJECT(AmpWidget)
public:
    void set_play_state(Player::PlayState);

    void set_is_stereo(bool);
    bool is_stereo() const { return m_is_stereo; }

protected:
    explicit AmpWidget();

    void set_visualization_widget(RefPtr<VisualizationWidget>);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    RefPtr<AmpButton> m_win_button;
    RefPtr<AmpButton> m_minimize_button;
    RefPtr<AmpButton> m_shade_button;
    RefPtr<AmpButton> m_close_button;
    RefPtr<AmpButton> m_prev_button;
    RefPtr<AmpButton> m_play_button;
    RefPtr<AmpButton> m_pause_button;
    RefPtr<AmpButton> m_stop_button;
    RefPtr<AmpButton> m_next_button;
    RefPtr<AmpButton> m_open_button;
    RefPtr<AmpSlider> m_pos_slider;
    RefPtr<AmpSlider> m_vol_slider;
    RefPtr<AmpSlider> m_bal_slider;
    RefPtr<AmpToggleButton> m_eq_button;
    RefPtr<AmpToggleButton> m_pl_button;
    RefPtr<AmpToggleButton> m_shuffle_button;
    RefPtr<AmpToggleButton> m_repeat_button;
    RefPtr<AmpTimeDisplay> m_time_display;

    RefPtr<VisualizationWidget> m_visualization;

    RefPtr<Core::Timer> m_time_display_blink_timer;

private:
    Skin m_skin;

    Player::PlayState m_play_state { Player::PlayState::NoFileLoaded };
    Gfx::IntPoint m_mouse_down_pos;
    bool m_is_stereo;
};
