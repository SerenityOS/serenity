/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MailboxTreeModel.h"
#include <AK/ByteString.h>
#include <AK/RefCounted.h>
#include <LibIMAP/Objects.h>

class BaseNode : public RefCounted<BaseNode> {
public:
    virtual ~BaseNode() = default;
};

class MailboxNode;

class AccountNode final : public BaseNode {
public:
    static NonnullRefPtr<AccountNode> create(ByteString name)
    {
        return adopt_ref(*new AccountNode(move(name)));
    }

    virtual ~AccountNode() override = default;

    void add_mailbox(NonnullRefPtr<MailboxNode> mailbox)
    {
        m_mailboxes.append(move(mailbox));
    }

    Vector<NonnullRefPtr<MailboxNode>> const& mailboxes() const { return m_mailboxes; }
    ByteString const& name() const { return m_name; }

private:
    explicit AccountNode(ByteString name)
        : m_name(move(name))
    {
    }

    ByteString m_name;
    Vector<NonnullRefPtr<MailboxNode>> m_mailboxes;
};

class MailboxNode final : public BaseNode {
public:
    static NonnullRefPtr<MailboxNode> create(AccountNode const& associated_account, IMAP::ListItem const& mailbox, ByteString display_name)
    {
        return adopt_ref(*new MailboxNode(associated_account, mailbox, move(display_name)));
    }

    virtual ~MailboxNode() override = default;

    AccountNode const& associated_account() const { return m_associated_account; }
    ByteString const& select_name() const { return m_mailbox.name; }
    ByteString const& display_name() const { return m_display_name; }
    ByteString const& display_name_with_unseen_count() const { return m_display_name_with_unseen_count; }
    IMAP::ListItem const& mailbox() const { return m_mailbox; }

    bool has_parent() const { return m_parent; }
    RefPtr<MailboxNode> parent() const { return m_parent; }
    void set_parent(NonnullRefPtr<MailboxNode> parent) { m_parent = parent; }

    bool has_children() const { return !m_children.is_empty(); }
    Vector<NonnullRefPtr<MailboxNode>> const& children() const { return m_children; }
    void add_child(NonnullRefPtr<MailboxNode> child) { m_children.append(child); }

    unsigned unseen_count() const { return m_unseen_count; }
    void decrement_unseen_count();
    void increment_unseen_count();
    void set_unseen_count(unsigned unseen_count);

private:
    MailboxNode(AccountNode const& associated_account, IMAP::ListItem const& mailbox, ByteString display_name)
        : m_associated_account(associated_account)
        , m_mailbox(mailbox)
        , m_display_name(move(display_name))
    {
    }

    void update_display_name_with_unseen_count();

    AccountNode const& m_associated_account;
    IMAP::ListItem m_mailbox;
    ByteString m_display_name;
    ByteString m_display_name_with_unseen_count;
    unsigned m_unseen_count;

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

    void add_account_with_name_and_mailboxes(ByteString, Vector<IMAP::ListItem> const&);

    Vector<NonnullRefPtr<AccountNode>> const& accounts() const { return m_accounts; }
    MailboxTreeModel& mailbox_tree_model() { return *m_mailbox_tree_model; }

private:
    AccountHolder();

    void rebuild_tree();

    Vector<NonnullRefPtr<AccountNode>> m_accounts;
    RefPtr<MailboxTreeModel> m_mailbox_tree_model;
};
