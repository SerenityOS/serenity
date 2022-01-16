/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterSearchWidget.h"
#include "SearchCharacters.h"
#include <Applications/CharacterMap/CharacterSearchWindowGML.h>

struct SearchResult {
    u32 code_point;
    String display_text;
};

class CharacterSearchModel final : public GUI::Model {
public:
    CharacterSearchModel() { }

    int row_count(GUI::ModelIndex const&) const override { return m_data.size(); }
    int column_count(GUI::ModelIndex const&) const override { return 1; }

    GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        auto& result = m_data.at(index.row());
        if (role == GUI::ModelRole::Display)
            return result.display_text;
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
    load_from_gml(character_search_window_gml);

    m_search_input = find_descendant_of_type_named<GUI::TextBox>("search_input");
    m_search_button = find_descendant_of_type_named<GUI::Button>("search_button");
    m_results_list = find_descendant_of_type_named<GUI::ListView>("results_list");

    m_search_input->on_return_pressed = [this] { search(); };
    m_search_button->on_click = [this](auto) { search(); };

    m_results_list->horizontal_scrollbar().set_visible(false);
    m_results_list->set_model(adopt_ref(*new CharacterSearchModel()));
    m_results_list->on_activation = [&](GUI::ModelIndex const& index) {
        auto& model = static_cast<CharacterSearchModel&>(*m_results_list->model());
        auto code_point = model.data(index, GUI::ModelRole::Custom).as_u32();
        if (on_character_selected)
            on_character_selected(code_point);
    };
}

CharacterSearchWidget::~CharacterSearchWidget()
{
}

void CharacterSearchWidget::search()
{
    // TODO: Sort the results nicely. They're sorted by code-point for now, which is easy, but not the most useful.
    //       Sorting intelligently in a style similar to Assistant would be nicer.
    auto& model = static_cast<CharacterSearchModel&>(*m_results_list->model());
    model.clear();
    auto query = m_search_input->text();
    if (query.is_empty())
        return;
    for_each_character_containing(query, [&](auto code_point, auto& display_name) {
        StringBuilder builder;
        builder.append_code_point(code_point);
        builder.append(" - ");
        builder.append(display_name);

        model.add_result({ code_point, builder.to_string() });
    });
}
