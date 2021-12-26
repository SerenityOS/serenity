#include "DirectoryView.h"
#include <AK/FileSystemPath.h>
#include <LibGUI/GSortingProxyModel.h>
#include <stdio.h>
#include <unistd.h>

void DirectoryView::handle_activation(const GModelIndex& index)
{
    if (!index.is_valid())
        return;
    dbgprintf("on activation: %d,%d, this=%p, m_model=%p\n", index.row(), index.column(), this, m_model.ptr());
    auto& entry = model().entry(index.row());
    auto path = canonicalized_path(String::format("%s/%s", model().path().characters(), entry.name.characters()));
    if (entry.is_directory()) {
        open(path);
        return;
    }
    if (entry.is_executable()) {
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
    , m_model(GDirectoryModel::create())
{
    set_active_widget(nullptr);
    m_item_view = new GItemView(this);
    m_item_view->set_model(model());

    m_table_view = new GTableView(this);
    m_table_view->set_model(GSortingProxyModel::create(m_model));

    m_table_view->model()->set_key_column_and_sort_order(GDirectoryModel::Column::Name, GSortOrder::Ascending);

    m_item_view->set_model_column(GDirectoryModel::Column::Name);

    m_model->on_path_change = [this] {
        m_table_view->selection().clear();
        m_item_view->selection().clear();
        if (on_path_change)
            on_path_change(model().path());
    };

    //  NOTE: We're using the on_update hook on the GSortingProxyModel here instead of
    //        the GDirectoryModel's hook. This is because GSortingProxyModel has already
    //        installed an on_update hook on the GDirectoryModel internally.
    // FIXME: This is an unfortunate design. We should come up with something better.
    m_table_view->model()->on_update = [this] {
        update_statusbar();
    };

    m_model->on_thumbnail_progress = [this](int done, int total) {
        if (on_thumbnail_progress)
            on_thumbnail_progress(done, total);
    };

    m_item_view->on_activation = [&](const GModelIndex& index) {
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

    m_table_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_table_view, index, event);
    };
    m_item_view->on_context_menu_request = [this](auto& index, auto& event) {
        if (on_context_menu_request)
            on_context_menu_request(*m_item_view, index, event);
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
    model().open(path);
}

void DirectoryView::set_status_message(const StringView& message)
{
    if (on_status_message)
        on_status_message(message);
}

void DirectoryView::open_parent_directory()
{
    auto path = String::format("%s/..", model().path().characters());
    add_path_to_history(path);
    model().open(path);
}

void DirectoryView::refresh()
{
    model().update();
}

void DirectoryView::open_previous_directory()
{
    if (m_path_history_position > 0) {
        m_path_history_position--;
        model().open(m_path_history[m_path_history_position]);
    }
}
void DirectoryView::open_next_directory()
{
    if (m_path_history_position < m_path_history.size() - 1) {
        m_path_history_position++;
        model().open(m_path_history[m_path_history_position]);
    }
}

void DirectoryView::update_statusbar()
{
    if (current_view().selection().is_empty()) {
        set_status_message(String::format("%d item%s (%u byte%s)",
            model().row_count(),
            model().row_count() != 1 ? "s" : "",
            model().bytes_in_files(),
            model().bytes_in_files() != 1 ? "s" : ""));
        return;
    }

    int selected_item_count = current_view().selection().size();
    size_t selected_byte_count = 0;

    current_view().selection().for_each_index([&](auto& index) {
        auto size_index = current_view().model()->index(index.row(), GDirectoryModel::Column::Size);
        auto file_size = current_view().model()->data(size_index).to_int();
        selected_byte_count += file_size;
    });

    set_status_message(String::format("%d item%s selected (%u byte%s)",
        selected_item_count,
        selected_item_count != 1 ? "s" : "",
        selected_byte_count,
        selected_byte_count != 1 ? "s" : ""));
}
