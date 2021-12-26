/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace MouseSettings {

class DoubleClickArrowWidget final : public GUI::Widget {
    C_OBJECT(DoubleClickArrowWidget);

public:
    virtual ~DoubleClickArrowWidget() override;
    void set_double_click_speed(int);

private:
    DoubleClickArrowWidget();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;

    RefPtr<Gfx::Bitmap> m_arrow_bitmap;
    int m_double_click_speed { 0 };
    bool m_inverted { false };
};

}
