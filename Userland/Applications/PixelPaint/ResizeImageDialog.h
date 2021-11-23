/*
 * Copyright (c) 2021, Kamil Chojnowski <contact@diath.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace PixelPaint {

class ResizeImageDialog final : public GUI::Dialog {
    C_OBJECT(ResizeImageDialog)

public:
    Gfx::IntSize const& image_size() const { return m_image_size; }

private:
    ResizeImageDialog(Gfx::IntSize const& current_size, GUI::Window* parent_window);

    Gfx::IntSize m_image_size;
};

}
