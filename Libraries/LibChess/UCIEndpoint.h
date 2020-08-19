/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    virtual ~Endpoint() override { }

    Endpoint() { }
    Endpoint(NonnullRefPtr<Core::IODevice> in, NonnullRefPtr<Core::IODevice> out);

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

    Core::IODevice& in() { return *m_in; }
    Core::IODevice& out() { return *m_out; }

    void set_in(RefPtr<Core::IODevice> in)
    {
        m_in = in;
        set_in_notifier();
    }
    void set_out(RefPtr<Core::IODevice> out) { m_out = out; }

private:
    void set_in_notifier();
    NonnullOwnPtr<Command> read_command();

    RefPtr<Core::IODevice> m_in;
    RefPtr<Core::IODevice> m_out;
    RefPtr<Core::Notifier> m_in_notifier;
};

}
