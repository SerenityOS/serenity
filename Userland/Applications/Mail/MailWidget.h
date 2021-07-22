/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AccountHolder.h"
#include "InboxModel.h"
#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/Widget.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibIMAP/Client.h>
#include <LibWeb/OutOfProcessWebView.h>

class MailWidget final : public GUI::Widget {
    C_OBJECT(MailWidget)
public:
    virtual ~MailWidget() override;

    bool connect_and_login();

    void on_window_close();

private:
    MailWidget();

    void selected_mailbox();
    void selected_email_to_load();

    struct Alternative {
        IMAP::BodyStructureData const& body_structure;
        Vector<u32> position;
    };

    IMAP::MultiPartBodyStructureData const* look_for_alternative_body_structure(IMAP::MultiPartBodyStructureData const& current_body_structure, Vector<u32>& position_stack) const;
    Vector<Alternative> get_alternatives(IMAP::MultiPartBodyStructureData const&) const;
    bool is_supported_alternative(Alternative const&) const;

    OwnPtr<IMAP::Client> m_imap_client;

    RefPtr<GUI::TreeView> m_mailbox_list;
    RefPtr<GUI::TableView> m_individual_mailbox_view;
    RefPtr<Web::OutOfProcessWebView> m_web_view;
    RefPtr<GUI::Statusbar> m_statusbar;

    RefPtr<GUI::Menu> m_link_context_menu;
    RefPtr<GUI::Action> m_link_context_menu_default_action;
    URL m_link_context_menu_url;

    RefPtr<GUI::Menu> m_image_context_menu;
    Gfx::ShareableBitmap m_image_context_menu_bitmap;
    URL m_image_context_menu_url;

    OwnPtr<AccountHolder> m_account_holder;
};
