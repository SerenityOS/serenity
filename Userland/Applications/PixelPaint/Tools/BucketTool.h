/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class BucketTool final : public Tool {
public:
    BucketTool();
    virtual ~BucketTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return m_cursor; }

private:
    virtual StringView tool_name() const override { return "Bucket Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;
    int m_threshold { 0 };
    Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> m_cursor { Gfx::StandardCursor::Crosshair };
};

}
