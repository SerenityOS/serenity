/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionFromClient.h>
#include <SessionServer/ConnectionFromClient.h>
#include <SessionServer/SessionExitInhibitionClientEndpoint.h>
#include <SessionServer/SessionExitInhibitionServerEndpoint.h>

namespace SessionServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<SessionExitInhibitionClientEndpoint, SessionExitInhibitionServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    virtual ~ConnectionFromClient() override;

    virtual void die() override;

    static void for_each_client(Function<void(ConnectionFromClient&)>);

    void on_inhibited_exit_prevented();

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual void inhibit_exit() override;
    virtual void allow_exit() override;
    virtual Messages::SessionExitInhibitionServer::IsExitInhibitedResponse is_exit_inhibited() override;
    virtual void report_inhibited_exit_prevention() override;
};

}
