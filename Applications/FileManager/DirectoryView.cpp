#include "DirectoryView.h"
#include <LibGUI/GSortingProxyModel.h>
#include <AK/FileSystemPath.h>
#include <unistd.h>
#include <stdio.h>

void DirectoryView::handle_activation(const GModelIndex& index)
{
    if (!index.is_valid())
        return;
    dbgprintf("on activation: %d,%d, this=%p, m_model=%p\n", index.row(), index.column(), this, m_model.ptr());
    auto& entry = model().entry(index.row());
    FileSystemPath path(String::format("%s/%s", model().path().characters(), entry.name.characters()));
    if (entry.is_directory()) {
        open(path.string());
        return;
    }
    if (entry.is_executable()) {
        if (fork() == 0) {
            int rc = execl(path.string().characters(), path.string().characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (path.string().to_lowercase().ends_with(".png")) {
        if (fork() == 0) {
            int rc = execl("/bin/qs", "/bin/qs", path.string().characters(), nullptr);
            if (rc < 0)
                perror("exec");
            ASSERT_NOT_REACHED();
        }
        return;
    }

    if (fork() == 0) {
        int rc = execl("/bin/TextEditor", "/bin/TextEditor", path.string().characters(), nullptr);
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
    m_table_view->set_model(GSortingProxyModel::create(m_model.copy_ref()));

    m_table_view->model()->set_key_column_and_sort_order(GDirectoryModel::Column::Name, GSortOrder::Ascending);

    m_item_view->set_model_column(GDirectoryModel::Column::Name);

    m_item_view->on_model_notification = [this] (const GModelNotification& notification) {
        if (notification.type() == GModelNotification::Type::ModelUpdated) {
            set_status_message(String::format("%d item%s (%u byte%s)",
                                              model().row_count(),
                                              model().row_count() != 1 ? "s" : "",
                                              model().bytes_in_files(),
                                              model().bytes_in_files() != 1 ? "s" : ""));

            if (on_path_change)
                on_path_change(model().path());
        }
    };

    m_model->on_thumbnail_progress = [this] (int done, int total) {
        if (on_thumbnail_progress)
            on_thumbnail_progress(done, total);
    };

    m_item_view->on_activation = [&] (const GModelIndex& index) {
        handle_activation(index);
    };
    m_table_view->on_activation = [&] (auto& index) {
        auto& filter_model = (GSortingProxyModel&)*m_table_view->model();
        handle_activation(filter_model.map_to_target(index));
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

void DirectoryView::open(const String& path)
{
    model().open(path);
}

void DirectoryView::set_status_message(const String& message)
{
    if (on_status_message)
        on_status_message(message);
}

void DirectoryView::open_parent_directory()
{
    model().open(String::format("%s/..", model().path().characters()));
}

void DirectoryView::refresh()
{
    model().update();
}
