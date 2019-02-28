#include <LibGUI/GTableModel.h>
#include <LibGUI/GTableView.h>

GTableModel::GTableModel()
{
}

GTableModel::~GTableModel()
{
}

void GTableModel::register_view(Badge<GTableView>, GTableView& view)
{
    m_views.set(&view);
}

void GTableModel::unregister_view(Badge<GTableView>, GTableView& view)
{
    m_views.remove(&view);
}

void GTableModel::for_each_view(Function<void(GTableView&)> callback)
{
    for (auto* view : m_views)
        callback(*view);
}

void GTableModel::did_update()
{
    for_each_view([] (GTableView& view) {
        view.did_update_model();
    });
}
