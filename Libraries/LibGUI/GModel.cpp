#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>

GModel::GModel()
{
}

GModel::~GModel()
{
}

void GModel::register_view(Badge<GAbstractView>, GAbstractView& view)
{
    m_views.set(&view);
}

void GModel::unregister_view(Badge<GAbstractView>, GAbstractView& view)
{
    m_views.remove(&view);
}

void GModel::for_each_view(Function<void(GAbstractView&)> callback)
{
    for (auto* view : m_views)
        callback(*view);
}

void GModel::did_update()
{
    if (on_update)
        on_update();
    for_each_view([](auto& view) {
        view.did_update_model();
    });
}

void GModel::set_selected_index(const GModelIndex& index)
{
    if (m_selected_index == index)
        return;
    m_selected_index = index;
    if (on_selection_changed)
        on_selection_changed(index);
    for_each_view([](auto& view) {
        view.did_update_selection();
    });
}

GModelIndex GModel::create_index(int row, int column, const void* data) const
{
    return GModelIndex(*this, row, column, const_cast<void*>(data));
}

GModelIndex GModel::sibling(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid())
        return {};
    int row_count = this->row_count(parent);
    if (row < 0 || row > row_count)
        return {};
    return index(row, column, parent);
}
