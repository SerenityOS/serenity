/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BrushTool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class PenTool final : public BrushTool {
public:
    PenTool();
    virtual ~PenTool() override = default;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;

protected:
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point) override;
    virtual void draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end) override;

private:
    virtual StringView tool_name() const override { return "Pen Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;
};

}
