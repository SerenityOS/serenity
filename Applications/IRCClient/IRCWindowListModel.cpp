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
