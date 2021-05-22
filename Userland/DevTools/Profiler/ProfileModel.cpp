/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProfileModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <LibGUI/FileIconProvider.h>
#include <ctype.h>
#include <stdio.h>

namespace Profiler {

ProfileModel::ProfileModel(Profile& profile)
    : m_profile(profile)
{
    m_user_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
    m_kernel_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object-red.png"));
}

ProfileModel::~ProfileModel()
{
}

GUI::ModelIndex ProfileModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        if (m_profile.roots().is_empty())
            return {};
        return create_index(row, column, m_profile.roots().at(row).ptr());
    }
    auto& remote_parent = *static_cast<ProfileNode*>(parent.internal_data());
    return create_index(row, column, remote_parent.children().at(row).ptr());
}

GUI::ModelIndex ProfileModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *static_cast<ProfileNode*>(index.internal_data());
    if (!node.parent())
        return {};

    // NOTE: If the parent has no parent, it's a root, so we have to look among the roots.
    if (!node.parent()->parent()) {
        for (size_t row = 0; row < m_profile.roots().size(); ++row) {
            if (m_profile.roots()[row].ptr() == node.parent()) {
                return create_index(row, index.column(), node.parent());
            }
        }
        VERIFY_NOT_REACHED();
        return {};
    }

    for (size_t row = 0; row < node.parent()->parent()->children().size(); ++row) {
        if (node.parent()->parent()->children()[row].ptr() == node.parent())
            return create_index(row, index.column(), node.parent());
    }

    VERIFY_NOT_REACHED();
    return {};
}

int ProfileModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_profile.roots().size();
    auto& node = *static_cast<ProfileNode*>(index.internal_data());
    return node.children().size();
}

int ProfileModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String ProfileModel::column_name(int column) const
{
    switch (column) {
    case Column::SampleCount:
        return m_profile.show_percentages() ? "% Samples" : "# Samples";
    case Column::SelfCount:
        return m_profile.show_percentages() ? "% Self" : "# Self";
    case Column::ObjectName:
        return "Object";
    case Column::StackFrame:
        return "Stack Frame";
    default:
        VERIFY_NOT_REACHED();
        return {};
    }
}

GUI::Variant ProfileModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* node = static_cast<ProfileNode*>(index.internal_data());
    if (role == GUI::ModelRole::TextAlignment) {
        if (index.column() == Column::SampleCount || index.column() == Column::SelfCount)
            return Gfx::TextAlignment::CenterRight;
    }
    if (role == GUI::ModelRole::Icon) {
        if (index.column() == Column::StackFrame) {
            if (node->is_root()) {
                return GUI::FileIconProvider::icon_for_executable(node->process().executable);
            }
            if (node->address() >= 0xc0000000)
                return m_kernel_frame_icon;
            return m_user_frame_icon;
        }
        return {};
    }
    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return ((float)node->event_count() / (float)m_profile.filtered_event_indices().size()) * 100.0f;
            return node->event_count();
        }
        if (index.column() == Column::SelfCount) {
            if (m_profile.show_percentages())
                return ((float)node->self_count() / (float)m_profile.filtered_event_indices().size()) * 100.0f;
            return node->self_count();
        }
        if (index.column() == Column::ObjectName)
            return node->object_name();
        if (index.column() == Column::StackFrame) {
            if (node->is_root()) {
                return String::formatted("{} ({})", node->process().basename, node->process().pid);
            }
            return node->symbol();
        }
        return {};
    }
    return {};
}

void ProfileModel::update()
{
    did_update(Model::InvalidateAllIndices);
}

}
