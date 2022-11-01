/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filters/Filter.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

class FilterPreviewWidget final : public GUI::Frame {
    C_OBJECT(FilterPreviewWidget);

public:
    virtual ~FilterPreviewWidget() override;
    void set_layer(RefPtr<Layer> layer);
    void set_bitmap(RefPtr<Gfx::Bitmap> const& bitmap);
    void set_filter(Filter* filter);
    void clear_filter();

private:
    explicit FilterPreviewWidget();

    RefPtr<Layer> m_layer;
    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<Gfx::Bitmap> m_filtered_bitmap;

    virtual void paint_event(GUI::PaintEvent&) override;
};

}
