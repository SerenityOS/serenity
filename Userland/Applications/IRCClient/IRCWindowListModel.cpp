/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IRCWindowListModel.h"
#include "IRCChannel.h"
#include "IRCClient.h"

IRCWindowListModel::IRCWindowListModel(IRCClient& client)
    : m_client(client)
{
}

IRCWindowListModel::~IRCWindowListModel()
{
}

int IRCWindowListModel::row_count(const GUI::ModelIndex&) const
{
    return m_client->window_count();
}

int IRCWindowListModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

String IRCWindowListModel::column_name(int column) const
{
    switch (column) {
    case Column::Name:
        return "Name";
    }
    VERIFY_NOT_REACHED();
}

GUI::Variant IRCWindowListModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Name: {
            auto& window = m_client->window_at(index.row());
            if (window.unread_count())
                return String::formatted("{} ({})", window.name(), window.unread_count());
            return window.name();
        }
        }
    }
    if (role == GUI::ModelRole::ForegroundColor) {
        switch (index.column()) {
        case Column::Name: {
            auto& window = m_client->window_at(index.row());
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
