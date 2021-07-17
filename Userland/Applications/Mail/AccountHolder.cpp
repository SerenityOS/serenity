/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AccountHolder.h"

AccountHolder::AccountHolder()
{
    m_mailbox_tree_model = MailboxTreeModel::create(*this);
}

AccountHolder::~AccountHolder()
{
}

void AccountHolder::add_account_with_name_and_mailboxes(String const& name, Vector<IMAP::ListItem> const& mailboxes)
{
    auto account = AccountNode::create(name);

    for (auto& mailbox : mailboxes) {
        if (mailbox.flags & (unsigned)IMAP::MailboxFlag::NoSelect)
            continue;

        auto mailbox_node = MailboxNode::create(account, mailbox);
        account->add_mailbox(move(mailbox_node));
    }

    m_accounts.append(move(account));
    rebuild_tree();
}

void AccountHolder::rebuild_tree()
{
    m_mailbox_tree_model->update();
}
