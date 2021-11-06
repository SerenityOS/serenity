/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteObjectGraphModel.h"
#include "RemoteObject.h"
#include "RemoteProcess.h"
#include <AK/JsonValue.h>
#include <LibGUI/Application.h>
#include <stdio.h>

namespace Inspector {

RemoteObjectGraphModel::RemoteObjectGraphModel(RemoteProcess& process)
    : m_process(process)
{
    m_object_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png").release_value_but_fixme_should_propagate_errors());
    m_window_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window.png").release_value_but_fixme_should_propagate_errors());
    m_layout_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/layout.png").release_value_but_fixme_should_propagate_errors());
    m_timer_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/timer.png").release_value_but_fixme_should_propagate_errors());
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
        VERIFY_NOT_REACHED();
        return {};
    }

    for (size_t row = 0; row < remote_object.parent->parent->children.size(); ++row) {
        if (&remote_object.parent->parent->children[row] == remote_object.parent)
            return create_index(row, 0, remote_object.parent);
    }

    VERIFY_NOT_REACHED();
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

GUI::Variant RemoteObjectGraphModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
    if (role == GUI::ModelRole::Icon) {
        if (remote_object->class_name == "Window")
            return m_window_icon;
        if (remote_object->class_name == "Timer")
            return m_timer_icon;
        if (remote_object->class_name.ends_with("Layout"))
            return m_layout_icon;
        return m_object_icon;
    }
    if (role == GUI::ModelRole::Display)
        return String::formatted("{}({:p})", remote_object->class_name, remote_object->address);

    return {};
}

}
