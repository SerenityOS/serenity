/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AmpSlider.h"

#include <LibGUI/Painter.h>

AmpSlider::AmpSlider(const Skin& skin, AmpSlider::Type type)
    : AutoSlider(Gfx::Orientation::Horizontal)
    , m_skin(skin)
    , m_type(type)
{
    set_knob_size_mode(GUI::Slider::KnobSizeMode::Fixed);

    switch (type) {
    case Type::Position:
        set_relative_rect({ 0, 0, 248, 10 });
        m_knob_rect = { 248, 0, 29, 10 };
        m_knob_down_rect = { 278, 0, 29, 10 };
        m_knob_size = 29;
        break;
    case Type::Volume:
        set_relative_rect({ 0, 0, 64, 11 });
        m_knob_rect = { 15, 422, 14, 16 };
        m_knob_down_rect = { 0, 422, 14, 16 };
        m_knob_size = 14;
        break;
    case Type::Balance:
        set_relative_rect({ 0, 0, 37, 14 });
        m_knob_rect = { 15, 422, 14, 14 };
        m_knob_down_rect = { 0, 422, 14, 14 };
        m_knob_size = 14;
        break;
    }
}

void AmpSlider::mousedown_event(GUI::MouseEvent& event)
{
    AutoSlider::mousedown_event(event);
    update();
}

void AmpSlider::mouseup_event(GUI::MouseEvent& event)
{
    AutoSlider::mouseup_event(event);
    update();
}

void AmpSlider::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    switch (m_type) {
    case Type::Position:
        painter.blit({ 0, 0 }, *m_skin.posbar(), rect());
        painter.blit({ knob_rect().x(), 0 }, *m_skin.posbar(), mouse_is_down() ? m_knob_down_rect : m_knob_rect);
        break;
    case Type::Volume: {
        float percent = (float)value() / (max() - min());
        int sprite = percent * 28;
        int offset = (sprite - 1) * 15;
        painter.blit({ 0, 0 }, *m_skin.volume(), rect().translated(0, offset < 0 ? 0 : offset));
        painter.blit({ knob_rect().x(), 0 }, *m_skin.volume(), mouse_is_down() ? m_knob_down_rect : m_knob_rect);
        break;
    }
    case Type::Balance: {
        float percent = (float)value() / (max() - min());
        int sprite = percent <= 0.5f ? (1.0f - percent) * 27 : percent * 27;
        int offset = sprite * 15;
        painter.blit({ 0, 0 }, *m_skin.balance(), rect().translated(9, offset < 0 ? 0 : offset));
        painter.blit({ knob_rect().x(), 0 }, *m_skin.balance(), mouse_is_down() ? m_knob_down_rect : m_knob_rect);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}
