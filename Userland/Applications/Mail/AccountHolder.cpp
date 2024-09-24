/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AccountHolder.h"

AccountHolder::AccountHolder()
{
    m_mailbox_tree_model = MailboxTreeModel::create(*this);
}

void AccountHolder::add_account_with_name_and_mailboxes(ByteString name, Vector<IMAP::ListItem> const& mailboxes)
{
    auto account = AccountNode::create(move(name));

    // This holds all of the ancestors of the current leaf folder.
    Vector<NonnullRefPtr<MailboxNode>> folder_stack;

    for (auto& mailbox : mailboxes) {
        // mailbox.name is converted to StringView to get access to split by string.
        auto subfolders = StringView(mailbox.name).split_view(mailbox.reference);

        // Use the last part of the path as the display name.
        // For example: "[Mail]/Subfolder" will be displayed as "Subfolder"
        auto mailbox_node = MailboxNode::create(account, mailbox, subfolders.last());

        if (subfolders.size() > 1) {
            VERIFY(!folder_stack.is_empty());

            // This gets the parent folder of the leaf folder that we just created above.
            // For example, with "[Mail]/Subfolder/Leaf", "subfolders" will have three items:
            //   - "[Mail]" at index 0.
            //   - "Subfolder" at index 1. This is the parent folder of the leaf folder.
            //   - "Leaf" at index 2. This is the leaf folder.
            // Notice that the parent folder is always two below the size of "subfolders".
            // This assumes that there was two listings before this, in this exact order:
            // 1. "[Mail]"
            // 2. "[Mail]/Subfolder"
            auto& parent_folder = folder_stack.at(subfolders.size() - 2);

            // Only keep the ancestors of the current leaf folder.
            folder_stack.shrink(subfolders.size() - 1);

            parent_folder->add_child(mailbox_node);
            VERIFY(!mailbox_node->has_parent());
            mailbox_node->set_parent(parent_folder);

            // FIXME: This assumes that the server has the "CHILDREN" capability.
            if (mailbox.flags & (unsigned)IMAP::MailboxFlag::HasChildren)
                folder_stack.append(mailbox_node);
        } else {
            // FIXME: This assumes that the server has the "CHILDREN" capability.
            if (mailbox.flags & (unsigned)IMAP::MailboxFlag::HasChildren) {
                if (!folder_stack.is_empty() && folder_stack.first()->select_name() != mailbox.name) {
                    // This is a new root folder, clear the stack as there are no ancestors of the current leaf folder at this point.
                    folder_stack.clear();
                }

                folder_stack.append(mailbox_node);
            }

            account->add_mailbox(move(mailbox_node));
        }
    }

    m_accounts.append(move(account));
    rebuild_tree();
}

void AccountHolder::rebuild_tree()
{
    m_mailbox_tree_model->invalidate();
}

void MailboxNode::update_display_name_with_unseen_count()
{
    m_display_name_with_unseen_count = ByteString::formatted("{} ({})", m_display_name, m_unseen_count);
}

void MailboxNode::decrement_unseen_count()
{
    if (m_unseen_count)
        set_unseen_count(m_unseen_count - 1);
}

void MailboxNode::increment_unseen_count()
{
    set_unseen_count(m_unseen_count + 1);
}

void MailboxNode::set_unseen_count(unsigned unseen_count)
{
    m_unseen_count = unseen_count;
    update_display_name_with_unseen_count();
}
