/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Undefine <cqundefine@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailWidget.h"
#include "InboxModel.h"
#include <AK/Base64.h>
#include <AK/GenericLexer.h>
#include <Applications/Mail/MailWindowGML.h>
#include <LibConfig/Client.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/PasswordInputDialog.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibIMAP/MessageHeaderEncoding.h>
#include <LibIMAP/QuotedPrintable.h>

MailWidget::MailWidget()
{
    load_from_gml(mail_window_gml).release_value_but_fixme_should_propagate_errors();

    m_mailbox_list = *find_descendant_of_type_named<GUI::TreeView>("mailbox_list");
    m_individual_mailbox_view = *find_descendant_of_type_named<GUI::TableView>("individual_mailbox_view");
    m_web_view = *find_descendant_of_type_named<WebView::OutOfProcessWebView>("web_view");
    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    m_mailbox_list->set_activates_on_selection(true);
    m_mailbox_list->on_activation = [this](auto& index) {
        selected_mailbox(index);
    };

    m_individual_mailbox_view->set_activates_on_selection(true);
    m_individual_mailbox_view->on_activation = [this](auto& index) {
        selected_email_to_load(m_mailbox_sorting_model->map_to_source(index));
    };

    m_web_view->on_link_click = [this](auto& url, auto&, unsigned) {
        if (!Desktop::Launcher::open(url)) {
            GUI::MessageBox::show(
                window(),
                ByteString::formatted("The link to '{}' could not be opened.", url),
                "Failed to open link"sv,
                GUI::MessageBox::Type::Error);
        }
    };

    m_web_view->on_link_middle_click = [this](auto& url, auto& target, unsigned modifiers) {
        m_web_view->on_link_click(url, target, modifiers);
    };

    m_web_view->on_link_hover = [this](auto& url) {
        if (url.is_valid())
            m_statusbar->set_text(String::from_byte_string(url.to_byte_string()).release_value_but_fixme_should_propagate_errors());
        else
            m_statusbar->set_text({});
    };

    m_link_context_menu = GUI::Menu::construct();
    auto link_default_action = GUI::Action::create("&Open in Browser", [this](auto&) {
        m_web_view->on_link_click(m_link_context_menu_url, "", 0);
    });
    m_link_context_menu->add_action(link_default_action);
    m_link_context_menu_default_action = link_default_action;
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(GUI::Action::create("&Copy URL", [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_link_context_menu_url.to_byte_string());
    }));

    m_web_view->on_link_context_menu_request = [this](auto& url, auto screen_position) {
        m_link_context_menu_url = url;
        m_link_context_menu->popup(screen_position, m_link_context_menu_default_action);
    };

    m_image_context_menu = GUI::Menu::construct();
    m_image_context_menu->add_action(GUI::Action::create("&Copy Image", [this](auto&) {
        if (m_image_context_menu_bitmap.is_valid())
            GUI::Clipboard::the().set_bitmap(*m_image_context_menu_bitmap.bitmap());
    }));
    m_image_context_menu->add_action(GUI::Action::create("Copy Image &URL", [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_image_context_menu_url.to_byte_string());
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(GUI::Action::create("&Open Image in Browser", [this](auto&) {
        m_web_view->on_link_click(m_image_context_menu_url, "", 0);
    }));

    m_web_view->on_image_context_menu_request = [this](auto& image_url, auto screen_position, Gfx::ShareableBitmap const& shareable_bitmap) {
        m_image_context_menu_url = image_url;
        m_image_context_menu_bitmap = shareable_bitmap;
        m_image_context_menu->popup(screen_position);
    };
}

MailboxNode* MailWidget::get_mailbox_by_name(ByteString const& username, ByteString const& mailbox_name)
{
    for (auto& account : m_account_holder->accounts()) {
        if (account->name() == username) {
            for (auto& mailbox : account->mailboxes()) {
                if (mailbox->select_name() == mailbox_name)
                    return mailbox;
            }
        }
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<void> MailWidget::refresh_unseen_count_for_mailbox(MailboxNode* mailbox)
{
    auto response = TRY(m_imap_client->status(mailbox->select_name(), { IMAP::StatusItemType::Unseen, IMAP::StatusItemType::Messages })->await());
    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to get mailbox status. The server says: '{}'", response.response_text());
        return {};
    }
    if (response.data().status_items().size() > 0)
        mailbox->set_unseen_count(response.data().status_items()[0].get(IMAP::StatusItemType::Unseen));
    return {};
}

ErrorOr<bool> MailWidget::connect_and_login()
{
    auto server = Config::read_string("Mail"sv, "Connection"sv, "Server"sv, {});

    if (server.is_empty()) {
        auto result = GUI::MessageBox::show(window(), "Mail has no servers configured. Do you want configure them now?"sv, "Error"sv, GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::YesNo);
        if (result == GUI::MessageBox::ExecResult::Yes)
            Desktop::Launcher::open(URL::create_with_file_scheme("/bin/MailSettings"));
        return false;
    }

    // Assume TLS by default, which is on port 993.
    auto port = Config::read_i32("Mail"sv, "Connection"sv, "Port"sv, 993);
    auto tls = Config::read_bool("Mail"sv, "Connection"sv, "TLS"sv, true);

    auto username = Config::read_string("Mail"sv, "User"sv, "Username"sv, {});
    if (username.is_empty()) {
        GUI::MessageBox::show_error(window(), "Mail has no username configured. Refer to the Mail(1) man page for more information."sv);
        return false;
    }

    // FIXME: Plaintext password storage, yikes!
    auto password = Config::read_string("Mail"sv, "User"sv, "Password"sv, {});
    while (password.is_empty()) {
        if (GUI::PasswordInputDialog::show(window(), password, "Login"sv, server, username) != GUI::Dialog::ExecResult::OK)
            return false;
    }

    m_statusbar->set_text(String::formatted("Connecting to {}:{}...", server, port).release_value_but_fixme_should_propagate_errors());
    auto maybe_imap_client = tls ? IMAP::Client::connect_tls(server, port) : IMAP::Client::connect_plaintext(server, port);
    if (maybe_imap_client.is_error()) {
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to connect to '{}:{}' over {}: {}", server, port, tls ? "TLS" : "Plaintext", maybe_imap_client.error()));
        return false;
    }
    m_imap_client = maybe_imap_client.release_value();

    TRY(m_imap_client->connection_promise()->await());

    m_statusbar->set_text(String::formatted("Connected. Logging in as {}...", username).release_value_but_fixme_should_propagate_errors());

    auto response = TRY(m_imap_client->login(username, password)->await());

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to login. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to login. The server says: '{}'", response.response_text()));
        m_statusbar->set_text("Failed to log in"_string);
        return false;
    }

    m_statusbar->set_text("Logged in. Loading mailboxes..."_string);
    response = TRY(m_imap_client->list(""sv, "*"sv, true)->await());

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve mailboxes. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to retrieve mailboxes. The server says: '{}'", response.response_text()));
        return false;
    }

    auto& list_items = response.data().list_items();

    m_account_holder = AccountHolder::create();
    m_account_holder->add_account_with_name_and_mailboxes(username, move(list_items));

    m_statusbar->set_text(MUST(String::formatted("Loaded {} mailboxes", list_items.size())));

    m_mailbox_list->set_model(m_account_holder->mailbox_tree_model());
    m_mailbox_list->expand_tree();

    auto& status_items = response.data().status_items();

    for (auto& status_item : status_items) {
        auto mailbox = get_mailbox_by_name(username, status_item.mailbox());
        mailbox->set_unseen_count(status_item.get(IMAP::StatusItemType::Unseen));
    }

    return true;
}

