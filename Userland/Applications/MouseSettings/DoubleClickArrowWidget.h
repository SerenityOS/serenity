/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mathias Jakobsen <mathias@jbcoding.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Widget.h>

namespace MouseSettings {

class DoubleClickArrowWidget final : public GUI::Widget {
    C_OBJECT(DoubleClickArrowWidget);

public:
    virtual ~DoubleClickArrowWidget() override = default;
    void set_double_click_speed(int);

private:
    DoubleClickArrowWidget();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    RefPtr<Gfx::Bitmap> m_arrow_bitmap;
    int m_double_click_speed { 0 };
    bool m_inverted { false };
    Core::ElapsedTimer m_double_click_timer;
};

}
