/*
 * Copyright (c) 2022, Andrew Smith <andrew@alsmith.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>
#include <LibGfx/ScalingMode.h>

namespace PixelPaint {

class ResizeImageDialog final : public GUI::Dialog {
    C_OBJECT(ResizeImageDialog);

public:
    Gfx::IntSize desired_size() const { return m_desired_size; }
    Gfx::ScalingMode scaling_mode() const { return m_scaling_mode; }
    bool should_rescale() const { return m_rescale_image; }

private:
    ResizeImageDialog(Gfx::IntSize starting_size, GUI::Window* parent_window);

    Gfx::IntSize m_desired_size;
    Gfx::ScalingMode m_scaling_mode;
    float m_starting_aspect_ratio;
    bool m_rescale_image;
};

}
