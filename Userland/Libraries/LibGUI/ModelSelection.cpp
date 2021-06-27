/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelSelection.h>
#include <LibGUI/PersistentModelIndex.h>

namespace GUI {

void ModelSelection::remove_matching(Function<bool(const ModelIndex&)> filter)
{
    bool did_remove = false;
    m_indices.remove_all_matching([&](PersistentModelIndex& index) -> bool {
        if (filter(index)) {
            did_remove = true;
            return true;
        }

        return false;
    });

    if (did_remove) {
        notify_selection_changed();
    }
}

void ModelSelection::set(const ModelIndex& index)
{
    VERIFY(index.is_valid());

    PersistentModelIndex persistent_index { index };
    if (m_indices.size() == 1 && m_indices.contains(persistent_index))
        return;

    m_indices.clear();
    m_indices.set(move(persistent_index));
    notify_selection_changed();
}

void ModelSelection::add(const ModelIndex& index)
{
    VERIFY(index.is_valid());

    PersistentModelIndex persistent_index { index };
    if (m_indices.contains(persistent_index))
        return;

    m_indices.set(move(persistent_index));
    notify_selection_changed();
}

void ModelSelection::add_all(const Vector<ModelIndex>& indices)
{
    {
        TemporaryChange notify_change { m_disable_notify, true };
        for (auto& index : indices)
            add(index);
    }

    if (m_notify_pending)
        notify_selection_changed();
}

void ModelSelection::toggle(const ModelIndex& index)
{
    VERIFY(index.is_valid());

    PersistentModelIndex persistent_index { index };
    if (m_indices.contains(persistent_index))
        m_indices.remove(persistent_index);
    else
        m_indices.set(move(persistent_index));
    notify_selection_changed();
}

bool ModelSelection::remove(const ModelIndex& index)
{
    VERIFY(index.is_valid());

    PersistentModelIndex persistent_index { index };
    if (!m_indices.contains(persistent_index))
        return false;

    m_indices.remove(persistent_index);
    notify_selection_changed();
    return true;
}

void ModelSelection::clear()
{
    if (m_indices.is_empty())
        return;
    m_indices.clear();
    notify_selection_changed();
}

void ModelSelection::notify_selection_changed()
{
    if (!m_disable_notify) {
        m_view.notify_selection_changed({});
        m_notify_pending = false;
    } else {
        m_notify_pending = true;
    }
}

}
