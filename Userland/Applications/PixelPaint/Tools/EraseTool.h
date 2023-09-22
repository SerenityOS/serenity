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
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class EraseTool final : public BrushTool {
public:
    EraseTool() = default;
    virtual ~EraseTool() override = default;

    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;

protected:
    virtual Color color_for(GUI::MouseEvent const& event) override;
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point) override;
    virtual NonnullRefPtr<Gfx::Bitmap> build_cursor() override;
    virtual float preferred_cursor_size() override;

private:
    virtual StringView tool_name() const override { return "Erase Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;

    enum class DrawMode {
        Pencil,
        Brush,
    };
    DrawMode m_draw_mode { DrawMode::Pencil };
    bool m_use_secondary_color { false };
};

}
