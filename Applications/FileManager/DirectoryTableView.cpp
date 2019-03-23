#include "DirectoryTableView.h"
#include <LibGUI/GSortingProxyModel.h>

DirectoryView::DirectoryView(GWidget* parent)
    : GStackWidget(parent)
    , m_model(DirectoryModel::create())
{
    set_active_widget(nullptr);
    m_item_view = new GItemView(this);
    m_item_view->set_model(model());

    m_table_view = new GTableView(this);
    m_table_view->set_model(GSortingProxyModel::create(m_model.copy_ref()));

    model().set_key_column_and_sort_order(DirectoryModel::Column::Name, GSortOrder::Ascending);

    m_item_view->set_model_column(DirectoryModel::Column::Name);

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
