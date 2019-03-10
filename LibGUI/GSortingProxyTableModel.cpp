#include <LibGUI/GSortingProxyTableModel.h>
#include <AK/QuickSort.h>
#include <stdlib.h>
#include <stdio.h>

GSortingProxyTableModel::GSortingProxyTableModel(OwnPtr<GTableModel>&& target)
    : m_target(move(target))
    , m_key_column(-1)
{
    m_target->on_model_update = [this] (GTableModel&) {
        resort();
    };
}

GSortingProxyTableModel::~GSortingProxyTableModel()
{
}

int GSortingProxyTableModel::row_count() const
{
    return target().row_count();
}

int GSortingProxyTableModel::column_count() const
{
    return target().column_count();
}

GModelIndex GSortingProxyTableModel::map_to_target(const GModelIndex& index) const
{
    if (!index.is_valid())
        return { };
    if (index.row() >= row_count() || index.column() >= column_count())
        return { };
    return { m_row_mappings[index.row()], index.column() };
}

String GSortingProxyTableModel::row_name(int index) const
{
    return target().row_name(index);
}

String GSortingProxyTableModel::column_name(int index) const
{
    return target().column_name(index);
}

GTableModel::ColumnMetadata GSortingProxyTableModel::column_metadata(int index) const
{
    return target().column_metadata(index);
}

GVariant GSortingProxyTableModel::data(const GModelIndex& index, Role role) const
{
    return target().data(map_to_target(index), role);
}

void GSortingProxyTableModel::activate(const GModelIndex& index)
{
    target().activate(map_to_target(index));
}

void GSortingProxyTableModel::update()
{
    target().update();
}

void GSortingProxyTableModel::set_key_column_and_sort_order(int column, GSortOrder sort_order)
{
    if (column == m_key_column && sort_order == m_sort_order)
        return;

    ASSERT(column >= 0 && column < column_count());
    m_key_column = column;
    m_sort_order = sort_order;
    resort();
}

void GSortingProxyTableModel::resort()
{
    int previously_selected_target_row = map_to_target(selected_index()).row();
    int row_count = target().row_count();
    m_row_mappings.resize(row_count);
    for (int i = 0; i < row_count; ++i)
        m_row_mappings[i] = i;
    if (m_key_column == -1)
        return;
    quick_sort(m_row_mappings.begin(), m_row_mappings.end(), [&] (auto row1, auto row2) -> bool {
        auto data1 = target().data({ row1, m_key_column }, GTableModel::Role::Sort);
        auto data2 = target().data({ row2, m_key_column }, GTableModel::Role::Sort);
        if (data1 == data2)
            return 0;
        bool is_less_than = data1 < data2;
        return m_sort_order == GSortOrder::Ascending ? is_less_than : !is_less_than;
    });
    if (previously_selected_target_row != -1) {
        // Preserve selection.
        for (int i = 0; i < row_count; ++i) {
            if (m_row_mappings[i] == previously_selected_target_row) {
                set_selected_index({ i, 0 });
                break;
            }
        }
    }

    did_update();
}
