/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FilterPreviewWidget.h"
#include "Filters/Filter.h"
#include "ImageEditor.h"
#include <LibGUI/Dialog.h>
#include <LibGUI/Label.h>

namespace PixelPaint {

class FilterGallery final : public GUI::Dialog {
    C_OBJECT(FilterGallery);

private:
    FilterGallery(GUI::Window* parent_window, ImageEditor*);
    GUI::TreeView* m_filter_tree { nullptr };
    GUI::Widget* m_config_widget { nullptr };
    FilterPreviewWidget* m_preview_widget { nullptr };
    RefPtr<GUI::Label> m_error_label { nullptr };
    RefPtr<GUI::Widget> m_selected_filter_config_widget { nullptr };
    Filter* m_selected_filter { nullptr };
};

}
