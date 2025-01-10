/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowPreview.h"

namespace ThemeEditor {

namespace Previews {

ErrorOr<void> WindowPreview::initialize()
{
    for_each_child_widget([](auto& child) {
        child.set_focus_policy(GUI::FocusPolicy::NoFocus);
        return IterationDecision::Continue;
    });
    return {};
}

void WindowPreview::set_preview_palette(Gfx::Palette& palette)
{
    set_palette(palette);
    Function<void(GUI::Widget&)> recurse = [&](GUI::Widget& parent_widget) {
        parent_widget.for_each_child_widget([&](auto& widget) {
            widget.set_palette(palette);
            recurse(widget);
            return IterationDecision::Continue;
        });
    };
    recurse(*this);
}

}
}
