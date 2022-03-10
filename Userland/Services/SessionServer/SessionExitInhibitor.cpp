/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SessionExitInhibitor.h"
#include "ConnectionFromClient.h"

SessionExitInhibitor& SessionExitInhibitor::the()
{
    static SessionExitInhibitor s_the;
    return s_the;
}

void SessionExitInhibitor::inhibit_exit(int client_id) { m_exit_inhibiting_client_ids.append(client_id); }

void SessionExitInhibitor::allow_exit(int client_id)
{
    m_exit_inhibiting_client_ids.remove_all_matching([&](auto id) { return id == client_id; });
}

void SessionExitInhibitor::on_inhibited_exit_prevented()
{
    SessionServer::ConnectionFromClient::for_each_client([&](auto& client) {
        client.on_inhibited_exit_prevented();
    });
}
