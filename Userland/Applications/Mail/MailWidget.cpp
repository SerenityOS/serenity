/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailWidget.h"
#include <AK/Base64.h>
#include <Applications/Mail/MailWindowGML.h>
#include <LibCore/ConfigFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibIMAP/QuotedPrintable.h>

MailWidget::MailWidget()
{
    load_from_gml(mail_window_gml);

    m_mailbox_list = *find_descendant_of_type_named<GUI::TreeView>("mailbox_list");
    m_individual_mailbox_view = *find_descendant_of_type_named<GUI::TableView>("individual_mailbox_view");
    m_web_view = *find_descendant_of_type_named<Web::OutOfProcessWebView>("web_view");
    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    m_mailbox_list->on_selection_change = [this] {
        selected_mailbox();
    };

    m_web_view->on_link_click = [this](auto& url, auto&, unsigned) {
        if (!Desktop::Launcher::open(url)) {
            GUI::MessageBox::show(
                window(),
                String::formatted("The link to '{}' could not be opened.", url),
                "Failed to open link",
                GUI::MessageBox::Type::Error);
        }
    };

    m_web_view->on_link_middle_click = [this](auto& url, auto& target, unsigned modifiers) {
        m_web_view->on_link_click(url, target, modifiers);
    };

    m_web_view->on_link_hover = [this](auto& url) {
        if (url.is_valid())
            m_statusbar->set_text(url.to_string());
        else
            m_statusbar->set_text("");
    };

    m_link_context_menu = GUI::Menu::construct();
    auto link_default_action = GUI::Action::create("&Open in Browser", [this](auto&) {
        m_web_view->on_link_click(m_link_context_menu_url, "", 0);
    });
    m_link_context_menu->add_action(link_default_action);
    m_link_context_menu_default_action = link_default_action;
    m_link_context_menu->add_separator();
    m_link_context_menu->add_action(GUI::Action::create("&Copy URL", [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_link_context_menu_url.to_string());
    }));

    m_web_view->on_link_context_menu_request = [this](auto& url, auto& screen_position) {
        m_link_context_menu_url = url;
        m_link_context_menu->popup(screen_position, m_link_context_menu_default_action);
    };

    m_image_context_menu = GUI::Menu::construct();
    m_image_context_menu->add_action(GUI::Action::create("&Copy Image", [this](auto&) {
        if (m_image_context_menu_bitmap.is_valid())
            GUI::Clipboard::the().set_bitmap(*m_image_context_menu_bitmap.bitmap());
    }));
    m_image_context_menu->add_action(GUI::Action::create("Copy Image &URL", [this](auto&) {
        GUI::Clipboard::the().set_plain_text(m_image_context_menu_url.to_string());
    }));
    m_image_context_menu->add_separator();
    m_image_context_menu->add_action(GUI::Action::create("&Open Image in Browser", [this](auto&) {
        m_web_view->on_link_click(m_image_context_menu_url, "", 0);
    }));

    m_web_view->on_image_context_menu_request = [this](auto& image_url, auto& screen_position, Gfx::ShareableBitmap const& shareable_bitmap) {
        m_image_context_menu_url = image_url;
        m_image_context_menu_bitmap = shareable_bitmap;
        m_image_context_menu->popup(screen_position);
    };
}

MailWidget::~MailWidget()
{
}

bool MailWidget::connect_and_login()
{
    auto config = Core::ConfigFile::get_for_app("Mail");

    auto server = config->read_entry("Connection", "Server", {});

    if (server.is_empty()) {
        GUI::MessageBox::show_error(window(), "The Mail configuration is missing the server to connect to.");
        return false;
    }

    // Assume TLS by default, which is on port 993.
    auto port = config->read_num_entry("Connection", "Port", 993);
    auto tls = config->read_bool_entry("Connection", "TLS", true);

    auto username = config->read_entry("User", "Username", {});
    if (username.is_empty()) {
        GUI::MessageBox::show_error(window(), "The Mail configuration is missing the username to login with.");
        return false;
    }

    auto password = config->read_entry("User", "Password", {});
    if (password.is_empty()) {
        GUI::MessageBox::show_error(window(), "The Mail configuration is missing the password to login with.");
        return false;
    }

    m_imap_client = adopt_own(*new IMAP::Client(server, port, tls));
    auto connection_promise = m_imap_client->connect();
    if (!connection_promise.has_value()) {
        GUI::MessageBox::show_error(window(), String::formatted("Failed to connect to '{}:{}' over {}.", server, port, tls ? "TLS" : "Plaintext"));
        return false;
    }
    connection_promise.value()->await();

    auto response = m_imap_client->login(username, password)->await().release_value();

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to login. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), String::formatted("Failed to login. The server says: '{}'", response.response_text()));
        return false;
    }

    response = m_imap_client->list("", "*")->await().release_value();

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve mailboxes. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), String::formatted("Failed to retrieve mailboxes. The server says: '{}'", response.response_text()));
        return false;
    }

    auto& list_items = response.data().list_items();

    m_account_holder = AccountHolder::create();
    m_account_holder->add_account_with_name_and_mailboxes(username, move(list_items));

    m_mailbox_list->set_model(m_account_holder->mailbox_tree_model());
    m_mailbox_list->expand_tree();

    return true;
}

