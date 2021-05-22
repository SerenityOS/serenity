/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FTPServerClient.h"
#include "FTPServerTransferModel.h"
#include <AK/JsonParser.h>
#include <LibCore/TCPServer.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextEditor.h>

struct FileTransferProgress {
    u32 client_id;
    String file;
    u64 bytes;
};

class FTPServer final : public Core::Object {
    C_OBJECT(FTPServer)
public:
    FTPServer(int, AK::JsonObject);

    void start();
    u32 transfer_count() { return m_transfers.size(); }

    FileTransferProgress transfer_at(u32 index) { return m_transfers[index]; }

    Optional<NonnullRefPtr<FTPServerClient>> client_with_id(u32 id)
    {
        return m_clients.first_matching([id](FTPServerClient& item) {
            return item.id() == id;
        });
    }

    RefPtr<GUI::TextEditor> m_log_view;
    RefPtr<GUI::TableView> m_transfer_table;

private:
    void on_ready_to_accept();
    void log(String);

    u16 m_port;
    AK::JsonObject m_json_settings;

    u32 next_client_id {};
    RefPtr<Core::TCPServer> m_server;
    NonnullRefPtrVector<FTPServerClient> m_clients;
    Vector<FileTransferProgress> m_transfers;

    FTPServerTransferModel* m_transfer_model;
};
