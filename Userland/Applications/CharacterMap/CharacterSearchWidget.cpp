/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterSearchWidget.h"
#include "SearchCharacters.h"
#include <Applications/CharacterMap/CharacterSearchWindowGML.h>
#include <LibCore/Debounce.h>

struct SearchResult {
    u32 code_point;
    ByteString code_point_string;
    ByteString display_text;
};

class CharacterSearchModel final : public GUI::Model {
public:
    CharacterSearchModel() = default;

    int row_count(GUI::ModelIndex const&) const override { return m_data.size(); }
    int column_count(GUI::ModelIndex const&) const override { return 2; }

    GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        auto& result = m_data.at(index.row());
        if (role == GUI::ModelRole::Display) {
            if (index.column() == 0)
                return result.code_point_string;
            return result.display_text;
        }
        if (role == GUI::ModelRole::Custom)
            return result.code_point;
        return {};
    }

    void clear()
    {
        m_data.clear();
        invalidate();
    }

    void add_result(SearchResult result)
    {
        m_data.append(move(result));
        invalidate();
    }

private:
    Vector<SearchResult> m_data;
};

CharacterSearchWidget::CharacterSearchWidget()
{
    load_from_gml(character_search_window_gml).release_value_but_fixme_should_propagate_errors();

    m_search_input = find_descendant_of_type_named<GUI::TextBox>("search_input");
    m_results_table = find_descendant_of_type_named<GUI::TableView>("results_table");

    m_search_input->on_up_pressed = [this] { m_results_table->move_cursor(GUI::AbstractView::CursorMovement::Up, GUI::AbstractView::SelectionUpdate::Set); };
    m_search_input->on_down_pressed = [this] { m_results_table->move_cursor(GUI::AbstractView::CursorMovement::Down, GUI::AbstractView::SelectionUpdate::Set); };

    m_search_input->on_change = Core::debounce(100, [this] { search(); });

    m_results_table->horizontal_scrollbar().set_visible(false);
    m_results_table->set_column_headers_visible(false);
    m_results_table->set_model(adopt_ref(*new CharacterSearchModel()));
    m_results_table->on_selection_change = [&] {
        auto& model = static_cast<CharacterSearchModel&>(*m_results_table->model());
        auto index = m_results_table->selection().first();
        auto code_point = model.data(index, GUI::ModelRole::Custom).as_u32();
        if (on_character_selected)
            on_character_selected(code_point);
    };
}

void CharacterSearchWidget::search()
{
    ScopeGuard guard { [&] { m_results_table->set_updates_enabled(true); } };
    m_results_table->set_updates_enabled(false);

    // TODO: Sort the results nicely. They're sorted by code-point for now, which is easy, but not the most useful.
    //       Sorting intelligently in a style similar to Assistant would be nicer.
    //       Note that this will mean limiting the number of results some other way.
    auto& model = static_cast<CharacterSearchModel&>(*m_results_table->model());
    model.clear();
    auto query = m_search_input->text();
    if (query.is_empty())
        return;
    for_each_character_containing(query, [&](auto code_point, auto display_name) {
        StringBuilder builder;
        builder.append_code_point(code_point);

        model.add_result({ code_point, builder.to_byte_string(), move(display_name) });

        // Stop when we reach 250 results.
        // This is already too many for the search to be useful, and means we don't spend forever recalculating the column size.
        if (model.row_count({}) >= 250)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
}
