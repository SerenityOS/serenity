/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DictionaryModel.h"

namespace Dictionary {

int DictionaryModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid()) {
        return m_last_index - m_first_index;
    }
    return 0;
}

int DictionaryModel::column_count(GUI::ModelIndex const&) const
{
    return 1;
}

GUI::Variant DictionaryModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (index.parent().is_valid())
        return {};

    if (role == GUI::ModelRole::Display || role == GUI::ModelRole::Sort || role == GUI::ModelRole::Search) {

        int row = index.row();

        if (0 <= row && row < (m_last_index - m_first_index)) {
            return m_dictionary.word_at_index(m_first_index + row);
        }
    }

    return {};
}

DictionaryModel::DictionaryModel()
    : m_dictionary("/res/dictionaries/wordnet.bin")
{
    m_first_index = 0;
    m_last_index = m_dictionary.word_count();
}

void DictionaryModel::set_query(String query)
{
    int num_words = m_dictionary.word_count();

    if (query == "") {
        set_range(0, num_words);
        m_query = query;
        return;
    }

    int first_index = 0;

    if (query.starts_with(m_query)) {
        first_index = m_first_index;
    }

    int first_result, num_results;
    m_dictionary.prefix_query(
        query,
        first_index,
        first_result,
        num_results);

    set_range(first_result, first_result + num_results);
    m_query = query;
}

void DictionaryModel::set_range(int start, int end)
{
    if (m_first_index != start || m_last_index != end) {
        m_first_index = start;
        m_last_index = end;
        invalidate();
    }
}

String DictionaryModel::definition_of(int index) const
{
    int n = m_last_index - m_first_index;

    if (0 <= index && index < n)
        return m_dictionary.definition_of(m_first_index + index);

    return "";
}

}
