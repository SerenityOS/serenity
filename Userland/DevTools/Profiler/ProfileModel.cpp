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

#include "ProfileModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>

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
        ASSERT_NOT_REACHED();
        return {};
    }

    for (size_t row = 0; row < node.parent()->parent()->children().size(); ++row) {
        if (node.parent()->parent()->children()[row].ptr() == node.parent())
            return create_index(row, index.column(), node.parent());
    }

    ASSERT_NOT_REACHED();
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
    case Column::StackFrame:
        return "Stack Frame";
    default:
        ASSERT_NOT_REACHED();
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
            if (node->address() >= 0xc0000000)
                return m_kernel_frame_icon;
            return m_user_frame_icon;
        }
        return {};
    }
    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return ((float)node->event_count() / (float)m_profile.filtered_event_count()) * 100.0f;
            return node->event_count();
        }
        if (index.column() == Column::SelfCount) {
            if (m_profile.show_percentages())
                return ((float)node->self_count() / (float)m_profile.filtered_event_count()) * 100.0f;
            return node->self_count();
        }
        if (index.column() == Column::StackFrame)
            return node->symbol();
        return {};
    }
    return {};
}

void ProfileModel::update()
{
    did_update(Model::InvalidateAllIndexes);
}
