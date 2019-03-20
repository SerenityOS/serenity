#include "DirectoryTableView.h"
#include <LibGUI/GSortingProxyTableModel.h>

DirectoryTableView::DirectoryTableView(GWidget* parent)
    : GTableView(parent)
    , m_model(DirectoryTableModel::create())
{
    set_model(GSortingProxyTableModel::create(m_model.copy_ref()));
    GTableView::model()->set_key_column_and_sort_order(DirectoryTableModel::Column::Name, GSortOrder::Ascending);
}

DirectoryTableView::~DirectoryTableView()
{
}

void DirectoryTableView::open(const String& path)
{
    model().open(path);
}

void DirectoryTableView::model_notification(const GModelNotification& notification)
{
    if (notification.type() == GModelNotification::Type::ModelUpdated) {
        set_status_message(String::format("%d item%s (%u byte%s)",
        model().row_count(),
        model().row_count() != 1 ? "s" : "",
        model().bytes_in_files(),
        model().bytes_in_files() != 1 ? "s" : ""));

        if (on_path_change)
            on_path_change(model().path());
    }
}

void DirectoryTableView::set_status_message(const String& message)
{
    if (on_status_message)
        on_status_message(message);
}

void DirectoryTableView::open_parent_directory()
{
    model().open(String::format("%s/..", model().path().characters()));
}
