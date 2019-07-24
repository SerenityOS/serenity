#include "VBWidgetPropertyModel.h"
#include "VBProperty.h"
#include "VBWidget.h"
#include <LibDraw/Font.h>

VBWidgetPropertyModel::VBWidgetPropertyModel(VBWidget& widget)
    : m_widget(widget)
{
}

VBWidgetPropertyModel::~VBWidgetPropertyModel()
{
}

int VBWidgetPropertyModel::row_count(const GModelIndex&) const
{
    return m_widget.m_properties.size();
}

String VBWidgetPropertyModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    case Column::Value:
        return "Value";
    case Column::Type:
        return "Type";
    default:
        ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata VBWidgetPropertyModel::column_metadata(int column) const
{
    UNUSED_PARAM(column);
    if (column == Column::Name)
        return { 110, TextAlignment::CenterLeft, &Font::default_bold_font() };
    return { 90, TextAlignment::CenterLeft };
}

GVariant VBWidgetPropertyModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Custom) {
        auto& property = m_widget.m_properties[index.row()];
        if (index.column() == Column::Type)
            return (int)property.value().type();
        return {};
    }
    if (role == Role::Display) {
        auto& property = m_widget.m_properties[index.row()];
        switch (index.column()) {
        case Column::Name:
            return property.name();
        case Column::Value:
            return property.value();
        case Column::Type:
            return to_string(property.value().type());
        }
        ASSERT_NOT_REACHED();
    }
    if (role == Role::ForegroundColor) {
        auto& property = m_widget.m_properties[index.row()];
        switch (index.column()) {
        case Column::Name:
            return Color::Black;
        case Column::Type:
            return Color::Blue;
        case Column::Value:
            return property.is_readonly() ? Color(Color::MidGray) : Color(Color::Black);
        }
        ASSERT_NOT_REACHED();
    }
    return {};
}

void VBWidgetPropertyModel::set_data(const GModelIndex& index, const GVariant& value)
{
    ASSERT(index.column() == Column::Value);
    auto& property = m_widget.m_properties[index.row()];
    ASSERT(!property.is_readonly());
    property.set_value(value);
}

bool VBWidgetPropertyModel::is_editable(const GModelIndex& index) const
{
    if (index.column() != Column::Value)
        return false;
    auto& property = m_widget.m_properties[index.row()];
    return !property.is_readonly();
}
