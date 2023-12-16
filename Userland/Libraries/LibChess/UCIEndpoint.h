/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibChess/UCICommand.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>

namespace Chess::UCI {

class Endpoint : public Core::EventReceiver {
    C_OBJECT(Endpoint)
public:
    virtual ~Endpoint() override = default;

    Function<void(ByteString, Error)> on_command_read_error;

    virtual void handle_uci() { }
    virtual void handle_debug(DebugCommand const&) { }
    virtual void handle_isready() { }
    virtual void handle_setoption(SetOptionCommand const&) { }
    virtual void handle_position(PositionCommand const&) { }
    virtual void handle_go(GoCommand const&) { }
    virtual void handle_stop() { }
    virtual void handle_id(IdCommand const&) { }
    virtual void handle_uciok() { }
    virtual void handle_readyok() { }
    virtual void handle_bestmove(BestMoveCommand const&) { }
    virtual void handle_info(InfoCommand const&) { }
    virtual void handle_quit() { }
    virtual void handle_ucinewgame() { }
    virtual void handle_unexpected_eof() { }

    void send_command(Command const&);

    virtual void event(Core::Event&) override;

    ErrorOr<void> set_in(NonnullOwnPtr<Core::File> in)
    {
        m_in_fd = in->fd();
        m_in = TRY(Core::InputBufferedFile::create(move(in)));
        set_in_notifier();
        return {};
    }
    void set_out(NonnullOwnPtr<Core::File> out) { m_out = move(out); }

protected:
    Endpoint() = default;
    virtual void custom_event(Core::CustomEvent&) override;

private:
    enum EndpointEventType {
        UnexpectedEof
    };
    void set_in_notifier();
    ErrorOr<NonnullOwnPtr<Command>> read_command(StringView line) const;

    Optional<int> m_in_fd {};
    OwnPtr<Core::InputBufferedFile> m_in;
    OwnPtr<Core::File> m_out;
    RefPtr<Core::Notifier> m_in_notifier;
};

}
