/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterGallery.h"
#include "FilterModel.h"
#include <Applications/PixelPaint/FilterGalleryGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

FilterGallery::FilterGallery(GUI::Window* parent_window, ImageEditor* editor)
    : GUI::Dialog(parent_window)
{
    set_title("Filter Gallery");
    set_icon(parent_window->icon());
    resize(400, 250);
    set_resizable(true);

    auto& main_widget = set_main_widget<GUI::Widget>();
    if (!main_widget.load_from_gml(filter_gallery_gml))
        VERIFY_NOT_REACHED();

    auto filter_tree = main_widget.find_descendant_of_type_named<GUI::TreeView>("tree_view");
    auto apply_button = main_widget.find_descendant_of_type_named<GUI::Button>("apply_button");
    auto cancel_button = main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    auto config_widget = main_widget.find_descendant_of_type_named<GUI::Widget>("config_widget");

    VERIFY(filter_tree);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(config_widget);

    auto filter_model = FilterModel::create(editor);
    filter_tree->set_model(filter_model);
    filter_tree->expand_tree();

    apply_button->on_click = [this, filter_tree](auto) {
        auto selected_index = filter_tree->selection().first();
        if (!selected_index.is_valid())
            done(ExecResult::ExecAborted);

        auto selected_filter = static_cast<const FilterModel::FilterInfo*>(selected_index.internal_data());
        if (selected_filter->type != FilterModel::FilterInfo::Type::Filter)
            done(ExecResult::ExecAborted);

        selected_filter->apply_filter();
        done(ExecResult::ExecOK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };
}

}
