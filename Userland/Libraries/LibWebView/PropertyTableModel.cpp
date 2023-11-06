/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/QuickSort.h>
#include <LibWebView/PropertyTableModel.h>

namespace WebView {

PropertyTableModel::PropertyTableModel(Type type, JsonValue const& properties)
{
    properties.as_object().for_each_member([&](auto const& property_name, auto const& property_value) {
        switch (type) {
        case PropertyTableModel::Type::ARIAProperties:
            m_values.empend(MUST(String::from_deprecated_string(property_name)), String {});

            property_value.as_object().for_each_member([&](auto const& property_name, auto const& property_value) {
                m_values.empend(MUST(String::from_deprecated_string(property_name)), MUST(String::from_deprecated_string(property_value.as_string())));
            });

            break;

        case PropertyTableModel::Type::StyleProperties:
            m_values.empend(MUST(String::from_deprecated_string(property_name)), MUST(String::from_deprecated_string(property_value.as_string())));
            break;
        }
    });

    quick_sort(m_values, [](auto const& a, auto const& b) {
        return a.name < b.name;
    });
}

PropertyTableModel::~PropertyTableModel() = default;

int PropertyTableModel::row_count(ModelIndex const&) const
{
    return static_cast<int>(m_values.size());
}

int PropertyTableModel::column_count(ModelIndex const&) const
{
    return 2;
}

ErrorOr<String> PropertyTableModel::column_name(int column_index) const
{
    switch (static_cast<Column>(column_index)) {
    case Column::PropertyName:
        return "Name"_string;
    case Column::PropertyValue:
        return "Value"_string;
    }

    VERIFY_NOT_REACHED();
}

ModelIndex PropertyTableModel::index(int row, int column, ModelIndex const&) const
{
    return { row, column };
}

String PropertyTableModel::text_for_display(ModelIndex const& index) const
{
    auto const& value = m_values[index.row];

    switch (static_cast<Column>(index.column)) {
    case Column::PropertyName:
        return value.name;
    case Column::PropertyValue:
        return value.value;
    }

    VERIFY_NOT_REACHED();
}

}
