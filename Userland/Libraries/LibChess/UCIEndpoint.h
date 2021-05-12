/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibChess/UCICommand.h>
#include <LibCore/File.h>
#include <LibCore/IODevice.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>

namespace Chess::UCI {

class Endpoint : public Core::Object {
    C_OBJECT(Endpoint)
public:
    virtual ~Endpoint() override { }

    Endpoint() { }
    Endpoint(NonnullRefPtr<Core::File> in, NonnullRefPtr<Core::IODevice> out);

    virtual void handle_uci() { }
    virtual void handle_debug(const DebugCommand&) { }
    virtual void handle_isready() { }
    virtual void handle_setoption(const SetOptionCommand&) { }
    virtual void handle_position(const PositionCommand&) { }
    virtual void handle_go(const GoCommand&) { }
    virtual void handle_stop() { }
    virtual void handle_id(const IdCommand&) { }
    virtual void handle_uciok() { }
    virtual void handle_readyok() { }
    virtual void handle_bestmove(const BestMoveCommand&) { }
    virtual void handle_info(const InfoCommand&) { }

    void send_command(const Command&);

    virtual void event(Core::Event&);

    auto& in() { return *m_in; }
    auto& out() { return *m_out; }

    void set_in(RefPtr<Core::File> in)
    {
        m_in = move(in);
        set_in_notifier();
    }
    void set_out(RefPtr<Core::IODevice> out) { m_out = move(out); }

private:
    void set_in_notifier();
    NonnullOwnPtr<Command> read_command();

    RefPtr<Core::File> m_in;
    RefPtr<Core::IODevice> m_out;
    RefPtr<Core::AbstractNotifier> m_in_notifier;
};

}
