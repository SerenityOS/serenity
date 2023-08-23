/*
 * Copyright (c) 2023, Abhishek R. <raturiabhi1000@gmail.com>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitLogModel.h"

namespace HackStudio {
NonnullRefPtr<GitLogModel> GitLogModel::create(Vector<DeprecatedString>&& commits)
{
    return adopt_ref(*new GitLogModel(move(commits)));
}

GitLogModel::GitLogModel(Vector<DeprecatedString>&& commits)
    : m_logs(move(commits))
{
}

GUI::Variant GitLogModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::Display) {
        return m_logs.at(index.row());
    }
    return {};
}

GUI::ModelIndex GitLogModel::index(int row, int column, const GUI::ModelIndex&) const
{
    if (row < 0 || row >= static_cast<int>(m_logs.size()))
        return {};
    return create_index(row, column, &m_logs.at(row));
}
}
