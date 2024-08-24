/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/GetPassword.h>
#include <LibIMAP/Client.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (pledge("stdio inet tty rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    ByteString host;
    int port = 0;
    bool tls { false };

    ByteString username;
    Core::SecretString password;
    bool interactive_password = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(interactive_password, "Prompt for password with getpass", "interactive", 'i');
    args_parser.add_option(tls, "Connect with TLS (IMAPS)", "secure", 's');
    args_parser.add_positional_argument(host, "IMAP host", "host");
    args_parser.add_positional_argument(port, "Port to connect to", "port");
    args_parser.add_positional_argument(username, "Username", "username");
    args_parser.parse(arguments);

    if (interactive_password) {
        password = TRY(Core::get_password());
    } else {
        auto standard_input = TRY(Core::File::standard_input());
        // This might leave the clear password in unused memory, but this is only a test program anyway.
        password = Core::SecretString::take_ownership(TRY(standard_input->read_until_eof()));
    }

    Core::EventLoop loop;
    auto client = TRY(tls ? IMAP::Client::connect_tls(host, port) : IMAP::Client::connect_plaintext(host, port));
    TRY(client->connection_promise()->await());

    auto response = TRY(client->login(username, password.view())->await());
    outln("[LOGIN] Login response: {}", response.response_text());

    response = move(TRY(client->send_simple_command(IMAP::CommandType::Capability)->await()).get<IMAP::SolidResponse>());
    outln("[CAPABILITY] First capability: {}", response.data().capabilities().first());
    bool idle_supported = !response.data().capabilities().find_if([](auto capability) { return capability.equals_ignoring_ascii_case("IDLE"sv); }).is_end();

    response = TRY(client->list(""sv, "*"sv)->await());
    outln("[LIST] First mailbox: {}", response.data().list_items().first().name);

    auto mailbox = "Inbox"sv;
    response = TRY(client->select(mailbox)->await());
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
    auto promise = client->append("INBOX"sv, move(message));
    response = TRY(promise->await());
    outln("[APPEND] Response: {}", response.response_text());

    Vector<IMAP::SearchKey> keys;
    keys.append(IMAP::SearchKey {
        IMAP::SearchKey::From { "jdoe@machine.example" } });
    keys.append(IMAP::SearchKey {
        IMAP::SearchKey::Subject { "Saying Hello" } });
    response = TRY(client->search({}, move(keys), false)->await());

    Vector<unsigned> search_results = move(response.data().search_results());
    auto added_message = search_results.first();
    outln("[SEARCH] Number of results: {}", search_results.size());

    response = TRY(client->status("INBOX"sv, { IMAP::StatusItemType::Recent, IMAP::StatusItemType::Messages })->await());
    if (response.data().status_items().size() > 0)
        outln("[STATUS] Recent items: {}", response.data().status_items()[0].get(IMAP::StatusItemType::Recent));

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

        auto fetch_response = TRY(client->fetch(fetch_command, false)->await());
        outln("[FETCH] Subject of search result: {}",
            fetch_response.data()
                .fetch_data()
                .first()
                .get<IMAP::FetchResponseData>()
                .body_data()
                .find_if([](Tuple<IMAP::FetchCommand::DataItem, ByteString>& data) {
                    auto const data_item = data.get<0>();
                    return data_item.section.has_value() && data_item.section->type == IMAP::FetchCommand::DataItem::SectionType::HeaderFields;
                })
                ->get<1>());
    }

    // FIXME: There is a discrepancy between IMAP::Sequence wanting signed ints
    //        and IMAP search results returning unsigned ones. Find which one is
    //        more correct and fix this.
    response = TRY(client->store(IMAP::StoreMethod::Add, { static_cast<int>(added_message), static_cast<int>(added_message) }, false, { "\\Deleted" }, false)->await());
    outln("[STORE] Store response: {}", response.response_text());

    response = move(TRY(client->send_simple_command(IMAP::CommandType::Expunge)->await()).get<IMAP::SolidResponse>());
    outln("[EXPUNGE] Number of expunged entries: {}", response.data().expunged().size());

    if (idle_supported) {
        VERIFY(!client->idle()->await().is_error());
        sleep(3);
        response = TRY(client->finish_idle()->await());
        outln("[IDLE] Idle response: {}", response.response_text());
    } else {
        outln("[IDLE] Skipped. No IDLE support.");
    }

    response = move(TRY(client->send_simple_command(IMAP::CommandType::Logout)->await()).get<IMAP::SolidResponse>());
    outln("[LOGOUT] Bye: {}", response.data().bye_message().value());

    client->close();

    return 0;
}
