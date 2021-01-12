/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    ASSERT_NOT_REACHED();
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
