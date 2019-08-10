#include <AK/JsonObject.h>
#include <LibCore/CFile.h>
#include <LibGUI/GJsonArrayModel.h>

void GJsonArrayModel::update()
{
    CFile file(m_json_path);
    if (!file.open(CIODevice::ReadOnly)) {
        dbg() << "Unable to open " << file.filename();
        return;
    }

    auto json = JsonValue::from_string(file.read_all());

    ASSERT(json.is_array());
    m_array = json.as_array();

    did_update();
}

GModel::ColumnMetadata GJsonArrayModel::column_metadata(int column) const
{
    ASSERT(column < m_fields.size());
    return { 100, m_fields[column].text_alignment };
}

GVariant GJsonArrayModel::data(const GModelIndex& index, Role role) const
{
    auto& object = m_array.at(index.row()).as_object();

    if (role == GModel::Role::Display) {
        auto& json_field_name = m_fields[index.column()].json_field_name;
        auto data = object.get(json_field_name);
        if (data.is_int())
            return data.as_int();
        if (data.is_uint())
            return data.as_uint();
        return object.get(json_field_name).to_string();
    }
    return {};
}
