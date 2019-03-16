#include "IRCWindowListModel.h"
#include "IRCWindow.h"
#include "IRCClient.h"
#include <stdio.h>
#include <time.h>

IRCWindowListModel::IRCWindowListModel(IRCClient& client)
    : m_client(client)
{
    set_activates_on_selection(true);
}

IRCWindowListModel::~IRCWindowListModel()
{
}

int IRCWindowListModel::row_count() const
{
    return m_client.window_count();
}

int IRCWindowListModel::column_count() const
{
    return 1;
}

String IRCWindowListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name: return "Name";
    }
    ASSERT_NOT_REACHED();
}

GTableModel::ColumnMetadata IRCWindowListModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Name: return { 70, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCWindowListModel::data(const GModelIndex& index, Role) const
{
    switch (index.column()) {
    case Column::Name: {
        auto& window = m_client.window_at(index.row());
        if (!window.unread_count())
            return window.name();
        return String::format("%s (%d)\n", window.name().characters(), window.unread_count());
    }
    }
    ASSERT_NOT_REACHED();
}

void IRCWindowListModel::update()
{
    did_update();
}

void IRCWindowListModel::activate(const GModelIndex& index)
{
    if (on_activation)
        on_activation(m_client.window_at(index.row()));
}
