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

#include "RemoteObjectPropertyModel.h"
#include "RemoteObject.h"
#include "RemoteProcess.h"

namespace Inspector {

RemoteObjectPropertyModel::RemoteObjectPropertyModel(RemoteObject& object)
    : m_object(object)
{
}

int RemoteObjectPropertyModel::row_count(const GUI::ModelIndex& index) const
{
    Function<int(const JsonValue&)> do_count = [&](const JsonValue& value) {
        if (value.is_array())
            return value.as_array().size();
        else if (value.is_object())
            return value.as_object().size();
        return 0;
    };

    if (index.is_valid()) {
        auto* path = static_cast<const JsonPath*>(index.internal_data());
        return do_count(path->resolve(m_object.json));
    } else {
        return do_count(m_object.json);
    }
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

GUI::Variant RemoteObjectPropertyModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* path = static_cast<const JsonPath*>(index.internal_data());
    if (!path)
        return {};

    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Name:
            return path->last().to_string();
        case Column::Value: {
            auto data = path->resolve(m_object.json);
            if (data.is_array())
                return String::format("<Array with %d element%s", data.as_array().size(), data.as_array().size() == 1 ? ">" : "s>");
            if (data.is_object())
                return String::format("<Object with %d entr%s", data.as_object().size(), data.as_object().size() == 1 ? "y>" : "ies>");
            return data;
        }
        }
    }
    return {};
}

void RemoteObjectPropertyModel::update()
{
    did_update();
}

void RemoteObjectPropertyModel::set_data(const GUI::ModelIndex& index, const GUI::Variant& new_value)
{
    if (!index.is_valid())
        return;

    auto* path = static_cast<const JsonPath*>(index.internal_data());
    if (path->size() != 1)
        return;

    FlatPtr address = m_object.address;
    RemoteProcess::the().set_property(address, path->first().to_string(), new_value.to_string());
    did_update();
}

GUI::ModelIndex RemoteObjectPropertyModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    const auto& parent_path = parent.is_valid() ? *static_cast<const JsonPath*>(parent.internal_data()) : JsonPath {};

    auto nth_child = [&](int n, const JsonValue& value) -> const JsonPath* {
        auto path = make<JsonPath>();
        path->append(parent_path);
        int row_index = n;
        if (value.is_object()) {
            String property_name;
            auto& object = value.as_object();
            object.for_each_member([&](auto& name, auto&) {
                if (row_index > 0) {
                    --row_index;
                } else if (row_index == 0) {
                    property_name = name;
                    --row_index;
                }
            });
            if (property_name.is_null())
                return nullptr;

            path->append({ property_name });
            m_paths.append(move(path));
        } else if (value.is_array()) {
            path->append(JsonPathElement { (size_t)n });
            m_paths.append(move(path));
        } else {
            return nullptr;
        }
        return &m_paths.last();
    };

    if (!parent.is_valid()) {
        if (m_object.json.is_empty())
            return {};
    }

    auto index_path = cached_path_at(row, parent_path);

    if (!index_path)
        index_path = nth_child(row, parent_path.resolve(m_object.json));

    if (!index_path)
        return {};

    return create_index(row, column, index_path);
}

GUI::ModelIndex RemoteObjectPropertyModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return index;

    auto path = *static_cast<const JsonPath*>(index.internal_data());
    if (path.is_empty())
        return {};

    path.take_last();
    if (path.is_empty())
        return {};

    auto* cpath = find_cached_path(path);
    if (cpath) {
        int index_in_parent = 0;
        if (cpath->last().kind() == JsonPathElement::Kind::Index)
            index_in_parent = cpath->last().index();
        else if (cpath->last().kind() == JsonPathElement::Kind::Key) {
            auto path_copy = path;
            auto last = path_copy.take_last();
            bool found = false;
            path_copy.resolve(m_object.json).as_object().for_each_member([&](auto& name, auto&) {
                if (!found) {
                    if (last.key() == name)
                        found = true;
                    else
                        index_in_parent++;
                }
            });
        }
        return create_index(index_in_parent, 0, cpath);
    }

    dbg() << "No cached path found for path " << path.to_string();
    return {};
}

const JsonPath* RemoteObjectPropertyModel::cached_path_at(int n, const Vector<JsonPathElement>& prefix) const
{
    // FIXME: ModelIndex wants a void*, so we have to keep these
    //        indices alive, but allocating a new path every time
    //        we're asked for an index is silly, so we have to look for existing ones first.
    const JsonPath* index_path = nullptr;
    int row_index = n;
    for (auto& path : m_paths) {
        if (path.size() != prefix.size() + 1)
            continue;

        for (size_t i = 0; i < prefix.size(); ++i) {
            if (path[i] != prefix[i])
                goto do_continue;
        }

        if (row_index == 0) {
            index_path = &path;
            break;
        }
        --row_index;
    do_continue:;
    }

    return index_path;
};

const JsonPath* RemoteObjectPropertyModel::find_cached_path(const Vector<JsonPathElement>& path) const
{
    for (auto& cpath : m_paths) {
        if (cpath.size() != path.size())
            continue;

        for (size_t i = 0; i < cpath.size(); ++i) {
            if (cpath[i] != path[i])
                goto do_continue;
        }

        return &cpath;
    do_continue:;
    }

    return nullptr;
}

}
