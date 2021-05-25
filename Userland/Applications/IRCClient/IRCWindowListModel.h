/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Model.h>

class IRCClient;
class IRCWindow;

class IRCWindowListModel final : public GUI::Model {
public:
    enum Column {
        Name,
    };

    static NonnullRefPtr<IRCWindowListModel> create(IRCClient& client) { return adopt_ref(*new IRCWindowListModel(client)); }
    virtual ~IRCWindowListModel() override;

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

private:
    explicit IRCWindowListModel(IRCClient&);

    NonnullRefPtr<IRCClient> m_client;
};
