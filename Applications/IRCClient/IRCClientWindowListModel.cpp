#include "IRCClientWindowListModel.h"
#include "IRCClientWindow.h"
#include "IRCClient.h"
#include <stdio.h>
#include <time.h>

IRCClientWindowListModel::IRCClientWindowListModel(IRCClient& client)
    : m_client(client)
{
}

IRCClientWindowListModel::~IRCClientWindowListModel()
{
}

int IRCClientWindowListModel::row_count() const
{
    return m_client.window_count();
}

int IRCClientWindowListModel::column_count() const
{
    return 1;
}

String IRCClientWindowListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name: return "Name";
    }
    ASSERT_NOT_REACHED();
}

GTableModel::ColumnMetadata IRCClientWindowListModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Name: return { 70, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCClientWindowListModel::data(const GModelIndex& index, Role) const
{
    switch (index.column()) {
    case Column::Name: return m_client.window_at(index.row()).name();
    }
    ASSERT_NOT_REACHED();
}

void IRCClientWindowListModel::update()
{
    did_update();
}

void IRCClientWindowListModel::activate(const GModelIndex& index)
{
    if (on_activation)
        on_activation(m_client.window_at(index.row()));
}
