/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "OutlineModel.h"
#include "ThumbnailsListView.h"
#include <LibGUI/ModelIndex.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <LibPDF/Document.h>

class SidebarWidget final : public GUI::Widget {
    C_OBJECT(SidebarWidget)

public:
    ~SidebarWidget() override = default;

    Function<void(PDF::Destination const&)> on_destination_selected;

    ErrorOr<void> set_outline(RefPtr<PDF::OutlineDict> outline)
    {
        if (outline) {
            m_model = TRY(OutlineModel::create(outline.release_nonnull()));
            m_outline_tree_view->set_model(m_model);
        } else {
            m_model = RefPtr<OutlineModel> {};
            m_outline_tree_view->set_model({});
        }
        return {};
    }

    RefPtr<ThumbnailsListView> thumbnails_list_view() { return m_thumbnails_list_view; }

private:
    SidebarWidget();

    RefPtr<OutlineModel> m_model;
    RefPtr<GUI::TreeView> m_outline_tree_view;
    RefPtr<ThumbnailsListView> m_thumbnails_list_view;
};
