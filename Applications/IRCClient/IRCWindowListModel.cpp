#include "IRCWindowListModel.h"
#include "IRCChannel.h"
#include "IRCClient.h"
#include "IRCWindow.h"
#include <stdio.h>
#include <time.h>

IRCWindowListModel::IRCWindowListModel(IRCClient& client)
    : m_client(client)
{
}

IRCWindowListModel::~IRCWindowListModel()
{
}

int IRCWindowListModel::row_count(const GModelIndex&) const
{
    return m_client.window_count();
}

int IRCWindowListModel::column_count(const GModelIndex&) const
{
    return 1;
}

String IRCWindowListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    }
    ASSERT_NOT_REACHED();
}

GModel::ColumnMetadata IRCWindowListModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Name:
        return { 70, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCWindowListModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Name: {
            auto& window = m_client.window_at(index.row());
            if (window.unread_count())
                return String::format("%s (%d)", window.name().characters(), window.unread_count());
            return window.name();
        }
        }
    }
    if (role == Role::ForegroundColor) {
        switch (index.column()) {
        case Column::Name: {
            auto& window = m_client.window_at(index.row());
            if (window.unread_count())
                return Color(Color::Red);
            if (!window.channel().is_open())
                return Color(Color::WarmGray);
            return Color(Color::Black);
        }
        }
    }
    return {};
}

void IRCWindowListModel::update()
{
    did_update();
}
