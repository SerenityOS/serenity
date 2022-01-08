/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filters/Filter.h"
#include "ImageEditor.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class FilterGallery final : public GUI::Dialog {
    C_OBJECT(FilterGallery);

private:
    FilterGallery(GUI::Window* parent_window, ImageEditor* editor);
    void restore_layer_bitmap(Layer* active_layer);
    ImageEditor* m_editor { nullptr };
    GUI::TreeView* m_filter_tree { nullptr };
    GUI::Widget* m_config_widget { nullptr };
    RefPtr<GUI::Widget> m_selected_filter_config_widget { nullptr };
    Filter* m_selected_filter { nullptr };
    RefPtr<Gfx::Bitmap> m_preview_bitmap { nullptr };
};

}
