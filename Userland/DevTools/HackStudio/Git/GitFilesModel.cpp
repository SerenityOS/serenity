/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitFilesModel.h"

namespace HackStudio {

NonnullRefPtr<GitFilesModel> GitFilesModel::create(Vector<LexicalPath>&& files)
{
    return adopt(*new GitFilesModel(move(files)));
}

GitFilesModel::GitFilesModel(Vector<LexicalPath>&& files)
    : m_files(move(files))
{
}

GUI::Variant GitFilesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::Display) {
        return m_files.at(index.row()).string();
    }
    return {};
}

GUI::ModelIndex GitFilesModel::index(int row, int column, const GUI::ModelIndex&) const
{
    if (row < 0 || row >= static_cast<int>(m_files.size()))
        return {};
    return create_index(row, column, &m_files.at(row));
}

};
