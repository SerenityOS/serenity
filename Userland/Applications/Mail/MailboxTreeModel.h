/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibIMAP/Objects.h>

class AccountHolder;

class MailboxTreeModel final : public GUI::Model {
public:
    static NonnullRefPtr<MailboxTreeModel> create(AccountHolder const& account_holder)
    {
        return adopt_ref(*new MailboxTreeModel(account_holder));
    }

    virtual ~MailboxTreeModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(GUI::ModelIndex const&) const override;

private:
    explicit MailboxTreeModel(AccountHolder const&);

    AccountHolder const& m_account_holder;

    GUI::Icon m_mail_icon;
    GUI::Icon m_folder_icon;
    GUI::Icon m_account_icon;
};
