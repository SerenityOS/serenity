#include "VBWidgetPropertyModel.h"
#include "VBWidget.h"
#include "VBProperty.h"

VBWidgetPropertyModel::VBWidgetPropertyModel(VBWidget& widget)
    : m_widget(widget)
{
}

VBWidgetPropertyModel::~VBWidgetPropertyModel()
{
    ASSERT_NOT_REACHED();
}

int VBWidgetPropertyModel::row_count(const GModelIndex&) const
{
    return m_widget.m_properties.size();
}

String VBWidgetPropertyModel::column_name(int column) const
{
    switch (column) {
    case Column::Name: return "Name";
    case Column::Value: return "Value";
    default: ASSERT_NOT_REACHED();
    }
}

GModel::ColumnMetadata VBWidgetPropertyModel::column_metadata(int column) const
{
    UNUSED_PARAM(column);
    return { 80, TextAlignment::CenterLeft };
}

GVariant VBWidgetPropertyModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        auto& property = *m_widget.m_properties[index.row()];
        switch (index.column()) {
        case Column::Name: return property.name();
        case Column::Value: return property.value();
        }
        ASSERT_NOT_REACHED();
    }
    if (role == Role::ForegroundColor) {
        auto& property = *m_widget.m_properties[index.row()];
        switch (index.column()) {
        case Column::Name: return Color::Black;
        case Column::Value: return property.is_readonly() ? Color(Color::MidGray) : Color(Color::Black);
        }
        ASSERT_NOT_REACHED();
    }
    return { };
}
