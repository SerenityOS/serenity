/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Undefine <cqundefine@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailWidget.h"
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
        selected_email_to_load(index);
    };

    m_web_view->on_link_click = [this](auto& url, auto&, unsigned) {
        if (!Desktop::Launcher::open(url)) {
            GUI::MessageBox::show(
                window(),
                DeprecatedString::formatted("The link to '{}' could not be opened.", url),
                "Failed to open link"sv,
                GUI::MessageBox::Type::Error);
        }
    };

    m_web_view->on_link_middle_click = [this](auto& url, auto& target, unsigned modifiers) {
        m_web_view->on_link_click(url, target, modifiers);
    };

    m_web_view->on_link_hover = [this](auto& url) {
        if (url.is_valid())
            m_statusbar->set_text(String::from_deprecated_string(url.to_deprecated_string()).release_value_but_fixme_should_propagate_errors());
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
        GUI::Clipboard::the().set_plain_text(m_link_context_menu_url.to_deprecated_string());
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
        GUI::Clipboard::the().set_plain_text(m_image_context_menu_url.to_deprecated_string());
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
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to connect to '{}:{}' over {}: {}", server, port, tls ? "TLS" : "Plaintext", maybe_imap_client.error()));
        return false;
    }
    m_imap_client = maybe_imap_client.release_value();

    TRY(m_imap_client->connection_promise()->await());

    m_statusbar->set_text(String::formatted("Connected. Logging in as {}...", username).release_value_but_fixme_should_propagate_errors());

    auto response = TRY(m_imap_client->login(username, password)->await());

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to login. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to login. The server says: '{}'", response.response_text()));
        m_statusbar->set_text("Failed to log in"_string);
        return false;
    }

    m_statusbar->set_text("Logged in. Loading mailboxes..."_string);
    response = TRY(m_imap_client->list(""sv, "*"sv)->await());

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to retrieve mailboxes. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to retrieve mailboxes. The server says: '{}'", response.response_text()));
        return false;
    }

    auto& list_items = response.data().list_items();

    m_statusbar->set_text(String::formatted("Loaded {} mailboxes", list_items.size()).release_value_but_fixme_should_propagate_errors());

    m_account_holder = AccountHolder::create();
    m_account_holder->add_account_with_name_and_mailboxes(username, move(list_items));

    m_mailbox_list->set_model(m_account_holder->mailbox_tree_model());
    m_mailbox_list->expand_tree();

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
    if (current_body_structure.media_type.equals_ignoring_ascii_case("ALTERNATIVE"sv))
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
    m_individual_mailbox_model = InboxModel::create({});
    m_individual_mailbox_view->set_model(m_individual_mailbox_model);

    if (!index.is_valid())
        return;

    auto& base_node = *static_cast<BaseNode*>(index.internal_data());

    if (is<AccountNode>(base_node)) {
        // FIXME: Do something when clicking on an account node.
        return;
    }

    auto& mailbox_node = verify_cast<MailboxNode>(base_node);
    auto& mailbox = mailbox_node.mailbox();

    // FIXME: It would be better if we didn't allow the user to click on this mailbox node at all.
    if (mailbox.flags & (unsigned)IMAP::MailboxFlag::NoSelect)
        return;

    auto response = MUST(m_imap_client->select(mailbox.name)->await());

    if (response.status() != IMAP::ResponseStatus::OK) {
        dbgln("Failed to select mailbox. The server says: '{}'", response.response_text());
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to select mailbox. The server says: '{}'", response.response_text()));
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
                .type = IMAP::FetchCommand::DataItemType::PeekBody,
                .section = IMAP::FetchCommand::DataItem::Section {
                    .type = IMAP::FetchCommand::DataItem::SectionType::HeaderFields,
                    .headers = { { "Subject", "From" } },
                },
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
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to retrieve e-mails. The server says: '{}'", response.response_text()));
        return;
    }

    Vector<InboxEntry> active_inbox_entries;

    int i = 0;
    for (auto& fetch_data : fetch_response.data().fetch_data()) {
        auto sequence_number = fetch_data.get<unsigned>();
        auto& response_data = fetch_data.get<IMAP::FetchResponseData>();
        auto& body_data = response_data.body_data();
        auto& internal_date = response_data.internal_date();

        auto seen = !response_data.flags().find_if([](StringView value) { return value.equals_ignoring_ascii_case("\\Seen"sv); }).is_end();

        auto data_item_has_header = [](IMAP::FetchCommand::DataItem const& data_item, DeprecatedString const& search_header) {
            if (!data_item.section.has_value())
                return false;
            if (data_item.section->type != IMAP::FetchCommand::DataItem::SectionType::HeaderFields)
                return false;
            if (!data_item.section->headers.has_value())
                return false;
            auto header_iterator = data_item.section->headers->find_if([&search_header](auto& header) {
                return header.equals_ignoring_ascii_case(search_header);
            });
            return header_iterator != data_item.section->headers->end();
        };

        auto subject_iterator = body_data.find_if([&data_item_has_header](Tuple<IMAP::FetchCommand::DataItem, Optional<DeprecatedString>>& data) {
            auto const data_item = data.get<0>();
            return data_item_has_header(data_item, "Subject");
        });

        VERIFY(subject_iterator != body_data.end());

        auto from_iterator = body_data.find_if([&data_item_has_header](Tuple<IMAP::FetchCommand::DataItem, Optional<DeprecatedString>>& data) {
            auto const data_item = data.get<0>();
            return data_item_has_header(data_item, "From");
        });

        VERIFY(from_iterator != body_data.end());

        // FIXME: All of the following doesn't really follow RFC 2822: https://datatracker.ietf.org/doc/html/rfc2822

        auto parse_and_unfold = [](DeprecatedString const& value) {
            GenericLexer lexer(value);
            StringBuilder builder;

            // There will be a space at the start of the value, which should be ignored.
            VERIFY(lexer.consume_specific(' '));

            while (!lexer.is_eof()) {
                auto current_line = lexer.consume_while([](char c) {
                    return c != '\r';
                });

                builder.append(current_line);

                bool consumed_end_of_line = lexer.consume_specific("\r\n");
                VERIFY(consumed_end_of_line);

                // If CRLF are immediately followed by WSP (which is either ' ' or '\t'), then it is not the end of the header and is instead just a wrap.
                // If it's not followed by WSP, then it is the end of the header.
                // https://datatracker.ietf.org/doc/html/rfc2822#section-2.2.3
                if (lexer.is_eof() || (lexer.peek() != ' ' && lexer.peek() != '\t'))
                    break;
            }

            return builder.to_deprecated_string();
        };

        DeprecatedString date = internal_date.to_deprecated_string();

        auto& subject_iterator_value = subject_iterator->get<1>().value();
        auto subject_index = subject_iterator_value.find("Subject:"sv);
        DeprecatedString subject;
        if (subject_index.has_value()) {
            auto potential_subject = subject_iterator_value.substring(subject_index.value());
            auto subject_parts = potential_subject.split_limit(':', 2);
            subject = parse_and_unfold(subject_parts.last());
        }

        if (subject.is_empty())
            subject = "(No subject)";

        if (subject.contains("=?"sv) && subject.contains("?="sv)) {
            subject = MUST(IMAP::decode_rfc2047_encoded_words(subject));
        }

        auto& from_iterator_value = from_iterator->get<1>().value();
        auto from_index = from_iterator_value.find("From:"sv);
        if (!from_index.has_value())
            from_index = from_iterator_value.find("from:"sv);
        DeprecatedString from;
        if (from_index.has_value()) {
            auto potential_from = from_iterator_value.substring(from_index.value());
            auto from_parts = potential_from.split_limit(':', 2);
            from = parse_and_unfold(from_parts.last());
        }

        if (from.is_empty())
            from = "(Unknown sender)";

        InboxEntry inbox_entry { sequence_number, date, from, subject, seen };
        m_statusbar->set_text(String::formatted("[{}]: Loading entry {}", mailbox.name, ++i).release_value_but_fixme_should_propagate_errors());

        active_inbox_entries.append(inbox_entry);
    }

    m_statusbar->set_text(String::formatted("[{}]: Loaded {} entries", mailbox.name, i).release_value_but_fixme_should_propagate_errors());
    m_individual_mailbox_model = InboxModel::create(move(active_inbox_entries));
    m_individual_mailbox_view->set_model(m_individual_mailbox_model);
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
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to retrieve the selected e-mail. The server says: '{}'", fetch_response.response_text()));
        return;
    }

    Vector<u32> selected_alternative_position;
    DeprecatedString selected_alternative_encoding;

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
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to retrieve the selected e-mail. The server says: '{}'", fetch_response.response_text()));
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
    m_individual_mailbox_model->set_seen(index.row(), seen);

    if (!fetch_response_data.contains_response_type(IMAP::FetchResponseType::Body)) {
        GUI::MessageBox::show_error(window(), "The server sent no body."sv);
        return;
    }

    auto& body_data = fetch_response_data.body_data();
    auto body_text_part_iterator = body_data.find_if([](Tuple<IMAP::FetchCommand::DataItem, Optional<DeprecatedString>>& data) {
        const auto data_item = data.get<0>();
        return data_item.section.has_value() && data_item.section->type == IMAP::FetchCommand::DataItem::SectionType::Parts;
    });
    VERIFY(body_text_part_iterator != body_data.end());

    auto& encoded_data = body_text_part_iterator->get<1>().value();

    DeprecatedString decoded_data;

    // FIXME: String uses char internally, so 8bit shouldn't be stored in it.
    //        However, it works for now.
    if (selected_alternative_encoding.equals_ignoring_ascii_case("7bit"sv) || selected_alternative_encoding.equals_ignoring_ascii_case("8bit"sv)) {
        decoded_data = encoded_data;
    } else if (selected_alternative_encoding.equals_ignoring_ascii_case("base64"sv)) {
        auto decoded_base64 = decode_base64(encoded_data);
        if (!decoded_base64.is_error())
            decoded_data = decoded_base64.release_value();
    } else if (selected_alternative_encoding.equals_ignoring_ascii_case("quoted-printable"sv)) {
        decoded_data = IMAP::decode_quoted_printable(encoded_data).release_value_but_fixme_should_propagate_errors();
    } else {
        dbgln("Mail: Unimplemented decoder for encoding: {}", selected_alternative_encoding);
        GUI::MessageBox::show(window(), DeprecatedString::formatted("The e-mail encoding '{}' is currently unsupported.", selected_alternative_encoding), "Unsupported"sv, GUI::MessageBox::Type::Information);
        return;
    }

    m_statusbar->set_text("Message loaded."_string);

    // FIXME: I'm not sure what the URL should be. Just use the default URL "about:blank".
    // FIXME: It would be nice if we could pass over the charset.
    // FIXME: Add ability to cancel the load when we switch to another email. Feels very sluggish on heavy emails otherwise
    m_web_view->load_html(decoded_data, "about:blank"sv);
}
