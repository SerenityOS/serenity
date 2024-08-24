/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AccountHolder.h"
#include "InboxModel.h"
#include <LibGUI/Widget.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibIMAP/Client.h>
#include <LibWebView/OutOfProcessWebView.h>

class MailWidget final : public GUI::Widget {
    C_OBJECT(MailWidget)
public:
    virtual ~MailWidget() override = default;

    ErrorOr<bool> connect_and_login();
    ErrorOr<void> refresh_unseen_count_for_mailbox(MailboxNode* mailbox);

    void on_window_close();

private:
    MailWidget();

    MailboxNode* get_mailbox_by_name(ByteString const& username, ByteString const& mailbox_name);
    void selected_mailbox(GUI::ModelIndex const&);
    void selected_email_to_load(GUI::ModelIndex const&);

    struct Alternative {
        IMAP::BodyStructureData const& body_structure;
        Vector<u32> position;
    };

    IMAP::MultiPartBodyStructureData const* look_for_alternative_body_structure(IMAP::MultiPartBodyStructureData const& current_body_structure, Vector<u32>& position_stack) const;
    Vector<Alternative> get_alternatives(IMAP::MultiPartBodyStructureData const&) const;
    bool is_supported_alternative(Alternative const&) const;

    OwnPtr<IMAP::Client> m_imap_client;

    GUI::ModelIndex m_mailbox_index;
    RefPtr<GUI::TreeView> m_mailbox_list;
    RefPtr<InboxModel> m_mailbox_model;
    RefPtr<GUI::SortingProxyModel> m_mailbox_sorting_model;
    RefPtr<GUI::TableView> m_individual_mailbox_view;
    RefPtr<WebView::OutOfProcessWebView> m_web_view;
    RefPtr<MailboxNode> m_selected_mailbox_node;
    RefPtr<GUI::Statusbar> m_statusbar;

    RefPtr<GUI::Menu> m_link_context_menu;
    RefPtr<GUI::Action> m_link_context_menu_default_action;
    URL::URL m_link_context_menu_url;

    RefPtr<GUI::Menu> m_image_context_menu;
    Gfx::ShareableBitmap m_image_context_menu_bitmap;
    URL::URL m_image_context_menu_url;

    OwnPtr<AccountHolder> m_account_holder;
};
