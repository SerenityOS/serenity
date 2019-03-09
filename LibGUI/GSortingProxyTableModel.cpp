#include <LibGUI/GSortingProxyTableModel.h>
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
    ASSERT(!m_row_mappings.is_empty());
    if (!index.is_valid()) {
        ASSERT_NOT_REACHED();
        return { };
    }
    if (index.row() >= row_count() || index.column() >= column_count()) {
        ASSERT_NOT_REACHED();
        return { };
    }
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

GVariant GSortingProxyTableModel::data(const GModelIndex& index) const
{
    return target().data(map_to_target(index));
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
    int row_count = target().row_count();
    m_row_mappings.resize(row_count);
    for (int i = 0; i < row_count; ++i)
        m_row_mappings[i] = i;

    if (m_key_column == -1)
        return;

    struct Context {
        GTableModel* target;
        int key_column;
        GSortOrder sort_order;
    };
    Context context { m_target.ptr(), m_key_column, m_sort_order };
    qsort_r(m_row_mappings.data(), m_row_mappings.size(), sizeof(int), [] (const void* a, const void* b, void* ctx) -> int {
        int row1 = *(const int*)(a);
        int row2 = *(const int*)(b);
        auto& context = *(Context*)(ctx);
        GModelIndex index1 { row1, context.key_column };
        GModelIndex index2 { row2, context.key_column };
        auto data1 = context.target->data(index1);
        auto data2 = context.target->data(index2);
        if (data1 == data2)
            return 0;
        bool is_less_than = data1 < data2;
        if (context.sort_order == GSortOrder::Ascending)
            return is_less_than ? -1 : 1;
        return is_less_than ? 1 : -1;
    }, &context);

    did_update();
}