void MailWidget::on_window_close()
{
    auto response = move(m_imap_client->send_simple_command(IMAP::CommandType::Logout)->await().release_value().get<IMAP::SolidResponse>());
    VERIFY(response.status() == IMAP::ResponseStatus::OK);

    m_imap_client->close();
}

IMAP::MultiPartBodyStructureData const* MailWidget::look_for_alternative_body_structure(IMAP::MultiPartBodyStructureData const& current_body_structure, Vector<int>& position_stack) const
{
    if (current_body_structure.media_type.equals_ignoring_case("ALTERNATIVE"))
        return &current_body_structure;

    int structure_index = 1;

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
    Vector<int> position_stack;

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
    auto lowercase_type = alternative.body_structure.type.to_lowercase();
    auto lowercase_subtype = alternative.body_structure.subtype.to_lowercase();
    return lowercase_type == "text" && lowercase_subtype.is_one_of("plain", "html");
}

void MailWidget::selected_mailbox()
{
    auto const& index = m_mailbox_list->selection().first();

    if (!index.is_valid())
        return;

    auto& base_node = *static_cast<BaseNode*>(index.internal_data());

    if (is<AccountNode>(base_node)) {
        // FIXME: Do something when clicking on an account node.
        return;
    }

    auto& mailbox_node = verify_cast<MailboxNode>(base_node);
    auto& mailbox = mailbox_node.mailbox();

    VERIFY(!(mailbox.flags & (unsigned)IMAP::MailboxFlag::NoSelect));

    auto response = m_imap_client->select(mailbox.name)->await().release_value();

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to select mailbox. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), String::formatted("Failed to select mailbox. The server says: '{}'", response.response_text()));
        return;
    }

    auto fetch_command = IMAP::FetchCommand {
        // Mail will always be numbered from 1 up to the number of mail items that exist, which is specified in the select response with "EXISTS".
        .sequence_set = { { 1, (int)response.data().exists() } },
        .data_items = {
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::BodySection,
                .section = IMAP::FetchCommand::DataItem::Section {
                    .type = IMAP::FetchCommand::DataItem::SectionType::HeaderFields,
                    .headers = { { "Subject", "From" } },
                },
            },
        },
    };

    auto fetch_response = m_imap_client->fetch(fetch_command, false)->await().release_value();

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve subject/from for e-mails. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), String::formatted("Failed to retrieve e-mails. The server says: '{}'", response.response_text()));
        return;
    }

    Vector<InboxEntry> active_inbox_entries;

    for (auto& fetch_data : fetch_response.data().fetch_data()) {
        auto& response_data = fetch_data.get<IMAP::FetchResponseData>();
        auto& body_data = response_data.body_data();

        auto data_item_has_header = [](IMAP::FetchCommand::DataItem const& data_item, String const& search_header) {
            return data_item.section.has_value()
                && data_item.section->type == IMAP::FetchCommand::DataItem::SectionType::HeaderFields
                && data_item.section->headers.has_value()
                && data_item.section->headers->find_if([&search_header](String const& header) {
                       return header == search_header;
                   })
                != data_item.section->headers->end();
        };

        auto subject_iterator = body_data.find_if([&data_item_has_header](Tuple<IMAP::FetchCommand::DataItem, Optional<String>>& data) {
            auto const data_item = data.get<0>();
            return data_item_has_header(data_item, "Subject");
        });

        VERIFY(subject_iterator != body_data.end());

        auto from_iterator = body_data.find_if([&data_item_has_header](Tuple<IMAP::FetchCommand::DataItem, Optional<String>>& data) {
            auto const data_item = data.get<0>();
            return data_item_has_header(data_item, "From");
        });

        VERIFY(from_iterator != body_data.end());

        auto& subject_value = subject_iterator->get<1>().value();
        auto subject_parts = subject_value.split(':');
        auto subject = subject_parts.last().trim_whitespace();

        if (subject.is_empty())
            subject = "(no subject)";

        auto& from_value = from_iterator->get<1>().value();
        auto from_parts = from_value.split(':');
        auto from = from_parts.last().trim_whitespace();

        InboxEntry inbox_entry { from, subject };

        active_inbox_entries.append(inbox_entry);
    }

    m_individual_mailbox_view->set_model(InboxModel::create(active_inbox_entries));
    m_individual_mailbox_view->on_selection_change = [this] {
        selected_email_to_load();
    };
}

