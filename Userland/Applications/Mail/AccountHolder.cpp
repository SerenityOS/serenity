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

    // Mailboxes with these names will be given priority in the display order.
    // Some of these mailboxes are defined per RFC 6154 as Special-Use Mailboxes,
    // however the ordering is primarily an arbitrary decision intended to match
    // the behavior of other email clients such as Thunderbird.
    append_mailbox_default_display_setting("INBOX", "Inbox");
    append_mailbox_default_display_setting("Drafts", "", "/res/icons/16x16/new.png");
    append_mailbox_default_display_setting("Sent", "", "/res/icons/16x16/sent.png");
    append_mailbox_default_display_setting("Archive", "", "/res/icons/16x16/filetype-archive.png");
    append_mailbox_default_display_setting("Junk", "", "/res/icons/16x16/spam.png");
    append_mailbox_default_display_setting("Spam", "", "/res/icons/16x16/spam.png");
    append_mailbox_default_display_setting("Trash", "", "/res/icons/16x16/trash-can.png");
}

void AccountHolder::append_mailbox_default_display_setting(ByteString select_name, ByteString display_name, ByteString path_to_display_icon)
{
    m_mailbox_default_display_settings.append(HashMap<ByteString, ByteString>());
    auto index = m_mailbox_default_display_settings.size() - 1;
    m_mailbox_default_display_settings[index].set(select_name, display_name == "" ? select_name : display_name);
    if (path_to_display_icon != "")
        m_mailbox_default_display_settings[index].set("path_to_display_icon", path_to_display_icon);
}

void AccountHolder::add_account_with_name_and_mailboxes(ByteString name, Vector<IMAP::ListItem> mailboxes)
{
    auto account = AccountNode::create(move(name));

    // This holds all of the ancestors of the current leaf folder.
    Vector<NonnullRefPtr<MailboxNode>> folder_stack;

    // Sort default mailboxes by arbitrary display name order.
    Vector<IMAP::ListItem> arbitrarily_ordered_mailboxes;
    for (auto& default_display_name : m_mailbox_default_display_settings) {
        for (size_t i = 0; i < mailboxes.size(); i++) {
            if (default_display_name.get(mailboxes[i].name).has_value()) {
                arbitrarily_ordered_mailboxes.append(mailboxes[i]);
                mailboxes.remove(i);
            }
        }
    }
    arbitrarily_ordered_mailboxes.extend(mailboxes);
    mailboxes.clear();

    for (auto& mailbox : arbitrarily_ordered_mailboxes) {
        // mailbox.name is converted to StringView to get access to split by string.
        auto subfolders = StringView(mailbox.name).split_view(mailbox.reference);

        // Use the last part of the path as the display name.
        // For example: "[Mail]/Subfolder" will be displayed as "Subfolder"
        auto display_name = subfolders.last();
        ByteString path_to_display_icon = "";
        for (auto& default_display_setting : m_mailbox_default_display_settings) {
            if (default_display_setting.get(mailbox.name).has_value()) {
                display_name = default_display_setting.get(mailbox.name).value();
                if (default_display_setting.get("path_to_display_icon").has_value())
                    path_to_display_icon = default_display_setting.get("path_to_display_icon").value();
            }
        }
        auto mailbox_node = MailboxNode::create(account, mailbox, display_name);

        if (path_to_display_icon != "")
            mailbox_node->set_display_icon(path_to_display_icon);

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

void MailboxNode::set_display_icon(ByteString path_to_display_icon)
{
    if (path_to_display_icon != "")
        m_display_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file(path_to_display_icon).release_value_but_fixme_should_propagate_errors());
}
