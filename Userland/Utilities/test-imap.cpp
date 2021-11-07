/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/GetPassword.h>
#include <LibIMAP/Client.h>

int main(int argc, char** argv)
{
    if (pledge("stdio inet tty rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    String host;
    int port;
    bool tls { false };

    String username;
    Core::SecretString password;

    bool interactive_password;

    Core::ArgsParser args_parser;
    args_parser.add_option(interactive_password, "Prompt for password with getpass", "interactive", 'i');
    args_parser.add_option(tls, "Connect with TLS (IMAPS)", "secure", 's');
    args_parser.add_positional_argument(host, "IMAP host", "host");
    args_parser.add_positional_argument(port, "Port to connect to", "port");
    args_parser.add_positional_argument(username, "Username", "username");
    args_parser.parse(argc, argv);

    if (interactive_password) {
        auto password_or_error = Core::get_password();
        if (password_or_error.is_error()) {
            warnln("{}", password_or_error.error());
            return 1;
        }
        password = password_or_error.release_value();
    } else {
        auto standard_input = Core::File::standard_input();
        password = Core::SecretString::take_ownership(standard_input->read_all());
    }

    Core::EventLoop loop;
    auto client = IMAP::Client(host, port, tls);
    client.connect()->await();

    auto response = client.login(username, password.view())->await().release_value();
    outln("[LOGIN] Login response: {}", response.response_text());

    response = move(client.send_simple_command(IMAP::CommandType::Capability)->await().value().get<IMAP::SolidResponse>());
    outln("[CAPABILITY] First capability: {}", response.data().capabilities().first());
    bool idle_supported = !response.data().capabilities().find_if([](auto capability) { return capability.equals_ignoring_case("IDLE"); }).is_end();

    response = client.list("", "*")->await().release_value();
    outln("[LIST] First mailbox: {}", response.data().list_items().first().name);

    auto mailbox = "Inbox";
    response = client.select(mailbox)->await().release_value();
    outln("[SELECT] Select response: {}", response.response_text());

    auto message = Message {
        "From: John Doe <jdoe@machine.example>\r\n"
        "To: Mary Smith <mary@example.net>\r\n"
        "Subject: Saying Hello\r\n"
        "Date: Fri, 21 Nov 1997 09:55:06 -0600\r\n"
        "Message-ID: <1234@local.machine.example>\r\n"
        "\r\n"
        "This is a message just to say hello.\r\n"
        "So, \"Hello\"."
    };
    auto promise = client.append("INBOX", move(message));
    response = promise->await().release_value();
    outln("[APPEND] Response: {}", response.response_text());

    Vector<IMAP::SearchKey> keys;
    keys.append(IMAP::SearchKey {
        IMAP::SearchKey::From { "jdoe@machine.example" } });
    keys.append(IMAP::SearchKey {
        IMAP::SearchKey::Subject { "Saying Hello" } });
    response = client.search({}, move(keys), false)->await().release_value();

    Vector<unsigned> search_results = move(response.data().search_results());
    int added_message = search_results.first();
    outln("[SEARCH] Number of results: {}", search_results.size());

    response = client.status("INBOX", { IMAP::StatusItemType::Recent, IMAP::StatusItemType::Messages })->await().release_value();
    outln("[STATUS] Recent items: {}", response.data().status_item().get(IMAP::StatusItemType::Recent));

    for (auto item : search_results) {
        // clang-format off
        // clang formats this very badly
        auto fetch_command = IMAP::FetchCommand {
            .sequence_set = { { (int)item, (int)item } },
            .data_items = {
                IMAP::FetchCommand::DataItem {
                    .type = IMAP::FetchCommand::DataItemType::BodyStructure
                },
                IMAP::FetchCommand::DataItem {
                    .type = IMAP::FetchCommand::DataItemType::BodySection,
                    .section = IMAP::FetchCommand::DataItem::Section {
                       .type = IMAP::FetchCommand::DataItem::SectionType::HeaderFields,
                       .headers = { { "Subject" } }
                    }
                },
                IMAP::FetchCommand::DataItem {
                    .type = IMAP::FetchCommand::DataItemType::BodySection,
                    .section = IMAP::FetchCommand::DataItem::Section {
                       .type = IMAP::FetchCommand::DataItem::SectionType::Parts,
                       .parts = { { 1 } }
                    },
                    .partial_fetch = true,
                    .start = 0,
                    .octets = 8192
                }
            }
        };
        // clang-format on

        auto fetch_response = client.fetch(fetch_command, false)->await().release_value();
        outln("[FETCH] Subject of search result: {}",
            fetch_response.data()
                .fetch_data()
                .first()
                .get<IMAP::FetchResponseData>()
                .body_data()
                .find_if([](Tuple<IMAP::FetchCommand::DataItem, Optional<String>>& data) {
                    const auto data_item = data.get<0>();
                    return data_item.section.has_value() && data_item.section->type == IMAP::FetchCommand::DataItem::SectionType::HeaderFields;
                })
                ->get<1>()
                .value());
    }

    response = client.store(IMAP::StoreMethod::Add, { added_message, added_message }, false, { "\\Deleted" }, false)->await().release_value();
    outln("[STORE] Store response: {}", response.response_text());

    response = move(client.send_simple_command(IMAP::CommandType::Expunge)->await().release_value().get<IMAP::SolidResponse>());
    outln("[EXPUNGE] Number of expunged entries: {}", response.data().expunged().size());

    if (idle_supported) {
        VERIFY(client.idle()->await().has_value());
        sleep(3);
        response = client.finish_idle()->await().release_value();
        outln("[IDLE] Idle response: {}", response.response_text());
    } else {
        outln("[IDLE] Skipped. No IDLE support.");
    }

    response = move(client.send_simple_command(IMAP::CommandType::Logout)->await().release_value().get<IMAP::SolidResponse>());
    outln("[LOGOUT] Bye: {}", response.data().bye_message().value());

    client.close();

    return 0;
}
