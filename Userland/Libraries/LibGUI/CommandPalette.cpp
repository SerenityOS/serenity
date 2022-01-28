/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CommandPalette.h>
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Painter.h>

namespace GUI {

class ActionModel final : public GUI::Model {
public:
    enum Column {
        Icon,
        Text,
        Shortcut,
        __Count,
    };

    ActionModel(Vector<NonnullRefPtr<GUI::Action>>& actions)
        : m_actions(actions)
    {
    }

    virtual ~ActionModel() override { }

    virtual int row_count(ModelIndex const& parent_index) const override
    {
        if (!parent_index.is_valid())
            return m_actions.size();
        return 0;
    }

    virtual int column_count(ModelIndex const& = ModelIndex()) const override
    {
        return Column::__Count;
    }

    virtual String column_name(int) const override { return {}; }

    virtual ModelIndex index(int row, int column = 0, ModelIndex const& = ModelIndex()) const override
    {
        return create_index(row, column, m_actions.at(row).ptr());
    }

    virtual Variant data(ModelIndex const& index, ModelRole role = ModelRole::Display) const override
    {
        if (role != ModelRole::Display)
            return {};

        auto& action = *static_cast<GUI::Action*>(index.internal_data());

        switch (index.column()) {
        case Column::Icon:
            if (action.icon())
                return *action.icon();
            return "";
        case Column::Text:
            return Gfx::parse_ampersand_string(action.text());
        case Column::Shortcut:
            if (!action.shortcut().is_valid())
                return "";
            return action.shortcut().to_string();
        }

        VERIFY_NOT_REACHED();
    }

    virtual TriState data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const override
    {
        auto& action = *static_cast<GUI::Action*>(index.internal_data());
        auto text = Gfx::parse_ampersand_string(action.text());
        if (text.contains(term.as_string(), CaseSensitivity::CaseInsensitive))
            return TriState::True;
        return TriState::False;
    }

private:
    Vector<NonnullRefPtr<GUI::Action>> const& m_actions;
};

CommandPalette::CommandPalette(GUI::Window& parent_window, ScreenPosition screen_position)
    : GUI::Dialog(&parent_window, screen_position)
{
    set_frameless(true);
    resize(400, 300);

    collect_actions(parent_window);

    auto& main_widget = set_main_widget<GUI::Frame>();
    main_widget.set_frame_shadow(Gfx::FrameShadow::Raised);
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    m_text_box = main_widget.add<GUI::TextBox>();
    m_table_view = main_widget.add<GUI::TableView>();
    m_model = adopt_ref(*new ActionModel(m_actions));
    m_table_view->set_column_headers_visible(false);

    m_filter_model = MUST(GUI::FilteringProxyModel::create(*m_model));
    m_filter_model->set_filter_term("");

    m_table_view->set_model(*m_filter_model);

    m_text_box->on_change = [this] {
        m_filter_model->set_filter_term(m_text_box->text());
        if (m_filter_model->row_count() != 0)
            m_table_view->set_cursor(m_filter_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_text_box->on_down_pressed = [this] {
        m_table_view->move_cursor(GUI::AbstractView::CursorMovement::Down, GUI::AbstractView::SelectionUpdate::Set);
    };

    m_text_box->on_up_pressed = [this] {
        m_table_view->move_cursor(GUI::AbstractView::CursorMovement::Up, GUI::AbstractView::SelectionUpdate::Set);
    };

    m_text_box->on_return_pressed = [this] {
        if (!m_table_view->selection().is_empty())
            finish_with_index(m_table_view->selection().first());
    };

    m_table_view->on_activation = [this](GUI::ModelIndex const& filter_index) {
        finish_with_index(filter_index);
    };

    m_text_box->set_focus(true);
}

CommandPalette::~CommandPalette()
{
}

void CommandPalette::collect_actions(GUI::Window& parent_window)
{
    OrderedHashTable<NonnullRefPtr<GUI::Action>> actions;

    auto collect_action_children = [&](Core::Object& action_parent) {
        action_parent.for_each_child_of_type<GUI::Action>([&](GUI::Action& action) {
            if (action.is_enabled())
                actions.set(action);
            return IterationDecision::Continue;
        });
    };

    for (auto* widget = parent_window.focused_widget(); widget; widget = widget->parent_widget())
        collect_action_children(*widget);

    collect_action_children(parent_window);

    if (!parent_window.is_modal()) {
        for (auto const& it : GUI::Application::the()->global_shortcut_actions({})) {
            if (it.value->is_enabled())
                actions.set(*it.value);
        }
    }

    m_actions.clear();
    for (auto& action : actions)
        m_actions.append(action);

    quick_sort(m_actions, [&](auto& a, auto& b) {
        // FIXME: This is so awkward. Don't be so awkward.
        return Gfx::parse_ampersand_string(a->text()) < Gfx::parse_ampersand_string(b->text());
    });
}

void CommandPalette::finish_with_index(GUI::ModelIndex const& filter_index)
{
    if (!filter_index.is_valid())
        return;
    auto action_index = m_filter_model->map(filter_index);
    auto* action = static_cast<GUI::Action*>(action_index.internal_data());
    VERIFY(action);
    m_selected_action = action;
    done(ExecOK);
}

}
