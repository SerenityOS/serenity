/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AmpButton.h"

#include <LibGUI/Painter.h>

AmpButton::AmpButton(const Skin& skin, Type type)
    : m_skin(skin)
    , m_type(type)
{
    int height;

    switch (type) {
    case Type::Minimize:
        set_relative_rect({ 0, 0, 9, 9 });
        m_use_cbuttons = false;
        m_rect = { 10, 0, 9, 9 };
        m_down_rect = { 10, 10, 9, 9 };
        return;
    case Type::Shade:
        set_relative_rect({ 0, 0, 9, 9 });
        m_use_cbuttons = false;
        m_rect = { 0, 18, 9, 9 };
        m_down_rect = { 10, 18, 9, 9 };
        return;
    case Type::Close:
        set_relative_rect({ 0, 0, 9, 9 });
        m_use_cbuttons = false;
        m_rect = { 18, 0, 9, 9 };
        m_down_rect = { 18, 10, 9, 9 };
        return;
    case Type::Window:
        set_relative_rect({ 0, 0, 9, 9 });
        m_use_cbuttons = false;
        m_rect = { 0, 0, 9, 9 };
        m_down_rect = { 0, 10, 9, 9 };
        return;
    case Type::Eject:
        set_relative_rect({ 0, 0, 22, 16 });
        height = 16;
        break;
    default:
        set_relative_rect({ 0, 0, 22, 18 });
        height = 18;
        break;
    }

    m_rect = { (int)type * 23, 0, (int)type * 23 + 23, height };
    m_down_rect = { (int)type * 23, height, (int)type * 23 + 23, height };
    m_use_cbuttons = true;
}

void AmpButton::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.blit({ 0, 0 }, m_use_cbuttons ? *m_skin.cbuttons() : *m_skin.titlebar(), is_being_pressed() ? m_down_rect : m_rect);
}

