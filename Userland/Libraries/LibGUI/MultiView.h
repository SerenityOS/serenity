/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Action.h>
#include <LibGUI/ColumnsView.h>
#include <LibGUI/IconView.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>

namespace GUI {

class MultiView final : public GUI::StackWidget {
    C_OBJECT(MultiView)
public:
    virtual ~MultiView() override;

    Function<void()> on_selection_change;
    Function<void(ModelIndex const&)> on_activation;
    Function<void(ModelIndex const&)> on_selection;
    Function<void(ModelIndex const&, ContextMenuEvent const&)> on_context_menu_request;
    Function<void(ModelIndex const&, DropEvent const&)> on_drop;

    enum ViewMode {
        Invalid,
        Table,
        Columns,
        Icon
    };
    void set_view_mode(ViewMode);
    ViewMode view_mode() const { return m_view_mode; }

    int model_column() const { return m_model_column; }
    void set_model_column(int);

    void set_column_visible(int column_index, bool visible);

    void set_key_column_and_sort_order(int column, SortOrder);

    GUI::AbstractView& current_view()
    {
        switch (m_view_mode) {
        case ViewMode::Table:
            return *m_table_view;
        case ViewMode::Columns:
            return *m_columns_view;
        case ViewMode::Icon:
            return *m_icon_view;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ModelSelection const& selection() const { return const_cast<MultiView&>(*this).current_view().selection(); }
    ModelSelection& selection() { return current_view().selection(); }

    template<typename Callback>
    void for_each_view_implementation(Callback callback)
    {
        callback(*m_table_view);
        callback(*m_icon_view);
        callback(*m_columns_view);
    }

    Model* model() { return m_model; }
    Model const* model() const { return m_model; }

    void set_model(RefPtr<Model>);

    Action& view_as_table_action() { return *m_view_as_table_action; }
    Action& view_as_icons_action() { return *m_view_as_icons_action; }
    Action& view_as_columns_action() { return *m_view_as_columns_action; }

    AbstractView::SelectionMode selection_mode() const;
    void set_selection_mode(AbstractView::SelectionMode);

private:
    MultiView();

    void build_actions();

    ViewMode m_view_mode { Invalid };
    int m_model_column { 0 };

    RefPtr<Model> m_model;

    RefPtr<TableView> m_table_view;
    RefPtr<IconView> m_icon_view;
    RefPtr<ColumnsView> m_columns_view;

    RefPtr<Action> m_view_as_table_action;
    RefPtr<Action> m_view_as_icons_action;
    RefPtr<Action> m_view_as_columns_action;

    OwnPtr<ActionGroup> m_view_type_action_group;
};

}
