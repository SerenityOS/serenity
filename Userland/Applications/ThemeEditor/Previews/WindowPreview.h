/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace ThemeEditor {

namespace Previews {

class WindowPreview : public GUI::Frame {
    C_OBJECT_ABSTRACT(WindowPreview)

public:
    static ErrorOr<NonnullRefPtr<WindowPreview>> try_create();
    ErrorOr<void> initialize();

    void set_preview_palette(Gfx::Palette& palette);

    virtual ~WindowPreview() override = default;

private:
    WindowPreview() = default;
};

}
}
