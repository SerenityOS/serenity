/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibGUI/JsonArrayModel.h>

namespace GUI {

void JsonArrayModel::invalidate()
{
    auto invalidate_or_error = [this]() -> ErrorOr<void> {
        auto file = TRY(Core::File::open(m_json_path, Core::File::OpenMode::Read));

        auto file_contents = TRY(file->read_until_eof());

        auto json = TRY(JsonValue::from_string(file_contents));

        VERIFY(json.is_array());
        m_array = json.as_array();

        return {};
    };

    if (auto result = invalidate_or_error(); result.is_error()) {
        dbgln("Unable to invalidate {}: {}", m_json_path, result.error());
        m_array.clear();
    }

    did_update();
}

void JsonArrayModel::update()
{
    auto update_or_error = [this]() -> ErrorOr<void> {
        auto file = TRY(Core::File::open(m_json_path, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());

        auto json = TRY(JsonValue::from_string(file_contents));

        VERIFY(json.is_array());
        m_array = json.as_array();

        return {};
    };

    if (auto result = update_or_error(); result.is_error()) {
        dbgln("Unable to update {}: {}", m_json_path, result.error());
        m_array.clear();
        did_update();
        return;
    }

    did_update(GUI::Model::UpdateFlag::DontInvalidateIndices);
}

ErrorOr<void> JsonArrayModel::store()
{
    auto file = TRY(Core::File::open(m_json_path, Core::File::OpenMode::Write));
    ByteBuffer json_bytes = m_array.to_byte_string().to_byte_buffer();

    TRY(file->write_until_depleted(json_bytes));
    file->close();
    return {};
}

ErrorOr<void> JsonArrayModel::add(Vector<JsonValue> const&& fields)
{
    VERIFY(fields.size() == m_fields.size());

    JsonObject obj;
    for (size_t i = 0; i < m_fields.size(); ++i) {
        auto& field_spec = m_fields[i];
        obj.set(field_spec.json_field_name, fields.at(i));
    }

    TRY(m_array.append(move(obj)));
    did_update();

    return {};
}

ErrorOr<void> JsonArrayModel::set(int row, Vector<JsonValue>&& fields)
{
    VERIFY(fields.size() == m_fields.size());

    if ((size_t)row >= m_array.size())
        return Error::from_string_view("Row out of bounds"sv);

    JsonObject obj;
    for (size_t i = 0; i < m_fields.size(); ++i) {
        auto& field_spec = m_fields[i];
        obj.set(field_spec.json_field_name, move(fields.at(i)));
    }

    m_array.set(row, move(obj));
    did_update();

    return {};
}

ErrorOr<void> JsonArrayModel::remove(int row)
{
    if ((size_t)row >= m_array.size())
        return Error::from_string_view("Row out of bounds"sv);

    JsonArray new_array;
    for (size_t i = 0; i < m_array.size(); ++i)
        if (i != (size_t)row)
            TRY(new_array.append(m_array.at(i)));

    m_array = new_array;

    did_update();

    return {};
}

Variant JsonArrayModel::data(ModelIndex const& index, ModelRole role) const
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
        if (!data.has_value())
            return "";
        if (data->is_number())
            return data->serialized<StringBuilder>();
        return data->as_string();
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

void JsonArrayModel::set_json_path(ByteString const& json_path)
{
    if (m_json_path == json_path)
        return;

    m_json_path = json_path;
    invalidate();
}

}