void MailWidget::selected_email_to_load()
{
    auto const& index = m_individual_mailbox_view->selection().first();

    if (!index.is_valid())
        return;

    // IMAP is 1-based.
    int id_of_email_to_load = index.row() + 1;

    auto fetch_command = IMAP::FetchCommand {
        .sequence_set = { { id_of_email_to_load, id_of_email_to_load } },
        .data_items = {
            IMAP::FetchCommand::DataItem {
                .type = IMAP::FetchCommand::DataItemType::BodyStructure,
            },
        },
    };

    auto fetch_response = m_imap_client->fetch(fetch_command, false)->await().release_value();

    if (fetch_response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve the body structure of the selected e-mail. The server says: '{}'", fetch_response.response_text());
        GUI::MessageBox::show_error(window(), String::formatted("Failed to retrieve the selected e-mail. The server says: '{}'", fetch_response.response_text()));
        return;
    }

    Vector<int> selected_alternative_position;
    String selected_alternative_encoding;

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
                GUI::MessageBox::show_error(window(), String::formatted("The server sent no message to display."));
                return;
            }

            // We can choose whichever alternative we want. In general, we should choose the last alternative that know we can display.
            // RFC 2046 Section 5.1.4 https://datatracker.ietf.org/doc/html/rfc2046#section-5.1.4
            auto chosen_alternative = alternatives.last_matching([this](auto& alternative) {
                return is_supported_alternative(alternative);
            });

            if (alternatives.is_empty()) {
                GUI::MessageBox::show(window(), String::formatted("Displaying this type of e-mail is currently unsupported."), "Unsupported", GUI::MessageBox::Type::Information);
                return;
            }
            selected_alternative_position = chosen_alternative.value().position;
            selected_alternative_encoding = chosen_alternative.value().body_structure.encoding;
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
        },
    };

    fetch_response = m_imap_client->fetch(fetch_command, false)->await().release_value();

    if (fetch_response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve the body of the selected e-mail. The server says: '{}'", fetch_response.response_text());
        GUI::MessageBox::show_error(window(), String::formatted("Failed to retrieve the selected e-mail. The server says: '{}'", fetch_response.response_text()));
        return;
    }

    auto& encoded_data = fetch_response.data()
                             .fetch_data()
                             .last()
                             .get<IMAP::FetchResponseData>()
                             .body_data()
                             .find_if([](Tuple<IMAP::FetchCommand::DataItem, Optional<String>>& data) {
                                 const auto data_item = data.get<0>();
                                 return data_item.section.has_value() && data_item.section->type == IMAP::FetchCommand::DataItem::SectionType::Parts;
                             })
                             ->get<1>()
                             .value();

    String decoded_data;

    // FIXME: String uses char internally, so 8bit shouldn't be stored in it.
    //        However, it works for now.
    if (selected_alternative_encoding.equals_ignoring_case("7bit") || selected_alternative_encoding.equals_ignoring_case("8bit")) {
        decoded_data = encoded_data;
    } else if (selected_alternative_encoding.equals_ignoring_case("base64")) {
        decoded_data = decode_base64(encoded_data);
    } else if (selected_alternative_encoding.equals_ignoring_case("quoted-printable")) {
        decoded_data = IMAP::decode_quoted_printable(encoded_data);
    } else {
        dbgln("Mail: Unimplemented decoder for encoding: {}", selected_alternative_encoding);
        TODO();
    }

    // FIXME: I'm not sure what the URL should be. Just use the default URL "about:blank".
    m_web_view->load_html(decoded_data, "about:blank");
}
