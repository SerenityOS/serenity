/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
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

    virtual void on_mousedown(Layer*, MouseEvent&) override;

    virtual GUI::Widget* get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override { return Gfx::StandardCursor::Eyedropper; }

private:
    RefPtr<GUI::Widget> m_properties_widget;
    bool m_sample_all_layers { false };
};

}
