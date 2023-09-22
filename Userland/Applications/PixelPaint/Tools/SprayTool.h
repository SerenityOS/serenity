/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibCore/Timer.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Painter.h>

namespace PixelPaint {

class SprayTool final : public Tool {
public:
    SprayTool();
    virtual ~SprayTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    virtual StringView tool_name() const override { return "Spray Tool"sv; }

    void paint_it();

    RefPtr<GUI::Widget> m_properties_widget;
    RefPtr<Core::Timer> m_timer;
    Gfx::IntPoint m_last_pos;
    Color m_color;
    int m_thickness { 10 };
    int m_density { 40 };
};

}
