/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DirectoryTreeView.h"
#include <LibGUI/FileSystemModel.h>

namespace FileManager {

void DirectoryTreeView::model_did_update(unsigned flags)
{
    TreeView::model_did_update(flags);

    auto file_system_model = dynamic_cast<GUI::FileSystemModel*>(model());

    auto current_path = m_directory_view->path();

    struct stat st;
    // If the directory no longer exists, we find a parent that does.
    while (stat(current_path.characters(), &st) != 0) {
        m_directory_view->open_parent_directory();
        current_path = m_directory_view->path();
        if (current_path == file_system_model->root_path()) {
            break;
        }
    }

    // Reselect the existing folder in the tree.
    auto new_index = file_system_model->index(current_path, GUI::FileSystemModel::Column::Name);
    if (new_index.is_valid()) {
        expand_all_parents_of(new_index);
        set_cursor(new_index, GUI::AbstractView::SelectionUpdate::Set, true);
    }

    m_directory_view->refresh();
}

}
