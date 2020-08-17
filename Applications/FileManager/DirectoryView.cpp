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
#include <AK/NumberFormat.h>
#include <AK/StringBuilder.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SortingProxyModel.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

NonnullRefPtr<GUI::Action> LauncherHandler::create_launch_action(Function<void(const LauncherHandler&)> launch_handler)
{
    RefPtr<Gfx::Bitmap> icon;
    auto icon_file = details().icons.get("16x16");
    if (icon_file.has_value())
        icon = Gfx::Bitmap::load_from_file(icon_file.value());
    return GUI::Action::create(details().name, move(icon), [this, launch_handler = move(launch_handler)](auto&) {
        launch_handler(*this);
    });
}

RefPtr<LauncherHandler> DirectoryView::get_default_launch_handler(const NonnullRefPtrVector<LauncherHandler>& handlers)
{
    // If this is an application, pick it first
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i].details().launcher_type == Desktop::Launcher::LauncherType::Application)
            return handlers[i];
    }
    // If there's a handler preferred by the user, pick this first
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i].details().launcher_type == Desktop::Launcher::LauncherType::UserPreferred)
            return handlers[i];
    }
    // Otherwise, use the user's default, if available
    for (size_t i = 0; i < handlers.size(); i++) {
        if (handlers[i].details().launcher_type == Desktop::Launcher::LauncherType::UserDefault)
            return handlers[i];
    }
    // If still no match, use the first one we find
    if (!handlers.is_empty()) {
        return handlers[0];
    }

    return {};
}

NonnullRefPtrVector<LauncherHandler> DirectoryView::get_launch_handlers(const URL& url)
{
    NonnullRefPtrVector<LauncherHandler> handlers;
    for (auto& h : Desktop::Launcher::get_handlers_with_details_for_url(url)) {
        handlers.append(adopt(*new LauncherHandler(h)));
    }
    return handlers;
}

NonnullRefPtrVector<LauncherHandler> DirectoryView::get_launch_handlers(const String& path)
{
    return get_launch_handlers(URL::create_with_file_protocol(path));
}

void DirectoryView::handle_activation(const GUI::ModelIndex& index)
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
        if (is_desktop()) {
            Desktop::Launcher::open(URL::create_with_file_protocol(path));
            return;
        }
        open(path);
        return;
    }

    auto url = URL::create_with_file_protocol(path);
    auto launcher_handlers = get_launch_handlers(url);
    auto default_launcher = get_default_launch_handler(launcher_handlers);
    if (default_launcher) {
        launch(url, *default_launcher);
    } else {
        auto error_message = String::format("Could not open %s", path.characters());
        GUI::MessageBox::show(window(), error_message, "File Manager", GUI::MessageBox::Type::Error);
    }
}

DirectoryView::DirectoryView(Mode mode)
    : m_mode(mode)
    , m_model(GUI::FileSystemModel::create())
    , m_sorting_model(GUI::SortingProxyModel::create(m_model))
{
    set_active_widget(nullptr);
    set_content_margins({ 2, 2, 2, 2 });

    setup_model();

    setup_icon_view();
    if (mode != Mode::Desktop) {
        setup_columns_view();
        setup_table_view();
    }

    set_view_mode(ViewMode::Icon);
}

void DirectoryView::setup_model()
{
    m_model->set_root_path(Core::StandardPaths::desktop_directory());

    m_model->on_error = [this](int error, const char* error_string) {
        bool quit = false;
        if (m_path_history.size())
            open(m_path_history.at(m_path_history_position));
        else
            quit = true;

        if (on_error)
            on_error(error, error_string, quit);
    };

    m_model->on_complete = [this] {
        if (m_table_view)
            m_table_view->selection().clear();
        if (m_icon_view)
            m_icon_view->selection().clear();

        add_path_to_history(model().root_path());

        if (on_path_change)
            on_path_change(model().root_path());
    };

    m_model->register_client(*this);

    m_model->on_thumbnail_progress = [this](int done, int total) {
        if (on_thumbnail_progress)
            on_thumbnail_progress(done, total);
    };
}

void DirectoryView::setup_icon_view()
{
    m_icon_view = add<GUI::IconView>();

    if (is_desktop()) {
        m_icon_view->set_frame_shape(Gfx::FrameShape::NoFrame);
        m_icon_view->set_scrollbars_enabled(false);
        m_icon_view->set_fill_with_background_color(false);
    }

    m_icon_view->set_model(m_sorting_model);
    m_icon_view->set_model_column(GUI::FileSystemModel::Column::Name);
    m_icon_view->on_activation = [&](auto& index) {
        handle_activation(map_index(index));
    };
    m_icon_view->on_selection_change = [this] {
        update_statusbar();
        if (on_selection_change)
            on_selection_change(*m_icon_view);
    };
    m_icon_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_icon_view, map_index(index), event);
    };
    m_icon_view->on_drop = [this](auto& index, auto& event) {
        if (on_drop)
            on_drop(*m_icon_view, map_index(index), event);
    };
}

