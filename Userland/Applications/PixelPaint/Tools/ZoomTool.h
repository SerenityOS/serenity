/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
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
    virtual GUI::Widget* get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override { return Gfx::StandardCursor::Zoom; }

private:
    RefPtr<GUI::Widget> m_properties_widget;
    double m_sensitivity { 0.5 };
};

}
