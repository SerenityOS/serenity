/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StylePropertiesModel.h"
#include <AK/QuickSort.h>

namespace WebView {

StylePropertiesModel::StylePropertiesModel(JsonObject properties)
    : m_properties(move(properties))
{
    m_properties.for_each_member([&](auto& property_name, auto& property_value) {
        Value value;
        value.name = property_name;
        value.value = property_value.to_deprecated_string();
        m_values.append(value);
    });

    quick_sort(m_values, [](auto& a, auto& b) { return a.name < b.name; });
}

StylePropertiesModel::~StylePropertiesModel() = default;

int StylePropertiesModel::row_count(GUI::ModelIndex const&) const
{
    return m_values.size();
}

ErrorOr<String> StylePropertiesModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::PropertyName:
        return "Name"_string;
    case Column::PropertyValue:
        return "Value"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant StylePropertiesModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& value = m_values[index.row()];
    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::PropertyName)
            return value.name;
        if (index.column() == Column::PropertyValue)
            return value.value;
    }
    return {};
}

Vector<GUI::ModelIndex> StylePropertiesModel::matches(StringView searching, unsigned flags, GUI::ModelIndex const& parent)
{
    if (m_values.is_empty())
        return {};
    Vector<GUI::ModelIndex> found_indices;
    for (auto it = m_values.begin(); !it.is_end(); ++it) {
        GUI::ModelIndex index = this->index(it.index(), Column::PropertyName, parent);
        if (!string_matches(data(index, GUI::ModelRole::Display).as_string(), searching, flags))
            continue;

        found_indices.append(index);
        if (flags & FirstMatchOnly)
            break;
    }
    return found_indices;
}

}
