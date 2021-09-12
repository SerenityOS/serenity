/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class ZoomTool final : public Tool {
public:
    ZoomTool();
    virtual ~ZoomTool() override;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;
    virtual Gfx::StandardCursor cursor() override { return Gfx::StandardCursor::Zoom; }

private:
    RefPtr<GUI::Widget> m_properties_widget;
    double m_sensitivity { 0.5 };
};

}
