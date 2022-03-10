/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIPC/ConnectionToServer.h>
#include <LibSession/Session.h>
#include <SessionServer/SessionExitInhibitionClientEndpoint.h>
#include <SessionServer/SessionExitInhibitionServerEndpoint.h>

namespace Session {

class ConnectionToSessionExitInhibitionServer final
    : public IPC::ConnectionToServer<SessionExitInhibitionClientEndpoint, SessionExitInhibitionServerEndpoint>
    , public SessionExitInhibitionClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToSessionExitInhibitionServer, "/tmp/portal/session")

private:
    ConnectionToSessionExitInhibitionServer(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<SessionExitInhibitionClientEndpoint, SessionExitInhibitionServerEndpoint>(*this, move(socket))
    {
    }

    virtual void on_inhibited_exit_prevented() override
    {
        auto& session = Session::the();
        if (session.on_inhibited_exit_prevented)
            session.on_inhibited_exit_prevented();
    }
};

static RefPtr<ConnectionToSessionExitInhibitionServer> s_connection;

static ConnectionToSessionExitInhibitionServer& connection()
{
    auto result = ConnectionToSessionExitInhibitionServer::try_create();
    if (result.is_error())
        dbgln("Session: {}", result.error());
    else
        s_connection = result.release_value();
    return *s_connection;
}

Session& Session::the()
{
    static Session s_the;
    return s_the;
}

Session::~Session()
{
    connection().shutdown();
}

void Session::inhibit_exit()
{
    connection().async_inhibit_exit();
}

void Session::allow_exit()
{
    connection().async_allow_exit();
}

bool Session::is_exit_inhibited()
{
    return connection().is_exit_inhibited();
}

void Session::report_inhibited_exit_prevention()
{
    connection().async_report_inhibited_exit_prevention();
}

}
