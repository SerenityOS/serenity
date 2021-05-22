/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FTPServerTransferModel.h"
#include "FTPServer.h"
#include <stdio.h>
#include <time.h>

FTPServerTransferModel::FTPServerTransferModel(FTPServer& server)
    : m_server(server)
{
}

FTPServerTransferModel::~FTPServerTransferModel()
{
}

int FTPServerTransferModel::row_count(const GUI::ModelIndex&) const
{
    return m_server.transfer_count();
}

int FTPServerTransferModel::column_count(const GUI::ModelIndex&) const
{
    return 3;
}

String FTPServerTransferModel::column_name(int column) const
{
    switch (column) {
    case Column::Client:
        return "Client";
    case Column::File:
        return "File";
    case Column::Bytes:
        return "Bytes";
    }
    VERIFY_NOT_REACHED();
}

GUI::Variant FTPServerTransferModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::TextAlignment)
        return Gfx::TextAlignment::CenterLeft;
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Client: {
            u32 client_id = m_server.transfer_at(index.row()).client_id;
            auto client = m_server.client_with_id(client_id);
            if (client.has_value()) {
                return client.value()->user();
            }

            return "INVALID";
        }
        case Column::File:
            return m_server.transfer_at(index.row()).file;
        case Column::Bytes:
            return m_server.transfer_at(index.row()).bytes;
        }
    }
    return {};
}

void FTPServerTransferModel::update()
{
    did_update();
}
