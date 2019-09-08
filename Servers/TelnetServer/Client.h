#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CTCPSocket.h>

#include "Command.h"
#include "Parser.h"

class Client : public RefCounted<Client> {
public:
    static NonnullRefPtr<Client> create(int id, CTCPSocket* socket, int ptm_fd)
    {
        return adopt(*new Client(id, socket, ptm_fd));
    }

    Function<void()> on_exit;

protected:
    Client(int id, CTCPSocket* socket, int ptm_fd);

    void drain_socket();
    void drain_pty();
    void handle_data(const StringView&);
    void handle_command(const Command& command);
    void handle_error();
    void send_data(StringView str);
    void send_command(Command command);
    void send_commands(Vector<Command> commands);
    void quit();

private:
    // client id
    int m_id { 0 };
    // client resources
    CTCPSocket* m_socket { nullptr };
    Parser m_parser;
    // pty resources
    int m_ptm_fd { -1 };
    CNotifier m_ptm_notifier;
};
