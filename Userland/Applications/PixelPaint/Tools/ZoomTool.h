/*
 * Copyright (c) 2021-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class ZoomTool final : public Tool {
public:
    ZoomTool() = default;
    virtual ~ZoomTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Zoom; }

private:
    virtual StringView tool_name() const override { return "Zoom Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;
    float m_sensitivity { 0.5f };
};

}
