/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/StylePropertiesModel.h>

namespace Web {

StylePropertiesModel::StylePropertiesModel(const CSS::StyleProperties& properties)
    : m_properties(properties)
{
    properties.for_each_property([&](auto property_id, auto& property_value) {
        Value value;
        value.name = CSS::string_from_property_id(property_id);
        value.value = property_value.to_string();
        m_values.append(value);
    });

    quick_sort(m_values, [](auto& a, auto& b) { return a.name < b.name; });
}

int StylePropertiesModel::row_count(const GUI::ModelIndex&) const
{
    return m_values.size();
}

String StylePropertiesModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::PropertyName:
        return "Name";
    case Column::PropertyValue:
        return "Value";
    default:
        VERIFY_NOT_REACHED();
    }
}
GUI::Variant StylePropertiesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
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

}
