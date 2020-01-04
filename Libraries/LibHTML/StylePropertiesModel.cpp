#include "StylePropertiesModel.h"
#include <LibHTML/CSS/PropertyID.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/CSS/StyleProperties.h>

StylePropertiesModel::StylePropertiesModel(const StyleProperties& properties)
    : m_properties(properties)
{
    properties.for_each_property([&](auto property_id, auto& property_value) {
	Value value;
	value.name = CSS::string_from_property_id(property_id);
	value.value = property_value.to_string();
	m_values.append(value);
    });
}

int StylePropertiesModel::row_count(const GModelIndex&) const
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
        ASSERT_NOT_REACHED();
    }
}
GVariant StylePropertiesModel::data(const GModelIndex& index, Role role) const
{
    auto& value = m_values[index.row()];
    if (role == Role::Display) {
        if (index.column() == Column::PropertyName)
            return value.name;
        if (index.column() == Column::PropertyValue)
            return value.value;
    }
    return {};
}

void StylePropertiesModel::update()
{
    did_update();
}
