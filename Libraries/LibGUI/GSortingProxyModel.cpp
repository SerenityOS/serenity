#include <AK/QuickSort.h>
#include <LibGUI/GSortingProxyModel.h>
#include <stdio.h>
#include <stdlib.h>

GSortingProxyModel::GSortingProxyModel(NonnullRefPtr<GModel>&& target)
    : m_target(move(target))
    , m_key_column(-1)
{
    m_target->on_model_update = [this](GModel&) {
        resort();
    };
}

GSortingProxyModel::~GSortingProxyModel()
{
}

int GSortingProxyModel::row_count(const GModelIndex& index) const
{
    return target().row_count(index);
}

int GSortingProxyModel::column_count(const GModelIndex& index) const
{
    return target().column_count(index);
}

GModelIndex GSortingProxyModel::map_to_target(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    if (index.row() >= m_row_mappings.size() || index.column() >= column_count())
        return {};
    return target().index(m_row_mappings[index.row()], index.column());
}

String GSortingProxyModel::row_name(int index) const
{
    return target().row_name(index);
}

String GSortingProxyModel::column_name(int index) const
{
    return target().column_name(index);
}

GModel::ColumnMetadata GSortingProxyModel::column_metadata(int index) const
{
    return target().column_metadata(index);
}

GVariant GSortingProxyModel::data(const GModelIndex& index, Role role) const
{
    return target().data(map_to_target(index), role);
}

void GSortingProxyModel::update()
{
    target().update();
}

void GSortingProxyModel::set_key_column_and_sort_order(int column, GSortOrder sort_order)
{
    if (column == m_key_column && sort_order == m_sort_order)
        return;

    ASSERT(column >= 0 && column < column_count());
    m_key_column = column;
    m_sort_order = sort_order;
    resort();
}

void GSortingProxyModel::resort()
{
    int previously_selected_target_row = map_to_target(selected_index()).row();
    int row_count = target().row_count();
    m_row_mappings.resize(row_count);
    for (int i = 0; i < row_count; ++i)
        m_row_mappings[i] = i;
    if (m_key_column == -1) {
        did_update();
        return;
    }
    quick_sort(m_row_mappings.begin(), m_row_mappings.end(), [&](auto row1, auto row2) -> bool {
        auto data1 = target().data(target().index(row1, m_key_column), GModel::Role::Sort);
        auto data2 = target().data(target().index(row2, m_key_column), GModel::Role::Sort);
        if (data1 == data2)
            return 0;
        bool is_less_than = data1 < data2;
        return m_sort_order == GSortOrder::Ascending ? is_less_than : !is_less_than;
    });
    if (previously_selected_target_row != -1) {
        // Preserve selection.
        ASSERT(m_row_mappings.size() == row_count);
        for (int i = 0; i < row_count; ++i) {
            if (m_row_mappings[i] == previously_selected_target_row) {
                set_selected_index(index(i, 0));
                break;
            }
        }
    }

    did_update();
}
