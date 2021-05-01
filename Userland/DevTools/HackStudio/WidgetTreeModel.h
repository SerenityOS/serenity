/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>

namespace HackStudio {

class WidgetTreeModel final : public GUI::Model {
public:
    static NonnullRefPtr<WidgetTreeModel> create(GUI::Widget& root) { return adopt_ref(*new WidgetTreeModel(root)); }
    virtual ~WidgetTreeModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;
    virtual void update() override;

    GUI::ModelIndex index_for_widget(GUI::Widget&) const;

private:
    explicit WidgetTreeModel(GUI::Widget&);

    NonnullRefPtr<GUI::Widget> m_root;
    GUI::Icon m_widget_icon;
};

}
