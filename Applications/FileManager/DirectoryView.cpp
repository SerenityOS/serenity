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

#include "DirectoryView.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibGUI/GSortingProxyModel.h>
#include <stdio.h>
#include <unistd.h>

// FIXME: Remove this hackery once printf() supports floats.
static String number_string_with_one_decimal(float number, const char* suffix)
{
    float decimals = number - (int)number;
    return String::format("%d.%d %s", (int)number, (int)(decimals * 10), suffix);
}

static String human_readable_size(size_t size)
{
    if (size < 1 * KB)
        return String::format("%zu bytes", size);
    if (size < 1 * MB)
        return number_string_with_one_decimal((float)size / (float)KB, "KB");
    if (size < 1 * GB)
        return number_string_with_one_decimal((float)size / (float)MB, "MB");
    return number_string_with_one_decimal((float)size / (float)GB, "GB");
}

void DirectoryView::handle_activation(const GModelIndex& index)
{
    if (!index.is_valid())
        return;
    dbgprintf("on activation: %d,%d, this=%p, m_model=%p\n", index.row(), index.column(), this, m_model.ptr());
    auto& node = model().node(index);
    auto path = node.full_path(model());

    struct stat st;
    if (stat(path.characters(), &st) < 0) {
        perror("stat");
        return;
    }

    if (S_ISDIR(st.st_mode)) {
        open(path);
        return;
    }

    // FIXME: This doesn't seem like the right way to fully detect executability.
    if (st.st_mode & S_IXUSR) {
        if (fork() == 0) {
            int rc = execl(path.characters(), path.characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (path.to_lowercase().ends_with(".png")) {
        if (fork() == 0) {
            int rc = execl("/bin/qs", "/bin/qs", path.characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (path.to_lowercase().ends_with(".html")) {
        if (fork() == 0) {
            int rc = execl("/bin/Browser", "/bin/Browser", path.characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (path.to_lowercase().ends_with(".wav")) {
        if (fork() == 0) {
            int rc = execl("/bin/SoundPlayer", "/bin/SoundPlayer", path.characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (fork() == 0) {
        int rc = execl("/bin/TextEditor", "/bin/TextEditor", path.characters(), nullptr);
        if (rc < 0)
            perror("exec");
        ASSERT_NOT_REACHED();
    }
};

DirectoryView::DirectoryView(GWidget* parent)
    : GStackWidget(parent)
    , m_model(GFileSystemModel::create())
{
    set_active_widget(nullptr);
    m_item_view = GItemView::construct(this);
    m_item_view->set_model(model());

    m_columns_view = GColumnsView::construct(this);
    m_columns_view->set_model(model());

    m_table_view = GTableView::construct(this);
    m_table_view->set_model(GSortingProxyModel::create(m_model));

    m_table_view->model()->set_key_column_and_sort_order(GFileSystemModel::Column::Name, GSortOrder::Ascending);

    m_item_view->set_model_column(GFileSystemModel::Column::Name);
    m_columns_view->set_model_column(GFileSystemModel::Column::Name);

    m_model->on_root_path_change = [this] {
        m_table_view->selection().clear();
        m_item_view->selection().clear();
        if (on_path_change)
            on_path_change(model().root_path());
    };

    //  NOTE: We're using the on_update hook on the GSortingProxyModel here instead of
    //        the GFileSystemModel's hook. This is because GSortingProxyModel has already
    //        installed an on_update hook on the GFileSystemModel internally.
    // FIXME: This is an unfortunate design. We should come up with something better.
    m_table_view->model()->on_update = [this] {
        for_each_view_implementation([](auto& view) {
            view.selection().clear();
        });
        update_statusbar();
    };

    m_model->on_thumbnail_progress = [this](int done, int total) {
        if (on_thumbnail_progress)
            on_thumbnail_progress(done, total);
    };

    m_item_view->on_activation = [&](const GModelIndex& index) {
        handle_activation(index);
    };
    m_columns_view->on_activation = [&](const GModelIndex& index) {
        handle_activation(index);
    };
    m_table_view->on_activation = [&](auto& index) {
        auto& filter_model = (GSortingProxyModel&)*m_table_view->model();
        handle_activation(filter_model.map_to_target(index));
    };

    m_table_view->on_selection_change = [this] {
        update_statusbar();
        if (on_selection_change)
            on_selection_change(*m_table_view);
    };
    m_item_view->on_selection_change = [this] {
        update_statusbar();
        if (on_selection_change)
            on_selection_change(*m_item_view);
    };
    m_columns_view->on_selection_change = [this] {
        update_statusbar();
        if (on_selection_change)
            on_selection_change(*m_columns_view);
    };

    m_table_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_table_view, index, event);
    };
    m_item_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_item_view, index, event);
    };
    m_columns_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_columns_view, index, event);
    };

    set_view_mode(ViewMode::Icon);
}

DirectoryView::~DirectoryView()
{
}

void DirectoryView::set_view_mode(ViewMode mode)
{
    if (m_view_mode == mode)
        return;
    m_view_mode = mode;
    update();
    if (mode == ViewMode::List) {
        set_active_widget(m_table_view);
        return;
    }
    if (mode == ViewMode::Columns) {
        set_active_widget(m_columns_view);
        return;
    }
    if (mode == ViewMode::Icon) {
        set_active_widget(m_item_view);
        return;
    }
    ASSERT_NOT_REACHED();
}

void DirectoryView::add_path_to_history(const StringView& path)
{
    if (m_path_history_position < m_path_history.size())
        m_path_history.resize(m_path_history_position + 1);

    m_path_history.append(path);
    m_path_history_position = m_path_history.size() - 1;
}

void DirectoryView::open(const StringView& path)
{
    add_path_to_history(path);
    model().set_root_path(path);
}

void DirectoryView::set_status_message(const StringView& message)
{
    if (on_status_message)
        on_status_message(message);
}

void DirectoryView::open_parent_directory()
{
    auto path = String::format("%s/..", model().root_path().characters());
    add_path_to_history(path);
    model().set_root_path(path);
}

void DirectoryView::refresh()
{
    model().update();
}

void DirectoryView::open_previous_directory()
{
    if (m_path_history_position > 0) {
        m_path_history_position--;
        model().set_root_path(m_path_history[m_path_history_position]);
    }
}
void DirectoryView::open_next_directory()
{
    if (m_path_history_position < m_path_history.size() - 1) {
        m_path_history_position++;
        model().set_root_path(m_path_history[m_path_history_position]);
    }
}

void DirectoryView::update_statusbar()
{
    size_t total_size = model().node({}).total_size;
    if (current_view().selection().is_empty()) {
        set_status_message(String::format("%d item%s (%s)",
            model().row_count(),
            model().row_count() != 1 ? "s" : "",
            human_readable_size(total_size).characters()));
        return;
    }

    int selected_item_count = current_view().selection().size();
    size_t selected_byte_count = 0;

    current_view().selection().for_each_index([&](auto& index) {
        auto& model = *current_view().model();
        auto size_index = model.sibling(index.row(), GFileSystemModel::Column::Size, model.parent_index(index));
        auto file_size = model.data(size_index).to_i32();
        selected_byte_count += file_size;
    });

    StringBuilder builder;
    builder.append(String::number(selected_item_count));
    builder.append(" item");
    if (selected_item_count != 1)
        builder.append('s');
    builder.append(" selected (");
    builder.append(human_readable_size(selected_byte_count).characters());
    builder.append(')');

    if (selected_item_count == 1) {
        auto index = current_view().selection().first();

        // FIXME: This is disgusting. This code should not even be aware that there is a GSortingProxyModel in the table view.
        if (m_view_mode == ViewMode::List) {
            auto& filter_model = (GSortingProxyModel&)*m_table_view->model();
            index = filter_model.map_to_target(index);
        }

        auto& node = model().node(index);
        if (!node.symlink_target.is_empty()) {
            builder.append(" -> ");
            builder.append(node.symlink_target);
        }
    }

    set_status_message(builder.to_string());
}
