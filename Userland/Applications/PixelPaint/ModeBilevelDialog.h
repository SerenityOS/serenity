/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>
#include <LibGfx/ImageFormats/BilevelImage.h>

namespace PixelPaint {

class ModeBilevelDialog final : public GUI::Dialog {
    C_OBJECT(ModeBilevelDialog);

public:
    Gfx::DitheringAlgorithm dithering_algorithm() const { return m_dithering_algorithm; }

private:
    explicit ModeBilevelDialog(GUI::Window* parent_window);

    Gfx::DitheringAlgorithm m_dithering_algorithm { Gfx::DitheringAlgorithm::FloydSteinberg };
};

}
