/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MailboxTreeModel.h"
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibIMAP/Objects.h>

class BaseNode : public RefCounted<BaseNode> {
public:
    virtual ~BaseNode()
    {
    }
};

class MailboxNode;

class AccountNode final : public BaseNode {
public:
    static NonnullRefPtr<AccountNode> create(String const& name)
    {
        return adopt_ref(*new AccountNode(name));
    }

    virtual ~AccountNode() override = default;

    void add_mailbox(NonnullRefPtr<MailboxNode> mailbox)
    {
        m_mailboxes.append(move(mailbox));
    }

    NonnullRefPtrVector<MailboxNode> const& mailboxes() const { return m_mailboxes; }
    String const& name() const { return m_name; }

private:
    explicit AccountNode(String const& name)
        : m_name(name)
    {
    }

    String m_name;
    NonnullRefPtrVector<MailboxNode> m_mailboxes;
};

class MailboxNode final : public BaseNode {
public:
    static NonnullRefPtr<MailboxNode> create(AccountNode const& associated_account, IMAP::ListItem const& mailbox)
    {
        return adopt_ref(*new MailboxNode(associated_account, mailbox));
    }

    virtual ~MailboxNode() override = default;

    AccountNode const& associated_account() const { return m_associated_account; }
    String const& name() const { return m_mailbox.name; }
    IMAP::ListItem const& mailbox() const { return m_mailbox; }

private:
    MailboxNode(AccountNode const& associated_account, IMAP::ListItem const& mailbox)
        : m_associated_account(associated_account)
        , m_mailbox(mailbox)
    {
    }

    AccountNode const& m_associated_account;
    IMAP::ListItem m_mailbox;
};

class AccountHolder {
public:
    ~AccountHolder();

    static NonnullOwnPtr<AccountHolder> create()
    {
        return adopt_own(*new AccountHolder());
    }

    void add_account_with_name_and_mailboxes(String const&, Vector<IMAP::ListItem> const&);

    NonnullRefPtrVector<AccountNode> const& accounts() const { return m_accounts; }
    MailboxTreeModel& mailbox_tree_model() { return *m_mailbox_tree_model; }

private:
    AccountHolder();

    void rebuild_tree();

    NonnullRefPtrVector<AccountNode> m_accounts;
    RefPtr<MailboxTreeModel> m_mailbox_tree_model;
};