void MailWidget::on_window_close()
{
    if (!m_imap_client) {
        // User closed main window before a connection was established
        return;
    }
    auto response = move(MUST(m_imap_client->send_simple_command(IMAP::CommandType::Logout)->await()).get<IMAP::SolidResponse>());
    VERIFY(response.status() == IMAP::ResponseStatus::OK);

    m_imap_client->close();
}

IMAP::MultiPartBodyStructureData const* MailWidget::look_for_alternative_body_structure(IMAP::MultiPartBodyStructureData const& current_body_structure, Vector<u32>& position_stack) const
{
    if (current_body_structure.multipart_subtype.equals_ignoring_ascii_case("ALTERNATIVE"sv))
        return &current_body_structure;

    u32 structure_index = 1;

    for (auto& structure : current_body_structure.bodies) {
        if (structure->data().has<IMAP::BodyStructureData>()) {
            ++structure_index;
            continue;
        }

        position_stack.append(structure_index);
        auto* potential_alternative_structure = look_for_alternative_body_structure(structure->data().get<IMAP::MultiPartBodyStructureData>(), position_stack);

        if (potential_alternative_structure)
            return potential_alternative_structure;

        position_stack.take_last();
        ++structure_index;
    }

    return nullptr;
}

Vector<MailWidget::Alternative> MailWidget::get_alternatives(IMAP::MultiPartBodyStructureData const& multi_part_body_structure_data) const
{
    Vector<u32> position_stack;

    auto* alternative_body_structure = look_for_alternative_body_structure(multi_part_body_structure_data, position_stack);
    if (!alternative_body_structure)
        return {};

    Vector<MailWidget::Alternative> alternatives;
    alternatives.ensure_capacity(alternative_body_structure->bodies.size());

    int alternative_index = 1;
    for (auto& alternative_body : alternative_body_structure->bodies) {
        VERIFY(alternative_body->data().has<IMAP::BodyStructureData>());

        position_stack.append(alternative_index);

        MailWidget::Alternative alternative = {
            .body_structure = alternative_body->data().get<IMAP::BodyStructureData>(),
            .position = position_stack,
        };
        alternatives.append(alternative);

        position_stack.take_last();
        ++alternative_index;
    }

    return alternatives;
}

