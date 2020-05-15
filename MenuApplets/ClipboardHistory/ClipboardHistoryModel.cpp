#include "ClipboardHistoryModel.h"

NonnullRefPtr<ClipboardHistoryModel> ClipboardHistoryModel::create()
{
    return adopt(*new ClipboardHistoryModel());
}

ClipboardHistoryModel::~ClipboardHistoryModel()
{
}

String ClipboardHistoryModel::column_name(int column) const
{
    switch (column) {
    case Column::Data:
        return "Data";
    case Column::Type:
        return "Type";
    default:
        ASSERT_NOT_REACHED();
    }
}

GUI::Model::ColumnMetadata ClipboardHistoryModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Data:
        return { 200 };
    case Column::Type:
        return { 100 };
    default:
        ASSERT_NOT_REACHED();
    }
}

GUI::Variant ClipboardHistoryModel::data(const GUI::ModelIndex& index, Role) const
{
    auto& data_and_type = m_history_items[index.row()];
    switch (index.column()) {
    case Column::Data:
        if (data_and_type.type.starts_with("text/"))
            return data_and_type.data;
        return "<...>";
    case Column::Type:
        return data_and_type.type;
    default:
        ASSERT_NOT_REACHED();
    }
}

void ClipboardHistoryModel::update()
{
    did_update();
}

void ClipboardHistoryModel::add_item(const GUI::Clipboard::DataAndType& item)
{
    m_history_items.prepend(item);
    update();
}
