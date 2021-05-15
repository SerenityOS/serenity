/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

int IRCChannelMemberListModel::row_count(const GUI::ModelIndex&) const
{
    return m_channel.member_count();
}

int IRCChannelMemberListModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

String IRCChannelMemberListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    }
    VERIFY_NOT_REACHED();
}

GUI::Variant IRCChannelMemberListModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;
    if (role == GUI::ModelRole::Display) {
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

String IRCChannelMemberListModel::nick_at(const GUI::ModelIndex& index) const
{
    return data(index, GUI::ModelRole::Display).to_string();
}