bool MailWidget::is_supported_alternative(Alternative const& alternative) const
{
    return alternative.body_structure.type.equals_ignoring_ascii_case("text"sv) && (alternative.body_structure.subtype.equals_ignoring_ascii_case("plain"sv) || alternative.body_structure.subtype.equals_ignoring_ascii_case("html"sv));
}

void MailWidget::selected_mailbox(GUI::ModelIndex const& index)
{
    if (!index.is_valid() || index == m_mailbox_index)
        return;
    m_mailbox_index = index;

    m_mailbox_model = InboxModel::create({});
    m_individual_mailbox_view->set_model(m_mailbox_model);

    auto& base_node = *static_cast<BaseNode*>(index.internal_data());

    if (is<AccountNode>(base_node)) {
        // FIXME: Do something when clicking on an account node.
        return;
    }

    auto& mailbox_node = verify_cast<MailboxNode>(base_node);
    auto& mailbox = mailbox_node.mailbox();

    m_selected_mailbox_node = mailbox_node;

    // FIXME: It would be better if we didn't allow the user to click on this mailbox node at all.
    if (mailbox.flags & (unsigned)IMAP::MailboxFlag::NoSelect)
        return;

    auto response = MUST(m_imap_client->select(mailbox.name)->await());

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to select mailbox. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to select mailbox. The server says: '{}'", response.response_text()));
        return;
    }

    if (response.data().exists() == 0) {
        // No mail in this mailbox, return.
        m_statusbar->set_text(String::formatted("[{}]: 0 messages", mailbox.name).release_value_but_fixme_should_propagate_errors());
        return;
    }

    m_statusbar->set_text(String::formatted("[{}]: Fetching {} messages...", mailbox.name, response.data().exists()).release_value_but_fixme_should_propagate_errors());
    auto fetch_command = IMAP::FetchCommand {
        // Mail will always be numbered from 1 up to the number of mail items that exist, which is specified in the select response with "EXISTS".
        .sequence_set = { { 1, (int)response.data().exists() } },
        .data_items = {
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::Envelope,
            },
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::InternalDate,
            },
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::Flags,
            },
        },
    };

    auto fetch_response = MUST(m_imap_client->fetch(fetch_command, false)->await());
    if (fetch_response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve subject/from for e-mails. The server says: '{}'", response.response_text());
        m_statusbar->set_text(String::formatted("[{}]: Failed to fetch messages :^(", mailbox.name).release_value_but_fixme_should_propagate_errors());
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to retrieve e-mails. The server says: '{}'", response.response_text()));
        return;
    }

    Vector<InboxEntry> active_inbox_entries;

    int i = 0;
    for (auto& fetch_data : fetch_response.data().fetch_data()) {
        auto sequence_number = fetch_data.get<unsigned>();
        auto& response_data = fetch_data.get<IMAP::FetchResponseData>();
        auto const& envelope = response_data.envelope();
        auto const& internal_date = response_data.internal_date();

        auto seen = !response_data.flags().find_if([](StringView value) { return value.equals_ignoring_ascii_case("\\Seen"sv); }).is_end();

        ByteString date = internal_date.to_byte_string();
        ByteString subject = envelope.subject.is_empty() ? "(No subject)" : envelope.subject;
        if (subject.contains("=?"sv) && subject.contains("?="sv)) {
            subject = MUST(IMAP::decode_rfc2047_encoded_words(subject)).span();
        }

        StringBuilder sender_builder;
        if (!envelope.from.is_empty()) {
            bool first { true };
            for (auto const& address : envelope.from) {
                if (!first)
                    sender_builder.append(", "sv);

                if (!address.name.is_empty()) {
                    if (address.name.contains("=?"sv) && address.name.contains("?="sv))
                        sender_builder.append(MUST(IMAP::decode_rfc2047_encoded_words(address.name)));
                    else
                        sender_builder.append(address.name);

                    sender_builder.append(" <"sv);
                    sender_builder.append(address.mailbox);
                    sender_builder.append('@');
                    sender_builder.append(address.host);
                    sender_builder.append('>');
                } else {
                    sender_builder.append(address.mailbox);
                    sender_builder.append('@');
                    sender_builder.append(address.host);
                }
            }
        }
        ByteString from = sender_builder.to_byte_string();

        InboxEntry inbox_entry { sequence_number, date, from, subject, seen ? MailStatus::Seen : MailStatus::Unseen };
        m_statusbar->set_text(String::formatted("[{}]: Loading entry {}", mailbox.name, ++i).release_value_but_fixme_should_propagate_errors());

        active_inbox_entries.append(inbox_entry);
    }

    (void)refresh_unseen_count_for_mailbox(m_selected_mailbox_node);

    m_statusbar->set_text(String::formatted("[{}]: Loaded {} entries", mailbox.name, i).release_value_but_fixme_should_propagate_errors());
    m_mailbox_model = InboxModel::create(move(active_inbox_entries));
    m_mailbox_sorting_model = MUST(GUI::SortingProxyModel::create(*m_mailbox_model));
    m_mailbox_sorting_model->set_sort_role(GUI::ModelRole::Display);
    m_individual_mailbox_view->set_model(m_mailbox_sorting_model);
    m_individual_mailbox_view->set_key_column_and_sort_order(InboxModel::Date, GUI::SortOrder::Descending);
}

