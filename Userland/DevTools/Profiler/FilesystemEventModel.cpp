/*
 * Copyright (c) 2022-2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilesystemEventModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <LibGUI/FileIconProvider.h>
#include <LibSymbolication/Symbolication.h>
#include <stdio.h>

namespace Profiler {

FileEventModel::FileEventModel(Profile& profile)
    : m_profile(profile)
{
}

FileEventModel::~FileEventModel()
{
}

u64 FileEventNode::total_count() const
{
    return m_open.count + m_close.count + m_readv.count + m_read.count + m_pread.count;
}

Duration FileEventNode::total_duration() const
{
    return m_open.duration + m_close.duration + m_readv.duration + m_read.duration + m_pread.duration;
}

FileEventNode& FileEventNode::find_or_create_node(ByteString const& searched_path)
{
    // TODO: Optimize this function.
    if (searched_path == ""sv || searched_path == "/"sv) {
        return *this;
    }

    auto lex_path = LexicalPath(searched_path);
    auto parts = lex_path.parts();
    auto current = parts.take_first();

    StringBuilder sb;
    sb.join('/', parts);
    auto new_s = sb.to_byte_string();

    for (auto& child : m_children) {
        if (child->m_path == current) {
            return child->find_or_create_node(new_s);
        }
    }

    if (m_parent) {
        for (auto& child : m_children) {
            if (child->m_path == current) {
                return child->find_or_create_node(new_s);
            }
        }
        return create_recursively(searched_path);
    } else {
        if (!searched_path.starts_with("/"sv)) {
            m_children.append(create(searched_path, this));
            return *m_children.last();
        }

        return create_recursively(searched_path);
    }
}

FileEventNode& FileEventNode::create_recursively(ByteString new_path)
{
    auto const lex_path = LexicalPath(new_path);
    auto parts = lex_path.parts();

    if (parts.size() == 1) {
        auto new_node = FileEventNode::create(parts.take_first(), this);
        m_children.append(new_node);
        return *new_node.ptr();
    } else {
        auto new_node = FileEventNode::create(parts.take_first(), this);
        m_children.append(new_node);

        StringBuilder sb;
        sb.join('/', parts);

        return new_node->create_recursively(sb.to_byte_string());
    }
}

void FileEventNode::for_each_parent_node(Function<void(FileEventNode&)> callback)
{
    auto* current = this;
    while (current) {
        callback(*current);
        current = current->m_parent;
    }
}

GUI::ModelIndex FileEventModel::index(int row, int column, GUI::ModelIndex const& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, m_profile.file_event_nodes()->children()[row].ptr());
    }
    auto& remote_parent = *static_cast<FileEventNode*>(parent.internal_data());
    return create_index(row, column, remote_parent.children().at(row).ptr());
}

GUI::ModelIndex FileEventModel::parent_index(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *static_cast<FileEventNode*>(index.internal_data());
    if (!node.parent())
        return {};

    if (node.parent()->parent()) {
        auto const& children = node.parent()->parent()->children();

        for (size_t row = 0; row < children.size(); ++row) {
            if (children.at(row).ptr() == node.parent()) {
                return create_index(row, index.column(), node.parent());
            }
        }
    }

    auto const& children = node.parent()->children();

    for (size_t row = 0; row < children.size(); ++row) {
        if (children.at(row).ptr() == &node) {
            return create_index(row, index.column(), node.parent());
        }
    }

    VERIFY_NOT_REACHED();
    return {};
}

int FileEventModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_profile.file_event_nodes()->children().size();
    auto& node = *static_cast<FileEventNode*>(index.internal_data());
    return node.children().size();
}

int FileEventModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

ErrorOr<String> FileEventModel::column_name(int column) const
{
    switch (column) {
    case Column::Path:
        return "Path"_string;
    case Column::TotalCount:
        return "Total Count"_string;
    case Column::TotalDuration:
        return "Total Duration [ms]"_string;
    case Column::OpenCount:
        return "Open Count"_string;
    case Column::OpenDuration:
        return "Open Duration [ms]"_string;
    case Column::CloseCount:
        return "Close Count"_string;
    case Column::CloseDuration:
        return "Close Duration [ms]"_string;
    case Column::ReadvCount:
        return "Readv Count"_string;
    case Column::ReadvDuration:
        return "Readv Duration [ms]"_string;
    case Column::ReadCount:
        return "Read Count"_string;
    case Column::ReadDuration:
        return "Read Duration [ms]"_string;
    case Column::PreadCount:
        return "Pread Count"_string;
    case Column::PreadDuration:
        return "Pread Duration [ms]"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant FileEventModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment) {
        if (index.column() == Path)
            return Gfx::TextAlignment::CenterLeft;
        return Gfx::TextAlignment::CenterRight;
    }

    auto* node = static_cast<FileEventNode*>(index.internal_data());

    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Path:
            return node->path();
        case Column::TotalCount:
            return node->total_count();
        case Column::TotalDuration:
            return static_cast<f32>(node->total_duration().to_nanoseconds()) / 1'000'000;
        case Column::OpenCount:
            return node->open().count;
        case Column::OpenDuration:
            return static_cast<f32>(node->open().duration.to_nanoseconds()) / 1'000'000;
        case Column::CloseCount:
            return node->close().count;
        case Column::CloseDuration:
            return static_cast<f32>(node->close().duration.to_nanoseconds()) / 1'000'000;
        case Column::ReadvCount:
            return node->readv().count;
        case Column::ReadvDuration:
            return static_cast<f32>(node->readv().duration.to_nanoseconds()) / 1'000'000;
        case Column::ReadCount:
            return node->read().count;
        case Column::ReadDuration:
            return static_cast<f32>(node->read().duration.to_nanoseconds()) / 1'000'000;
        case Column::PreadCount:
            return node->pread().count;
        case Column::PreadDuration:
            return static_cast<f32>(node->pread().duration.to_nanoseconds()) / 1'000'000;
        default:
            return {};
        }
    }

    return {};
}

}
