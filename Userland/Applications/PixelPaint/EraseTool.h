/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class EraseTool final : public Tool {
public:
    EraseTool();
    virtual ~EraseTool() override;

    virtual void on_mousedown(Layer*, MouseEvent& event) override;
    virtual void on_mousemove(Layer*, MouseEvent& event) override;
    virtual void on_mouseup(Layer*, MouseEvent& event) override;
    virtual GUI::Widget* get_properties_widget() override;

private:
    Gfx::Color get_color() const;
    Gfx::IntRect build_rect(Gfx::IntPoint const& pos, Gfx::IntRect const& widget_rect);
    RefPtr<GUI::Widget> m_properties_widget;

    bool m_use_secondary_color { false };
    int m_thickness { 1 };
};

}
