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
#include <LibGUI/ItemView.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TableView.h>

//#define MULTIVIEW_WITH_COLUMNSVIEW

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
        List,
        Columns,
        Icon
    };
    void set_view_mode(ViewMode);
    ViewMode view_mode() const { return m_view_mode; }

    int model_column() const { return m_model_column; }
    void set_model_column(int);

    void set_column_hidden(int column_index, bool hidden);

    GUI::AbstractView& current_view()
    {
        switch (m_view_mode) {
        case ViewMode::List:
            return *m_table_view;
#ifdef MULTIVIEW_WITH_COLUMNSVIEW
        case ViewMode::Columns:
            return *m_columns_view;
#endif
        case ViewMode::Icon:
            return *m_item_view;
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
        callback(*m_item_view);
#ifdef MULTIVIEW_WITH_COLUMNSVIEW
        callback(*m_columns_view);
#endif
    }

    Model* model() { return m_model; }
    const Model* model() const { return m_model; }

    void set_model(RefPtr<Model>);

    Action& view_as_table_action() { return *m_view_as_table_action; }
    Action& view_as_icons_action() { return *m_view_as_icons_action; }
#ifdef MULTIVIEW_WITH_COLUMNSVIEW
    Action& view_as_columns_action() { return *m_view_as_columns_action; }
#endif

private:
    MultiView();

    void build_actions();

    ViewMode m_view_mode { Icon };
    int m_model_column { 0 };

    RefPtr<Model> m_model;

    RefPtr<TableView> m_table_view;
    RefPtr<ItemView> m_item_view;
#ifdef MULTIVIEW_WITH_COLUMNSVIEW
    RefPtr<ColumnsView> m_columns_view;
#endif

    RefPtr<Action> m_view_as_table_action;
    RefPtr<Action> m_view_as_icons_action;
#ifdef MULTIVIEW_WITH_COLUMNSVIEW
    RefPtr<Action> m_view_as_columns_action;
#endif

    OwnPtr<ActionGroup> m_view_type_action_group;
};

}
