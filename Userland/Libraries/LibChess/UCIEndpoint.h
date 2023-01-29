/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibChess/UCICommand.h>
#include <LibCore/IODevice.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>

namespace Chess::UCI {

class Endpoint : public Core::Object {
    C_OBJECT(Endpoint)
public:
    virtual ~Endpoint() override = default;

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

    void send_command(Command const&);

    virtual void event(Core::Event&) override;

    Core::IODevice& in() { return *m_in; }
    Core::IODevice& out() { return *m_out; }

    void set_in(RefPtr<Core::IODevice> in)
    {
        m_in = in;
        set_in_notifier();
    }
    void set_out(RefPtr<Core::IODevice> out) { m_out = out; }

protected:
    Endpoint() = default;
    Endpoint(NonnullRefPtr<Core::IODevice> in, NonnullRefPtr<Core::IODevice> out);

private:
    void set_in_notifier();
    NonnullOwnPtr<Command> read_command();

    RefPtr<Core::IODevice> m_in;
    RefPtr<Core::IODevice> m_out;
    RefPtr<Core::Notifier> m_in_notifier;
};

}