void MailWidget::selected_email_to_load(GUI::ModelIndex const& index)
{
    if (!index.is_valid())
        return;

    int id_of_email_to_load = index.data(static_cast<GUI::ModelRole>(InboxModelCustomRole::Sequence)).as_u32();

    m_statusbar->set_text("Fetching message..."_string);

    auto fetch_command = IMAP::FetchCommand {
        .sequence_set = { { id_of_email_to_load, id_of_email_to_load } },
        .data_items = {
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::BodyStructure,
            },
        },
    };

    auto fetch_response = MUST(m_imap_client->fetch(fetch_command, false)->await());

    if (fetch_response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve the body structure of the selected e-mail. The server says: '{}'", fetch_response.response_text());
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to retrieve the selected e-mail. The server says: '{}'", fetch_response.response_text()));
        return;
    }

    Vector<u32> selected_alternative_position;
    ByteString selected_alternative_encoding;

    auto& response_data = fetch_response.data().fetch_data().last().get<IMAP::FetchResponseData>();

    response_data.body_structure().data().visit(
        [&](IMAP::BodyStructureData const& data) {
            // The message will be in the first position.
            selected_alternative_position.append(1);
            selected_alternative_encoding = data.encoding;
        },
        [&](IMAP::MultiPartBodyStructureData const& data) {
            auto alternatives = get_alternatives(data);
            if (alternatives.is_empty()) {
                dbgln("No alternatives. The server said: '{}'", fetch_response.response_text());
                GUI::MessageBox::show_error(window(), "The server sent no message to display."sv);
                return;
            }

            // We can choose whichever alternative we want. In general, we should choose the last alternative that know we can display.
            // RFC 2046 Section 5.1.4 https://datatracker.ietf.org/doc/html/rfc2046#section-5.1.4
            auto chosen_alternative = alternatives.last_matching([this](auto& alternative) {
                return is_supported_alternative(alternative);
            });

            if (!chosen_alternative.has_value()) {
                GUI::MessageBox::show(window(), "Displaying this type of e-mail is currently unsupported."sv, "Unsupported"sv, GUI::MessageBox::Type::Information);
                return;
            }

            selected_alternative_position = chosen_alternative->position;
            selected_alternative_encoding = chosen_alternative->body_structure.encoding;
        });

    if (selected_alternative_position.is_empty()) {
        // An error occurred above, return.
        return;
    }

    fetch_command = IMAP::FetchCommand {
        .sequence_set { { id_of_email_to_load, id_of_email_to_load } },
        .data_items = {
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::BodySection,
                .section = IMAP::FetchCommand::DataItem::Section {
                    .type = IMAP::FetchCommand::DataItem::SectionType::Parts,
                    .parts = selected_alternative_position,
                },
                .partial_fetch = false,
            },
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::Flags,
            },
        },
    };

    fetch_response = MUST(m_imap_client->fetch(fetch_command, false)->await());

    if (fetch_response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve the body of the selected e-mail. The server says: '{}'", fetch_response.response_text());
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to retrieve the selected e-mail. The server says: '{}'", fetch_response.response_text()));
        return;
    }

    m_statusbar->set_text("Parsing message..."_string);

    auto& fetch_data = fetch_response.data().fetch_data();

    if (fetch_data.is_empty()) {
        dbgln("The server sent no fetch data.");
        GUI::MessageBox::show_error(window(), "The server sent no data."sv);
        return;
    }

    auto& fetch_response_data = fetch_data.last().get<IMAP::FetchResponseData>();

    auto seen = !fetch_response_data.flags().find_if([](StringView value) { return value.equals_ignoring_ascii_case("\\Seen"sv); }).is_end();
    if (m_mailbox_model->mail_status(index.row()) != (seen ? MailStatus::Seen : MailStatus::Unseen)) {
        seen ? m_selected_mailbox_node->decrement_unseen_count() : m_selected_mailbox_node->increment_unseen_count();
        m_mailbox_list->repaint();
    }
    m_mailbox_model->set_mail_status(index.row(), seen ? MailStatus::Seen : MailStatus::Unseen);

    if (!fetch_response_data.contains_response_type(IMAP::FetchResponseType::Body)) {
        GUI::MessageBox::show_error(window(), "The server sent no body."sv);
        return;
    }

    auto& body_data = fetch_response_data.body_data();
    auto body_text_part_iterator = body_data.find_if([](Tuple<IMAP::FetchCommand::DataItem, ByteString>& data) {
        auto const data_item = data.get<0>();
        return data_item.section.has_value() && data_item.section->type == IMAP::FetchCommand::DataItem::SectionType::Parts;
    });
    VERIFY(body_text_part_iterator != body_data.end());

    auto& encoded_data = body_text_part_iterator->get<1>();

    ByteString decoded_data;

    // FIXME: String uses char internally, so 8bit shouldn't be stored in it.
    //        However, it works for now.
    if (selected_alternative_encoding.equals_ignoring_ascii_case("7bit"sv) || selected_alternative_encoding.equals_ignoring_ascii_case("8bit"sv)) {
        decoded_data = encoded_data;
    } else if (selected_alternative_encoding.equals_ignoring_ascii_case("base64"sv)) {
        encoded_data = encoded_data.replace("\r"sv, ""sv).replace("\n"sv, ""sv);
        auto decoded_base64 = decode_base64(encoded_data);
        if (!decoded_base64.is_error())
            decoded_data = decoded_base64.release_value().span();
    } else if (selected_alternative_encoding.equals_ignoring_ascii_case("quoted-printable"sv)) {
        decoded_data = IMAP::decode_quoted_printable(encoded_data).release_value_but_fixme_should_propagate_errors().span();
    } else {
        dbgln("Mail: Unimplemented decoder for encoding: {}", selected_alternative_encoding);
        GUI::MessageBox::show(window(), ByteString::formatted("The e-mail encoding '{}' is currently unsupported.", selected_alternative_encoding), "Unsupported"sv, GUI::MessageBox::Type::Information);
        return;
    }

    m_statusbar->set_text("Message loaded."_string);

    // FIXME: I'm not sure what the URL should be. Just use the default URL "about:blank".
    // FIXME: It would be nice if we could pass over the charset.
    // FIXME: Add ability to cancel the load when we switch to another email. Feels very sluggish on heavy emails otherwise
    m_web_view->load_html(decoded_data);
}
