/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AmpToggleButton.h"

#include <LibGUI/Painter.h>

AmpToggleButton::AmpToggleButton(const Skin& skin, Type type)
    : m_skin(skin)
{
    switch (type) {
    case Type::Equalizer:
        set_relative_rect({ 0, 0, 23, 12 });
        m_up_unchecked_rect = { 0, 61, 23, 12 };
        m_up_checked_rect = { 0, 73, 23, 12 };
        m_down_unchecked_rect = { 46, 61, 23, 12 };
        m_down_checked_rect = { 46, 73, 23, 12 };
        break;
    case Type::Playlist:
        set_relative_rect({ 0, 0, 23, 12 });
        m_up_unchecked_rect = { 24, 61, 23, 12 };
        m_up_checked_rect = { 24, 73, 23, 12 };
        m_down_unchecked_rect = { 69, 61, 23, 12 };
        m_down_checked_rect = { 69, 73, 23, 12 };
        break;
    case Type::Repeat:
        set_relative_rect({ 0, 0, 28, 15 });
        m_up_unchecked_rect = { 0, 0, 28, 15 };
        m_up_checked_rect = { 0, 30, 28, 15 };
        m_down_unchecked_rect = { 0, 15, 28, 15 };
        m_down_checked_rect = { 0, 45, 28, 15 };
        break;
    case Type::Shuffle:
        set_relative_rect({ 0, 0, 47, 15 });
        m_up_unchecked_rect = { 28, 0, 47, 15 };
        m_up_checked_rect = { 28, 30, 47, 15 };
        m_down_unchecked_rect = { 28, 16, 47, 15 };
        m_down_checked_rect = { 28, 45, 47, 15 };
        break;
    }
}

void AmpToggleButton::mousedown_event(GUI::MouseEvent& event)
{
    m_mouse_down = true;
    CheckBox::mousedown_event(event);
}

void AmpToggleButton::mouseup_event(GUI::MouseEvent& event)
{
    m_mouse_down = false;
    CheckBox::mouseup_event(event);
}

void AmpToggleButton::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    auto which_rect = [&]() {
        if (m_mouse_down) {
            return is_checked() ? m_down_checked_rect : m_down_unchecked_rect;
        }
        return is_checked() ? m_up_checked_rect : m_up_unchecked_rect;
    };
    painter.blit({ 0, 0 }, *m_skin.shufrep(), which_rect());
}
