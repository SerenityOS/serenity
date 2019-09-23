#include "RemoteObjectGraphModel.h"
#include "RemoteObject.h"
#include "RemoteProcess.h"
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <stdio.h>

RemoteObjectGraphModel::RemoteObjectGraphModel(RemoteProcess& process)
    : m_process(process)
{
    m_object_icon.set_bitmap_for_size(16, load_png("/res/icons/16x16/inspector-object.png"));
    m_window_icon.set_bitmap_for_size(16, load_png("/res/icons/16x16/window.png"));
}

RemoteObjectGraphModel::~RemoteObjectGraphModel()
{
}

GModelIndex RemoteObjectGraphModel::index(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid()) {
        if (m_process.roots().is_empty())
            return {};
        return create_index(row, column, &m_process.roots().at(row));
    }
    auto& remote_parent = *static_cast<RemoteObject*>(parent.internal_data());
    return create_index(row, column, &remote_parent.children.at(row));
}

GModelIndex RemoteObjectGraphModel::parent_index(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& remote_object = *static_cast<RemoteObject*>(index.internal_data());
    if (!remote_object.parent)
        return {};

    // NOTE: If the parent has no parent, it's a root, so we have to look among the remote roots.
    if (!remote_object.parent->parent) {
        for (int row = 0; row < m_process.roots().size(); ++row) {
            if (&m_process.roots()[row] == remote_object.parent)
                return create_index(row, 0, remote_object.parent);
        }
        ASSERT_NOT_REACHED();
        return {};
    }

    for (int row = 0; row < remote_object.parent->parent->children.size(); ++row) {
        if (&remote_object.parent->parent->children[row] == remote_object.parent)
            return create_index(row, 0, remote_object.parent);
    }

    ASSERT_NOT_REACHED();
    return {};
}

int RemoteObjectGraphModel::row_count(const GModelIndex& index) const
{
    if (!index.is_valid())
        return m_process.roots().size();
    auto& remote_object = *static_cast<RemoteObject*>(index.internal_data());
    return remote_object.children.size();
}

int RemoteObjectGraphModel::column_count(const GModelIndex&) const
{
    return 1;
}

GVariant RemoteObjectGraphModel::data(const GModelIndex& index, Role role) const
{
    auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
    if (role == Role::Icon) {
        if (remote_object->class_name == "GWindow")
            return m_window_icon;
        return m_object_icon;
    }
    if (role == Role::Display) {
        return String::format("%s{%s} (%d)", remote_object->class_name.characters(), remote_object->address.characters(), remote_object->children.size());
    }
    return {};
}

void RemoteObjectGraphModel::update()
{
    did_update();
}
