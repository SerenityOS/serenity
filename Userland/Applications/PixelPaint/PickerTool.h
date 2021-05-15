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

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
};

}
