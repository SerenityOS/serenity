/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MailboxTreeModel.h"
#include <AK/DeprecatedString.h>
#include <AK/RefCounted.h>
#include <LibIMAP/Objects.h>

class BaseNode : public RefCounted<BaseNode> {
public:
    virtual ~BaseNode() = default;
};

class MailboxNode;

class AccountNode final : public BaseNode {
public:
    static NonnullRefPtr<AccountNode> create(DeprecatedString name)
    {
        return adopt_ref(*new AccountNode(move(name)));
    }

    virtual ~AccountNode() override = default;

    void add_mailbox(NonnullRefPtr<MailboxNode> mailbox)
    {
        m_mailboxes.append(move(mailbox));
    }

    Vector<NonnullRefPtr<MailboxNode>> const& mailboxes() const { return m_mailboxes; }
    DeprecatedString const& name() const { return m_name; }

private:
    explicit AccountNode(DeprecatedString name)
        : m_name(move(name))
    {
    }

    DeprecatedString m_name;
    Vector<NonnullRefPtr<MailboxNode>> m_mailboxes;
};

class MailboxNode final : public BaseNode {
public:
    static NonnullRefPtr<MailboxNode> create(AccountNode const& associated_account, IMAP::ListItem const& mailbox, DeprecatedString display_name)
    {
        return adopt_ref(*new MailboxNode(associated_account, mailbox, move(display_name)));
    }

    virtual ~MailboxNode() override = default;

    AccountNode const& associated_account() const { return m_associated_account; }
    DeprecatedString const& select_name() const { return m_mailbox.name; }
    DeprecatedString const& display_name() const { return m_display_name; }
    IMAP::ListItem const& mailbox() const { return m_mailbox; }

    bool has_parent() const { return m_parent; }
    RefPtr<MailboxNode> parent() const { return m_parent; }
    void set_parent(NonnullRefPtr<MailboxNode> parent) { m_parent = parent; }

    bool has_children() const { return !m_children.is_empty(); }
    Vector<NonnullRefPtr<MailboxNode>> const& children() const { return m_children; }
    void add_child(NonnullRefPtr<MailboxNode> child) { m_children.append(child); }

private:
    MailboxNode(AccountNode const& associated_account, IMAP::ListItem const& mailbox, DeprecatedString display_name)
        : m_associated_account(associated_account)
        , m_mailbox(mailbox)
        , m_display_name(move(display_name))
    {
    }

    AccountNode const& m_associated_account;
    IMAP::ListItem m_mailbox;
    DeprecatedString m_display_name;

    Vector<NonnullRefPtr<MailboxNode>> m_children;
    RefPtr<MailboxNode> m_parent;
};

class AccountHolder {
public:
    ~AccountHolder() = default;

    static NonnullOwnPtr<AccountHolder> create()
    {
        return adopt_own(*new AccountHolder());
    }

    void add_account_with_name_and_mailboxes(DeprecatedString, Vector<IMAP::ListItem> const&);

    Vector<NonnullRefPtr<AccountNode>> const& accounts() const { return m_accounts; }
    MailboxTreeModel& mailbox_tree_model() { return *m_mailbox_tree_model; }

private:
    AccountHolder();

    void rebuild_tree();

    Vector<NonnullRefPtr<AccountNode>> m_accounts;
    RefPtr<MailboxTreeModel> m_mailbox_tree_model;
};
