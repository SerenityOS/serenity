/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Model.h>

class IRCChannel;

class IRCChannelMemberListModel final : public GUI::Model {
public:
    enum Column {
        Name
    };
    static NonnullRefPtr<IRCChannelMemberListModel> create(IRCChannel& channel) { return adopt_ref(*new IRCChannelMemberListModel(channel)); }
    virtual ~IRCChannelMemberListModel() override;

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void update() override;
    virtual String nick_at(const GUI::ModelIndex& index) const;

private:
    explicit IRCChannelMemberListModel(IRCChannel&);

    IRCChannel& m_channel;
};
