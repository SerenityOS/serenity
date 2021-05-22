/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FTPServer.h"
#include <LibThreading/Thread.h>
#include <stdio.h>

FTPServer::FTPServer(int port, AK::JsonObject json_settings)
    : m_port(port)
    , m_json_settings(json_settings)
{
}

void FTPServer::log(String value)
{
    if (!m_log_view) {
        outln(value);
        dbgln(value);
        return;
    }

    StringBuilder builder;
    builder.append(m_log_view->get_text());
    builder.append(value);
    m_log_view->set_text(builder.to_string());
}

void FTPServer::start()
{
    m_server = Core::TCPServer::construct();
    IPv4Address address = {};

    if (!m_server->listen(address, m_port)) {
        log(String::formatted("INFO SERVER: Listening on {}:{} failed, restart to try again\n", address, m_port));
    }

    m_server->on_ready_to_accept = [this] {
        on_ready_to_accept();
    };

    log(String::formatted("INFO SERVER: Listening on {}:{}\n\n", address, m_port));
}

void FTPServer::on_ready_to_accept()
{
    u32 id = next_client_id++;

    StringBuilder builder;
    builder.appendff("FTPServer client ({})", id);

    auto client_socket = m_server->accept();
    if (!client_socket) {
        perror("accept");
        return;
    }

    auto client_thread = Threading::Thread::construct([=, this] {
        log(String::formatted("INFO SERVER: Client {} connected\n\n", id));

        auto client = FTPServerClient::create(id, move(client_socket), m_json_settings);
        client->on_exit = [this, id] {
            auto maybe_client = m_clients.first_matching([id](FTPServerClient& item) {
                return item.id() == id;
            });

            if (maybe_client.has_value()) {
                m_clients.remove_first_matching([id](FTPServerClient& item) {
                    return item.id() == id;
                });

                log(String::formatted("INFO SERVER: Client {} disconnected\n\n", id));
            }
        };

        client->on_receive_command = [this](FTPServerClient* client, String action) {
            log(String::formatted("CLIENT ({}({})) -> SERVER: {}\n\n", client->user(), client->id(), action));
        };

        client->on_send_command = [this](FTPServerClient* client, String action) {
            log(String::formatted("SERVER -> CLIENT ({}({})): {}\n", client->user(), client->id(), action));
        };

        client->on_info = [this](FTPServerClient* client, String action) {
            log(String::formatted("INFO SERVER: TO ({}({})): {}\n\n", client->user(), client->id(), action));
        };

        // FIXME: Run the update in the UI thread
        client->on_data_transfer_start = [this](u32 id, String path) {
            FileTransferProgress transfer_data = {
                .client_id = id,
                .file = path,
                .bytes = 0,
            };

            m_transfers.append(transfer_data);
            m_transfer_table->model()->update();
        };

        // FIXME: Run the update in the UI thread
        client->on_data_transfer_update = [this](u32 id, int bytes_sent) {
            auto transfer = m_transfers.find_if([id](FileTransferProgress& item) {
                return item.client_id == id;
            });

            if (transfer == m_transfers.end()) {
                return;
            }

            transfer->bytes += bytes_sent;
            m_transfer_table->model()->update();
        };

        // FIXME: Run the update in the UI thread
        client->on_data_transfer_end = [this](u32 id) {
            m_transfers.remove_first_matching([id](FileTransferProgress& item) {
                return item.client_id == id;
            });

            m_transfer_table->model()->update();
        };

        client->send_welcome();

        m_clients.append(move(client));

        return 0;

        // FIXME: This formatting is weird...
    },
        builder.to_string());

    client_thread->start();
}
