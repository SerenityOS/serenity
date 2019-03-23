#include "IRCChannelMemberListModel.h"
#include "IRCChannel.h"
#include <stdio.h>
#include <time.h>

IRCChannelMemberListModel::IRCChannelMemberListModel(IRCChannel& channel)
    : m_channel(channel)
{
    set_activates_on_selection(true);
}

IRCChannelMemberListModel::~IRCChannelMemberListModel()
{
}

int IRCChannelMemberListModel::row_count() const
{
    return m_channel.member_count();
}

int IRCChannelMemberListModel::column_count() const
{
    return 1;
}

String IRCChannelMemberListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name: return "Name";
    }
    ASSERT_NOT_REACHED();
}

GModel::ColumnMetadata IRCChannelMemberListModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Name: return { 70, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCChannelMemberListModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        switch (index.column()) {
        case Column::Name: return m_channel.member_at(index.row());
        }
    }
    return { };
}

void IRCChannelMemberListModel::update()
{
    did_update();
}

void IRCChannelMemberListModel::activate(const GModelIndex& index)
{
    if (on_activation)
        on_activation(m_channel.member_at(index.row()));
}
