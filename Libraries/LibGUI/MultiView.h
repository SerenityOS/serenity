/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    void refresh();

    Function<void()> on_selection_change;
    Function<void(const ModelIndex&)> on_activation;
    Function<void(const ModelIndex&)> on_selection;
    Function<void(const ModelIndex&, const ContextMenuEvent&)> on_context_menu_request;
    Function<void(const ModelIndex&, const DropEvent&)> on_drop;

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

    void set_column_hidden(int column_index, bool hidden);

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
            ASSERT_NOT_REACHED();
        }
    }

    const ModelSelection& selection() const { return const_cast<MultiView&>(*this).current_view().selection(); }
    ModelSelection& selection() { return current_view().selection(); }

    template<typename Callback>
    void for_each_view_implementation(Callback callback)
    {
        callback(*m_table_view);
        callback(*m_icon_view);
        callback(*m_columns_view);
    }

    Model* model() { return m_model; }
    const Model* model() const { return m_model; }

    void set_model(RefPtr<Model>);

    Action& view_as_table_action() { return *m_view_as_table_action; }
    Action& view_as_icons_action() { return *m_view_as_icons_action; }
    Action& view_as_columns_action() { return *m_view_as_columns_action; }

    bool is_multi_select() const { return m_multi_select; }
    void set_multi_select(bool);

private:
    MultiView();

    void build_actions();
    void apply_multi_select();

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

    bool m_multi_select { true };
};

}
