/*
 * Copyright (c) 2021, the SerenityOS Developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Palette.h>

class SelectableOverlay final : public GUI::Widget {
    C_OBJECT(SelectableOverlay)
public:
    SelectableOverlay(GUI::Window* window)
        : m_window(window)
        , m_background_color(palette().threed_highlight().with_alpha(128))
    {
        set_override_cursor(Gfx::StandardCursor::Crosshair);
    }

    virtual ~SelectableOverlay() override {};

    Gfx::IntRect region() const
    {
        return m_region;
    }

    Function<void()> callback;

private:
    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Left)
            m_anchor_point = event.position();
    };

    virtual void mousemove_event(GUI::MouseEvent& event) override
    {
        if (m_anchor_point.has_value()) {
            m_region = Gfx::IntRect::from_two_points(*m_anchor_point, event.position());
            update();
        }
    };

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Left) {
            m_window->close();
            callback();
        }
    };

    virtual void paint_event(GUI::PaintEvent&) override
    {
        GUI::Painter painter(*this);
        painter.clear_rect(m_window->rect(), Gfx::Color::Transparent);
        painter.fill_rect(m_window->rect(), m_background_color);

        if (m_region.is_empty())
            return;

        painter.clear_rect(m_region, Gfx::Color::Transparent);
    }

    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        if (event.key() == Key_Escape) {
            m_region = Gfx::IntRect();
            m_window->close();
        }
    }

    Optional<Gfx::IntPoint> m_anchor_point;
    Gfx::IntRect m_region;
    GUI::Window* m_window = nullptr;
    Gfx::Color const m_background_color;
};
