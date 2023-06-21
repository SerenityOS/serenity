/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractSlider.h>

namespace GUI {

class OpacitySlider : public AbstractSlider {
    C_OBJECT(OpacitySlider);

public:
    virtual ~OpacitySlider() override = default;

    void set_base_color(Gfx::Color);
    Gfx::Color base_color() { return m_base_color; }

protected:
    explicit OpacitySlider(Gfx::Orientation);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;

private:
    Gfx::IntRect frame_inner_rect() const;

    Gfx::Color m_base_color { 0, 0, 0 };

    virtual Optional<UISize> calculated_min_size() const override;
    virtual Optional<UISize> calculated_preferred_size() const override;

    int value_at(Gfx::IntPoint) const;

    bool m_dragging { false };
};

class VerticalOpacitySlider final : public OpacitySlider {
    C_OBJECT(VerticalOpacitySlider);

public:
    virtual ~VerticalOpacitySlider() override = default;

private:
    VerticalOpacitySlider()
        : OpacitySlider(Orientation::Vertical)
    {
    }
};

class HorizontalOpacitySlider final : public OpacitySlider {
    C_OBJECT(HorizontalOpacitySlider);

public:
    virtual ~HorizontalOpacitySlider() override = default;

private:
    HorizontalOpacitySlider()
        : OpacitySlider(Orientation::Horizontal)
    {
    }
};

}
