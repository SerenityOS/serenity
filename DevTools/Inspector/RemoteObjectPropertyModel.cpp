#include "RemoteObjectPropertyModel.h"
#include "RemoteObject.h"

RemoteObjectPropertyModel::RemoteObjectPropertyModel(RemoteObject& object)
    : m_object(object)
{
}

int RemoteObjectPropertyModel::row_count(const GModelIndex&) const
{
    return m_properties.size();
}

String RemoteObjectPropertyModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    case Column::Value:
        return "Value";
    }
    ASSERT_NOT_REACHED();
}

GVariant RemoteObjectPropertyModel::data(const GModelIndex& index, Role role) const
{
    auto& property = m_properties[index.row()];
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Name:
            return property.name;
        case Column::Value:
            return property.value;
        }
    }
    return {};
}

void RemoteObjectPropertyModel::update()
{
    m_properties.clear();
    m_object.json.for_each_member([this](auto& name, auto& value) {
        m_properties.append({ name, value });
    });
    did_update();
}
