/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProfileModel.h"
#include "PercentageFormatting.h"
#include "Profile.h"
#include <LibGUI/FileIconProvider.h>
#include <LibSymbolication/Symbolication.h>
#include <stdio.h>

namespace Profiler {

ProfileModel::ProfileModel(Profile& profile)
    : m_profile(profile)
{
    m_user_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"sv).release_value_but_fixme_should_propagate_errors());
    m_kernel_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object-red.png"sv).release_value_but_fixme_should_propagate_errors());
}

GUI::ModelIndex ProfileModel::index(int row, int column, GUI::ModelIndex const& parent) const
{
    if (!parent.is_valid()) {
        if (m_profile.roots().is_empty())
            return {};
        return create_index(row, column, m_profile.roots().at(row).ptr());
    }
    auto& remote_parent = *static_cast<ProfileNode*>(parent.internal_data());
    return create_index(row, column, remote_parent.children().at(row).ptr());
}

GUI::ModelIndex ProfileModel::parent_index(GUI::ModelIndex const& index) const
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

int ProfileModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_profile.roots().size();
    auto& node = *static_cast<ProfileNode*>(index.internal_data());
    return node.children().size();
}

int ProfileModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

ErrorOr<String> ProfileModel::column_name(int column) const
{
    switch (column) {
    case Column::SampleCount:
        return m_profile.show_percentages() ? "% Samples"_string : "# Samples"_string;
    case Column::SelfCount:
        return m_profile.show_percentages() ? "% Self"_string : "# Self"_string;
    case Column::ObjectName:
        return "Object"_string;
    case Column::StackFrame:
        return "Stack Frame"_string;
    case Column::SymbolAddress:
        return "Symbol Address"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant ProfileModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
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
            auto maybe_kernel_base = Symbolication::kernel_base();
            if (maybe_kernel_base.has_value() && node->address() >= maybe_kernel_base.value())
                return m_kernel_frame_icon;
            return m_user_frame_icon;
        }
        return {};
    }
    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return format_percentage(node->event_count(), m_profile.filtered_event_indices().size());
            return node->event_count();
        }
        if (index.column() == Column::SelfCount) {
            if (m_profile.show_percentages())
                return format_percentage(node->self_count(), m_profile.filtered_event_indices().size());
            return node->self_count();
        }
        if (index.column() == Column::ObjectName)
            return node->object_name();
        if (index.column() == Column::StackFrame) {
            if (node->is_root()) {
                return ByteString::formatted("{} ({})", node->process().basename, node->process().pid);
            }
            return node->symbol();
        }
        if (index.column() == Column::SymbolAddress) {
            if (node->is_root())
                return "";
            auto const* library = node->process().library_metadata.library_containing(node->address());
            if (!library)
                return "";
            return ByteString::formatted("{:p} (offset {:p})", node->address(), node->address() - library->base);
        }
        return {};
    }
    return {};
}

Vector<GUI::ModelIndex> ProfileModel::matches(StringView searching, unsigned flags, GUI::ModelIndex const& parent)
{
    RemoveReference<decltype(m_profile.roots())>* nodes { nullptr };

    if (!parent.is_valid())
        nodes = &m_profile.roots();
    else
        nodes = &static_cast<ProfileNode*>(parent.internal_data())->children();

    if (!nodes)
        return {};

    Vector<GUI::ModelIndex> found_indices;
    for (auto it = nodes->begin(); !it.is_end(); ++it) {
        GUI::ModelIndex index = this->index(it.index(), StackFrame, parent);
        if (!string_matches(data(index, GUI::ModelRole::Display).as_string(), searching, flags))
            continue;

        found_indices.append(index);
        if (flags & FirstMatchOnly)
            break;
    }
    return found_indices;
}

}
