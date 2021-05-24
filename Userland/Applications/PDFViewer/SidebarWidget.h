/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "OutlineModel.h"
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>

class SidebarWidget final : public GUI::Widget {
    C_OBJECT(SidebarWidget)

public:
    ~SidebarWidget() override = default;

    void set_outline(RefPtr<PDF::OutlineDict> outline)
    {
        if (outline) {
            m_model = OutlineModel::create(outline.release_nonnull());
            m_outline_tree_view->set_model(m_model);
        } else {
            m_model = RefPtr<OutlineModel> {};
            m_outline_tree_view->set_model({});
        }
    }

private:
    SidebarWidget();

    RefPtr<OutlineModel> m_model;
    RefPtr<GUI::TreeView> m_outline_tree_view;
};
