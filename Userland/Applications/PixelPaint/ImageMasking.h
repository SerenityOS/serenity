/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Dialog.h>
#include <LibGUI/RangeSlider.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class ImageMasking final : public GUI::Dialog {
    C_OBJECT(ImageMasking);

public:
    void revert_possible_changes();

private:
    ImageMasking(GUI::Window* parent_window, ImageEditor*);

    ImageEditor* m_editor { nullptr };
    RefPtr<Gfx::Bitmap> m_reference_mask { nullptr };
    bool m_did_change = false;

    RefPtr<GUI::RangeSlider> m_full_masking_slider = { nullptr };
    RefPtr<GUI::RangeSlider> m_edge_masking_slider = { nullptr };

    ErrorOr<void> ensure_reference_mask();
    void generate_new_mask();
    void cleanup_resources();
};

class RangeIllustrationWidget final : public GUI::Widget {
    C_OBJECT(RangeIllustrationWidget)
public:
    virtual ~RangeIllustrationWidget() override = default;

protected:
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    RangeIllustrationWidget(RefPtr<GUI::RangeSlider> edge_mask_values, RefPtr<GUI::RangeSlider> full_mask_values)
    {
        m_edge_mask_values = edge_mask_values;
        m_full_mask_values = full_mask_values;
    }
    RefPtr<GUI::RangeSlider> m_edge_mask_values;
    RefPtr<GUI::RangeSlider> m_full_mask_values;
};

}
