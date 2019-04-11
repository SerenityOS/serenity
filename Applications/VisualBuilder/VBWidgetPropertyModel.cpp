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
    dbgprintf("row count: %d\n", m_widget.m_properties.size());
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
    return { 100, TextAlignment::CenterLeft };
}

GVariant VBWidgetPropertyModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        dbgprintf("Accessing prop #%d (size=%d) column %d\n", index.row(), m_widget.m_properties.size(), index.column());
        auto& property = *m_widget.m_properties[index.row()];
        auto value = property.value();
        dbgprintf("value type=%u\n", (unsigned)value.type());
        dbgprintf("value impl=%x\n", (unsigned)value.to_string().impl());
        dbgprintf("value len=%x\n", (unsigned)value.to_string().impl()->length());
        dbgprintf("for value='%s'\n", value.to_string().characters());
        switch (index.column()) {
        case Column::Name: return property.name();
        case Column::Value: return property.value();
        }
    }
    return { };
}
