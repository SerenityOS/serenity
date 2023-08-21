/*
 * Copyright (c) 2022, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class LevelsDialog final : public GUI::Dialog {
    C_OBJECT(LevelsDialog);

public:
    void revert_possible_changes();

private:
    LevelsDialog(GUI::Window* parent_window, ImageEditor*);

    ImageEditor* m_editor { nullptr };
    RefPtr<Gfx::Bitmap> m_reference_bitmap { nullptr };
    RefPtr<GUI::ValueSlider> m_brightness_slider = { nullptr };
    RefPtr<GUI::ValueSlider> m_contrast_slider = { nullptr };
    RefPtr<GUI::ValueSlider> m_gamma_slider = { nullptr };
    bool m_did_change = false;
    int m_precomputed_color_correction[256];
    Optional<Gfx::IntRect> m_masked_area;

    ErrorOr<void> ensure_reference_bitmap();
    void generate_new_image();
    void cleanup_resources();
    void generate_precomputed_color_correction();
};

}
