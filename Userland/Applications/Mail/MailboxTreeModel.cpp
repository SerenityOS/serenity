/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailboxTreeModel.h"
#include "AccountHolder.h"

MailboxTreeModel::MailboxTreeModel(AccountHolder const& account_holder)
    : m_account_holder(account_holder)
{
    m_mail_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/app-mail.png"));
}

MailboxTreeModel::~MailboxTreeModel()
{
}

GUI::ModelIndex MailboxTreeModel::index(int row, int column, GUI::ModelIndex const& parent) const
{
    if (!parent.is_valid()) {
        if (m_account_holder.accounts().is_empty())
            return {};
        return create_index(row, column, &m_account_holder.accounts().at(row));
    }
    auto& base_node = *static_cast<BaseNode*>(parent.internal_data());
    auto& remote_parent = verify_cast<AccountNode>(base_node);
    return create_index(row, column, &remote_parent.mailboxes().at(row));
}

GUI::ModelIndex MailboxTreeModel::parent_index(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};

    auto& base_node = *static_cast<BaseNode*>(index.internal_data());

    if (is<AccountNode>(base_node))
        return {};

    auto& mailbox_node = verify_cast<MailboxNode>(base_node);
    for (size_t row = 0; row < mailbox_node.associated_account().mailboxes().size(); ++row) {
        if (&mailbox_node.associated_account().mailboxes()[row] == &mailbox_node) {
            return create_index(row, index.column(), &mailbox_node.associated_account());
        }
    }

    VERIFY_NOT_REACHED();
    return {};
}

int MailboxTreeModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_account_holder.accounts().size();

    auto& base_node = *static_cast<BaseNode*>(index.internal_data());

    if (is<MailboxNode>(base_node))
        return 0;

    auto& node = verify_cast<AccountNode>(base_node);
    return node.mailboxes().size();
}

int MailboxTreeModel::column_count(GUI::ModelIndex const&) const
{
    return 1;
}

GUI::Variant MailboxTreeModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& base_node = *static_cast<BaseNode*>(index.internal_data());

    if (role == GUI::ModelRole::Display) {
        if (is<AccountNode>(base_node)) {
            auto& account_node = verify_cast<AccountNode>(base_node);
            return account_node.name();
        }

        auto& mailbox_node = verify_cast<MailboxNode>(base_node);
        return mailbox_node.name();
    }

    if (role == GUI::ModelRole::Icon) {
        if (is<AccountNode>(base_node)) {
            // FIXME: We could add an icon for accounts.
            return {};
        }

        return m_mail_icon;
    }

    return {};
}

void MailboxTreeModel::update()
{
    did_update();
}
