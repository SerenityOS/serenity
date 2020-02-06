/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibGUI/JsonArrayModel.h>

namespace GUI {

void JsonArrayModel::update()
{
    auto file = Core::File::construct(m_json_path);
    if (!file->open(Core::IODevice::ReadOnly)) {
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

Model::ColumnMetadata JsonArrayModel::column_metadata(int column) const
{
    ASSERT(column < m_fields.size());
    return { 100, m_fields[column].text_alignment };
}

Variant JsonArrayModel::data(const ModelIndex& index, Role role) const
{
    auto& field_spec = m_fields[index.column()];
    auto& object = m_array.at(index.row()).as_object();

    if (role == Model::Role::Display) {
        auto& json_field_name = field_spec.json_field_name;
        auto data = object.get(json_field_name);
        if (field_spec.massage_for_display)
            return field_spec.massage_for_display(object);
        if (data.is_number())
            return data.to_i32();
        return object.get(json_field_name).to_string();
    }

    if (role == Model::Role::Sort) {
        if (field_spec.massage_for_sort)
            return field_spec.massage_for_sort(object);
        return data(index, Role::Display);
    }

    if (role == Model::Role::Custom) {
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
    update();
}

}
