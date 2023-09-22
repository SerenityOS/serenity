/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class PickerTool final : public Tool {
public:
    PickerTool() = default;
    virtual ~PickerTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;

    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Eyedropper; }

private:
    virtual StringView tool_name() const override { return "Picker Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;
    bool m_sample_all_layers { false };
};

}
