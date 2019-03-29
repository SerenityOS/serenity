#include <LibGUI/GModel.h>
#include <LibGUI/GAbstractView.h>

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
    if (on_model_update)
        on_model_update(*this);
    for_each_view([] (auto& view) {
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
    if (m_activates_on_selection && is_valid(index))
        activate(index);
}

GModelIndex GModel::create_index(int row, int column, void* data) const
{
    return GModelIndex(*this, row, column, data);
}
