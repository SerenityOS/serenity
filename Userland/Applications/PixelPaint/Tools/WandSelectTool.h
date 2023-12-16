/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Selection.h"
#include "Tool.h"

#include <AK/Vector.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class WandSelectTool final : public Tool {
public:
    WandSelectTool() = default;
    virtual ~WandSelectTool() = default;

    virtual void on_mousedown(Layer*, MouseEvent& event) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    virtual StringView tool_name() const override { return "Wand Select Tool"sv; }

    int m_threshold { 0 };
    RefPtr<GUI::Widget> m_properties_widget;
    Vector<ByteString> m_merge_mode_names {};
    Selection::MergeMode m_merge_mode { Selection::MergeMode::Set };
};

}
