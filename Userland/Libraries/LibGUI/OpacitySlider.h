/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractSlider.h>

namespace GUI {

class OpacitySlider : public AbstractSlider {
    C_OBJECT(OpacitySlider);

public:
    virtual ~OpacitySlider() override;

protected:
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;

private:
    explicit OpacitySlider(Gfx::Orientation = Gfx::Orientation::Horizontal);

    Gfx::IntRect frame_inner_rect() const;

    int value_at(const Gfx::IntPoint&) const;

    bool m_dragging { false };
};

}
