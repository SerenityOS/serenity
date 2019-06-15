#include "IRCChannelMemberListModel.h"
#include "IRCChannel.h"
#include <stdio.h>
#include <time.h>

IRCChannelMemberListModel::IRCChannelMemberListModel(IRCChannel& channel)
    : m_channel(channel)
{
}

IRCChannelMemberListModel::~IRCChannelMemberListModel()
{
}

int IRCChannelMemberListModel::row_count(const GModelIndex&) const
{
    return m_channel.member_count();
}

int IRCChannelMemberListModel::column_count(const GModelIndex&) const
{
    return 1;
}

String IRCChannelMemberListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    }
    ASSERT_NOT_REACHED();
}

GModel::ColumnMetadata IRCChannelMemberListModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Name:
        return { 70, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCChannelMemberListModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Name:
            return m_channel.member_at(index.row());
        }
    }
    return {};
}

void IRCChannelMemberListModel::update()
{
    did_update();
}
