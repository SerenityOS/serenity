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

#include "RemoteObjectGraphModel.h"
#include "RemoteObject.h"
#include "RemoteProcess.h"
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibGUI/Application.h>
#include <stdio.h>

RemoteObjectGraphModel::RemoteObjectGraphModel(RemoteProcess& process)
    : m_process(process)
{
    m_object_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
    m_window_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png"));
    m_layout_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/layout.png"));
}

RemoteObjectGraphModel::~RemoteObjectGraphModel()
{
}

GUI::ModelIndex RemoteObjectGraphModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        if (m_process.roots().is_empty())
            return {};
        return create_index(row, column, &m_process.roots().at(row));
    }
    auto& remote_parent = *static_cast<RemoteObject*>(parent.internal_data());
    return create_index(row, column, &remote_parent.children.at(row));
}

GUI::ModelIndex RemoteObjectGraphModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& remote_object = *static_cast<RemoteObject*>(index.internal_data());
    if (!remote_object.parent)
        return {};

    // NOTE: If the parent has no parent, it's a root, so we have to look among the remote roots.
    if (!remote_object.parent->parent) {
        for (size_t row = 0; row < m_process.roots().size(); ++row) {
            if (&m_process.roots()[row] == remote_object.parent)
                return create_index(row, 0, remote_object.parent);
        }
        ASSERT_NOT_REACHED();
        return {};
    }

    for (size_t row = 0; row < remote_object.parent->parent->children.size(); ++row) {
        if (&remote_object.parent->parent->children[row] == remote_object.parent)
            return create_index(row, 0, remote_object.parent);
    }

    ASSERT_NOT_REACHED();
    return {};
}

int RemoteObjectGraphModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_process.roots().size();
    auto& remote_object = *static_cast<RemoteObject*>(index.internal_data());
    return remote_object.children.size();
}

int RemoteObjectGraphModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

GUI::Variant RemoteObjectGraphModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
    if (role == Role::Icon) {
        if (remote_object->class_name == "Window")
            return m_window_icon;
        if (remote_object->class_name.ends_with("Layout"))
            return m_layout_icon;
        return m_object_icon;
    }
    if (role == Role::Display) {
        return String::format("%s{%s}", remote_object->class_name.characters(), remote_object->address.characters());
    }
    return {};
}

void RemoteObjectGraphModel::update()
{
    did_update();
}
