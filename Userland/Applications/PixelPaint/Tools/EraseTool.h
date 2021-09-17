/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BrushTool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class EraseTool final : public BrushTool {
public:
    EraseTool();
    virtual ~EraseTool() override;

    virtual GUI::Widget* get_properties_widget() override;

protected:
    virtual Color color_for(GUI::MouseEvent const& event) override;
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point) override;

private:
    RefPtr<GUI::Widget> m_properties_widget;

    enum class DrawMode {
        Pencil,
        Brush,
    };
    DrawMode m_draw_mode { DrawMode::Brush };
    bool m_use_secondary_color { false };
};

}
