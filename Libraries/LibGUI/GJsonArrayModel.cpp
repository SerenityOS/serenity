#include <AK/JsonObject.h>
#include <LibCore/CFile.h>
#include <LibGUI/GJsonArrayModel.h>

void GJsonArrayModel::update()
{
    auto file = CFile::construct(m_json_path);
    if (!file->open(CIODevice::ReadOnly)) {
        dbg() << "Unable to open " << file->filename();
        m_array.clear();
        did_update();
        return;
    }

    auto json = JsonValue::from_string(file->read_all());

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
    auto& field_spec = m_fields[index.column()];
    auto& object = m_array.at(index.row()).as_object();

    if (role == GModel::Role::Display) {
        auto& json_field_name = field_spec.json_field_name;
        auto data = object.get(json_field_name);
        if (field_spec.massage_for_display)
            return field_spec.massage_for_display(object);
        if (data.is_number())
            return data.to_i32();
        return object.get(json_field_name).to_string();
    }

    if (role == GModel::Role::Sort) {
        if (field_spec.massage_for_sort)
            return field_spec.massage_for_sort(object);
        return data(index, Role::Display);
    }

    if (role == GModel::Role::Custom) {
        if (field_spec.massage_for_custom)
            return field_spec.massage_for_custom(object);
        return {};
    }

    return {};
}

void GJsonArrayModel::set_json_path(const String& json_path)
{
    if (m_json_path == json_path)
        return;

    m_json_path = json_path;
    update();
}