void DirectoryView::setup_columns_view()
{
    m_columns_view = add<GUI::ColumnsView>();
    m_columns_view->set_model(m_sorting_model);
    m_columns_view->set_model_column(GUI::FileSystemModel::Column::Name);

    m_columns_view->on_activation = [&](auto& index) {
        handle_activation(map_index(index));
    };

    m_columns_view->on_selection_change = [this] {
        update_statusbar();
        if (on_selection_change)
            on_selection_change(*m_columns_view);
    };

    m_columns_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_columns_view, map_index(index), event);
    };

    m_columns_view->on_drop = [this](auto& index, auto& event) {
        if (on_drop)
            on_drop(*m_columns_view, map_index(index), event);
    };
}

void DirectoryView::setup_table_view()
{
    m_table_view = add<GUI::TableView>();
    m_table_view->set_model(m_sorting_model);

    m_table_view->set_key_column_and_sort_order(GUI::FileSystemModel::Column::Name, GUI::SortOrder::Ascending);

    m_table_view->on_activation = [&](auto& index) {
        handle_activation(map_index(index));
    };

    m_table_view->on_selection_change = [this] {
        update_statusbar();
        if (on_selection_change)
            on_selection_change(*m_table_view);
    };

    m_table_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_table_view, map_index(index), event);
    };

    m_table_view->on_drop = [this](auto& index, auto& event) {
        if (on_drop)
            on_drop(*m_table_view, map_index(index), event);
    };
}

DirectoryView::~DirectoryView()
{
    m_model->unregister_client(*this);
}

void DirectoryView::model_did_update(unsigned flags)
{
    if (flags & GUI::Model::UpdateFlag::InvalidateAllIndexes) {
        for_each_view_implementation([](auto& view) {
            view.selection().clear();
        });
    }
    update_statusbar();
}

void DirectoryView::set_view_mode(ViewMode mode)
{
    if (m_view_mode == mode)
        return;
    m_view_mode = mode;
    update();
    if (mode == ViewMode::Table) {
        set_active_widget(m_table_view);
        return;
    }
    if (mode == ViewMode::Columns) {
        set_active_widget(m_columns_view);
        return;
    }
    if (mode == ViewMode::Icon) {
        set_active_widget(m_icon_view);
        return;
    }
    ASSERT_NOT_REACHED();
}

void DirectoryView::add_path_to_history(const StringView& path)
{
    if (m_path_history.size() && m_path_history.at(m_path_history_position) == path)
        return;

    if (m_path_history_position < m_path_history.size())
        m_path_history.resize(m_path_history_position + 1);

    m_path_history.append(path);
    m_path_history_position = m_path_history.size() - 1;
}

void DirectoryView::open(const StringView& path)
{
    if (model().root_path() == path) {
        model().update();
        return;
    }
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

GUI::ModelIndex DirectoryView::map_index(const GUI::ModelIndex& index) const
{
    return m_sorting_model->map_to_source(index);
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
        auto size_index = model.index(index.row(), GUI::FileSystemModel::Column::Size, model.parent_index(index));
        auto file_size = size_index.data().to_i32();
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
        auto& node = model().node(map_index(current_view().selection().first()));
        if (!node.symlink_target.is_empty()) {
            builder.append(" -> ");
            builder.append(node.symlink_target);
        }
    }

    set_status_message(builder.to_string());
}

void DirectoryView::set_should_show_dotfiles(bool show_dotfiles)
{
    m_model->set_should_show_dotfiles(show_dotfiles);
}

void DirectoryView::launch(const URL&, const LauncherHandler& launcher_handler)
{
    pid_t child;
    if (launcher_handler.details().launcher_type == Desktop::Launcher::LauncherType::Application) {
        const char* argv[] = { launcher_handler.details().name.characters(), nullptr };
        posix_spawn(&child, launcher_handler.details().executable.characters(), nullptr, nullptr, const_cast<char**>(argv), environ);
        if (disown(child) < 0)
            perror("disown");
    } else {
        for (auto& path : selected_file_paths()) {
            const char* argv[] = { launcher_handler.details().name.characters(), path.characters(), nullptr };
            posix_spawn(&child, launcher_handler.details().executable.characters(), nullptr, nullptr, const_cast<char**>(argv), environ);
            if (disown(child) < 0)
                perror("disown");
        }
    }
}

Vector<String> DirectoryView::selected_file_paths() const
{
    Vector<String> paths;
    auto& view = current_view();
    auto& model = *view.model();
    view.selection().for_each_index([&](const GUI::ModelIndex& index) {
        auto parent_index = model.parent_index(index);
        auto name_index = model.index(index.row(), GUI::FileSystemModel::Column::Name, parent_index);
        auto path = name_index.data(GUI::ModelRole::Custom).to_string();
        paths.append(path);
    });
    return paths;
}
