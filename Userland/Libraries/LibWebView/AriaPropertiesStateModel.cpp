/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AriaPropertiesStateModel.h"

namespace WebView {

AriaPropertiesStateModel::AriaPropertiesStateModel(JsonObject properties_state)
    : m_properties_state(move(properties_state))
{
    m_properties_state.for_each_member([&](auto property_name, JsonValue const& values) {
        Value value;
        value.name = property_name;
        value.value = "";
        m_values.append(value);
        values.as_object().for_each_member([&](auto property_name, auto& property_value) {
            Value value;
            value.name = property_name;
            value.value = property_value.to_deprecated_string();
            m_values.append(value);
        });
    });
}

AriaPropertiesStateModel::~AriaPropertiesStateModel() = default;

int AriaPropertiesStateModel::row_count(GUI::ModelIndex const&) const
{
    return m_values.size();
}

ErrorOr<String> AriaPropertiesStateModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::PropertyName:
        return "Name"_string;
    case Column::PropertyValue:
        return "Value"_string;
    default:
        return Error::from_string_view("Unexpected column index"sv);
    }
}

GUI::Variant AriaPropertiesStateModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
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

Vector<GUI::ModelIndex> AriaPropertiesStateModel::matches(AK::StringView searching, unsigned int flags, GUI::ModelIndex const& parent)
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
