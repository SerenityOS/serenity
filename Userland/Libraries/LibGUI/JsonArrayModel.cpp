/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibGUI/JsonArrayModel.h>

namespace GUI {

void JsonArrayModel::invalidate()
{
    auto file = Core::File::construct(m_json_path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        dbgln("Unable to open {}", file->filename());
        m_array.clear();
        did_update();
        return;
    }

    auto json = JsonValue::from_string(file->read_all()).release_value_but_fixme_should_propagate_errors();

    VERIFY(json.is_array());
    m_array = json.as_array();

    did_update();
}

void JsonArrayModel::update()
{
    auto file = Core::File::construct(m_json_path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        dbgln("Unable to open {}", file->filename());
        m_array.clear();
        did_update();
        return;
    }

    auto json = JsonValue::from_string(file->read_all()).release_value_but_fixme_should_propagate_errors();

    VERIFY(json.is_array());
    m_array = json.as_array();

    did_update(GUI::Model::UpdateFlag::DontInvalidateIndices);
}

bool JsonArrayModel::store()
{
    auto file = Core::File::construct(m_json_path);
    if (!file->open(Core::OpenMode::WriteOnly)) {
        dbgln("Unable to open {}", file->filename());
        return false;
    }

    file->write(m_array.to_string());
    file->close();
    return true;
}

bool JsonArrayModel::add(const Vector<JsonValue>&& values)
{
    VERIFY(values.size() == m_fields.size());
    JsonObject obj;
    for (size_t i = 0; i < m_fields.size(); ++i) {
        auto& field_spec = m_fields[i];
        obj.set(field_spec.json_field_name, values.at(i));
    }
    m_array.append(move(obj));
    did_update();
    return true;
}

bool JsonArrayModel::set(int row, Vector<JsonValue>&& values)
{
    VERIFY(values.size() == m_fields.size());

    if ((size_t)row >= m_array.size())
        return false;

    JsonObject obj;
    for (size_t i = 0; i < m_fields.size(); ++i) {
        auto& field_spec = m_fields[i];
        obj.set(field_spec.json_field_name, move(values.at(i)));
    }

    m_array.set(row, move(obj));
    did_update();

    return true;
}

bool JsonArrayModel::remove(int row)
{
    if ((size_t)row >= m_array.size())
        return false;

    JsonArray new_array;
    for (size_t i = 0; i < m_array.size(); ++i)
        if (i != (size_t)row)
            new_array.append(m_array.at(i));

    m_array = new_array;

    did_update();

    return true;
}

Variant JsonArrayModel::data(const ModelIndex& index, ModelRole role) const
{
    auto& field_spec = m_fields[index.column()];
    auto& object = m_array.at(index.row()).as_object();

    if (role == ModelRole::TextAlignment) {
        return field_spec.text_alignment;
    }

    if (role == ModelRole::Display) {
        auto& json_field_name = field_spec.json_field_name;
        auto data = object.get(json_field_name);
        if (field_spec.massage_for_display)
            return field_spec.massage_for_display(object);
        if (data.is_number())
            return data;
        return object.get(json_field_name).to_string();
    }

    if (role == ModelRole::Sort) {
        if (field_spec.massage_for_sort)
            return field_spec.massage_for_sort(object);
        return data(index, ModelRole::Display);
    }

    if (role == ModelRole::Custom) {
        if (field_spec.massage_for_custom)
            return field_spec.massage_for_custom(object);
        return {};
    }

    return {};
}

void JsonArrayModel::set_json_path(const String& json_path)
{
    if (m_json_path == json_path)
        return;

    m_json_path = json_path;
    invalidate();
}

}
