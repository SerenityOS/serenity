/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AmpWidget.h"

#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

AmpWidget::AmpWidget()
{
    m_skin.load_from_file("/res/skins/base-2.91.wsz");

    m_win_button = add<AmpButton>(m_skin, AmpButton::Type::Window);
    m_win_button->move_by({ 6, 3 });

    m_minimize_button = add<AmpButton>(m_skin, AmpButton::Type::Minimize);
    m_minimize_button->move_by({ 245, 3 });

    m_shade_button = add<AmpButton>(m_skin, AmpButton::Type::Shade);
    m_shade_button->move_by({ 254, 3 });

    m_close_button = add<AmpButton>(m_skin, AmpButton::Type::Close);
    m_close_button->move_by({ 263, 3 });

    m_prev_button = add<AmpButton>(m_skin, AmpButton::Type::Previous);
    m_prev_button->move_by({ 16, 88 });

    m_play_button = add<AmpButton>(m_skin, AmpButton::Type::Play);
    m_play_button->move_by({ 38, 88 });

    m_pause_button = add<AmpButton>(m_skin, AmpButton::Type::Pause);
    m_pause_button->move_by({ 60, 88 });

    m_stop_button = add<AmpButton>(m_skin, AmpButton::Type::Stop);
    m_stop_button->move_by({ 82, 88 });

    m_next_button = add<AmpButton>(m_skin, AmpButton::Type::Next);
    m_next_button->move_by({ 104, 88 });

    m_open_button = add<AmpButton>(m_skin, AmpButton::Type::Eject);
    m_open_button->move_by({ 130, 89 });

    m_pos_slider = add<AmpSlider>(m_skin, AmpSlider::Type::Position);
    m_pos_slider->move_by({ 16, 72 });
    m_pos_slider->set_range(0, 100);

    m_vol_slider = add<AmpSlider>(m_skin, AmpSlider::Type::Volume);
    m_vol_slider->move_by({ 107, 56 });
    m_vol_slider->set_range(0, 100);

    m_bal_slider = add<AmpSlider>(m_skin, AmpSlider::Type::Balance);
    m_bal_slider->move_by({ 177, 56 });
    m_bal_slider->set_range(0, 100);
    m_bal_slider->set_value(50);

    m_eq_button = add<AmpToggleButton>(m_skin, AmpToggleButton::Type::Equalizer);
    m_eq_button->move_by({ 219, 56 });

    m_pl_button = add<AmpToggleButton>(m_skin, AmpToggleButton::Type::Playlist);
    m_pl_button->move_by({ 243, 56 });

    m_shuffle_button = add<AmpToggleButton>(m_skin, AmpToggleButton::Type::Shuffle);
    m_shuffle_button->move_by({ 164, 90 });

    m_repeat_button = add<AmpToggleButton>(m_skin, AmpToggleButton::Type::Repeat);
    m_repeat_button->move_by({ 211, 90 });

    m_time_display = add<AmpTimeDisplay>(m_skin);
    m_time_display->move_by({ 50, 26 });

    m_time_display_blink_timer = Core::Timer::create_repeating(500, [&]() {
        m_time_display->set_digits_visible(!m_time_display->digits_visible());
    });

    // Also needs:
    // - Song name display

    // Maybe needs:
    // - Context menu to open song, select skin, about, etc
}

void AmpWidget::mousedown_event(GUI::MouseEvent& event)
{
    m_mouse_down_pos = event.position();
}

void AmpWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Left) {
        auto window = static_cast<GUI::Window*>(parent());
        window->move_to(window->x() + event.x() - m_mouse_down_pos.x(), window->y() + event.y() - m_mouse_down_pos.y());
    }
}

void AmpWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    // Draw Background
    painter.blit({ 0, 0 }, *m_skin.main(), m_skin.main()->rect());

    // Draw Titlebar
    if (static_cast<const GUI::Window*>(parent())->is_active())
        painter.blit({ 0, 0 }, *m_skin.titlebar(), { 27, 0, 302, 14 });
    else
        painter.blit({ 0, 0 }, *m_skin.titlebar(), { 27, 15, 302, 14 });

    // Draw Stereo / Mono lights
    // FIXME: Dim both if not playing
    if (m_is_stereo) {
        painter.blit({ 240, 40 }, *m_skin.monoster(), { 0, 0, 29, 12 });
        painter.blit({ 211, 40 }, *m_skin.monoster(), { 29, 12, 29, 12 });
    } else {
        painter.blit({ 240, 40 }, *m_skin.monoster(), { 0, 12, 29, 12 });
        painter.blit({ 211, 40 }, *m_skin.monoster(), { 29, 0, 29, 12 });
    }

    switch (m_play_state) {
    case Player::PlayState::Playing:
        painter.blit({ 20, 26 }, *m_skin.playpaus(), { 0, 0, 9, 9 });
        break;
    case Player::PlayState::Paused:
        painter.blit({ 20, 26 }, *m_skin.playpaus(), { 9, 0, 9, 9 });
        break;
    case Player::PlayState::Stopped:
        painter.blit({ 20, 26 }, *m_skin.playpaus(), { 18, 0, 9, 9 });
        break;
    case Player::PlayState::NoFileLoaded:
        painter.blit({ 20, 26 }, *m_skin.playpaus(), { 27, 0, 9, 9 });
        break;
    }
}

void AmpWidget::set_visualization_widget(RefPtr<VisualizationWidget> widget)
{
    if (m_visualization)
        m_visualization->remove_from_parent();

    m_visualization = widget;
    m_visualization->set_relative_rect({ 0, 0, 77, 17 });
    m_visualization->move_by({ 24, 42 });

    m_visualization->set_frame_shadow(Gfx::FrameShadow::Plain);
    m_visualization->set_frame_shape(Gfx::FrameShape::NoFrame);
    m_visualization->set_frame_thickness(0);

    update();
}

void AmpWidget::set_is_stereo(bool stereo)
{
    if (m_is_stereo == stereo)
        return;
    m_is_stereo = stereo;
    update();
}

void AmpWidget::set_play_state(Player::PlayState state)
{
    if (state == m_play_state)
        return;
    m_play_state = state;
    update();
}
