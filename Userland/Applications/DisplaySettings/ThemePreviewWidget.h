/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/AbstractThemePreview.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/WindowTheme.h>

namespace DisplaySettings {

class ThemePreviewWidget final : public GUI::AbstractThemePreview {
    C_OBJECT(ThemePreviewWidget);

public:
    ErrorOr<void> set_theme(String path);

private:
    explicit ThemePreviewWidget(Gfx::Palette const& palette);

    void paint_preview(GUI::PaintEvent& event) override;
};

}
