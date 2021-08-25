/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class PickerTool final : public Tool {
public:
    PickerTool();
    virtual ~PickerTool() override;

    virtual void on_mousedown(Layer&, MouseEvent&) override;
    virtual Gfx::StandardCursor cursor() override { return Gfx::StandardCursor::Crosshair; }
};

}
