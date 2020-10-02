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

#include "UCIEndpoint.h"
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>

// #define UCI_DEBUG

namespace Chess::UCI {

Endpoint::Endpoint(NonnullRefPtr<Core::IODevice> in, NonnullRefPtr<Core::IODevice> out)
    : m_in(in)
    , m_out(out)
    , m_in_notifier(Core::Notifier::construct(in->fd(), Core::Notifier::Read))
{
    set_in_notifier();
}

void Endpoint::send_command(const Command& command)
{
#ifdef UCI_DEBUG
    dbg() << class_name() << " Sent UCI Command: " << String(command.to_string().characters(), Chomp);
#endif
    m_out->write(command.to_string());
}

void Endpoint::event(Core::Event& event)
{
    switch (event.type()) {
    case Command::Type::UCI:
        return handle_uci();
    case Command::Type::Debug:
        return handle_debug(static_cast<const DebugCommand&>(event));
    case Command::Type::IsReady:
        return handle_uci();
    case Command::Type::SetOption:
        return handle_setoption(static_cast<const SetOptionCommand&>(event));
    case Command::Type::Position:
        return handle_position(static_cast<const PositionCommand&>(event));
    case Command::Type::Go:
        return handle_go(static_cast<const GoCommand&>(event));
    case Command::Type::Stop:
        return handle_stop();
    case Command::Type::Id:
        return handle_id(static_cast<const IdCommand&>(event));
    case Command::Type::UCIOk:
        return handle_uciok();
    case Command::Type::ReadyOk:
        return handle_readyok();
    case Command::Type::BestMove:
        return handle_bestmove(static_cast<const BestMoveCommand&>(event));
    case Command::Type::Info:
        return handle_info(static_cast<const InfoCommand&>(event));
    default:
        break;
    }
}

void Endpoint::set_in_notifier()
{
    m_in_notifier = Core::Notifier::construct(m_in->fd(), Core::Notifier::Read);
    m_in_notifier->on_ready_to_read = [this] {
        while (m_in->can_read_line())
            Core::EventLoop::current().post_event(*this, read_command());
    };
}

NonnullOwnPtr<Command> Endpoint::read_command()
{
    String line(ReadonlyBytes(m_in->read_line(4096).bytes()), Chomp);

#ifdef UCI_DEBUG
    dbg() << class_name() << " Received UCI Command: " << line;
#endif

    if (line == "uci") {
        return make<UCICommand>(UCICommand::from_string(line));
    } else if (line.starts_with("debug")) {
        return make<DebugCommand>(DebugCommand::from_string(line));
    } else if (line.starts_with("isready")) {
        return make<IsReadyCommand>(IsReadyCommand::from_string(line));
    } else if (line.starts_with("setoption")) {
        return make<SetOptionCommand>(SetOptionCommand::from_string(line));
    } else if (line.starts_with("position")) {
        return make<PositionCommand>(PositionCommand::from_string(line));
    } else if (line.starts_with("go")) {
        return make<GoCommand>(GoCommand::from_string(line));
    } else if (line.starts_with("stop")) {
        return make<StopCommand>(StopCommand::from_string(line));
    } else if (line.starts_with("id")) {
        return make<IdCommand>(IdCommand::from_string(line));
    } else if (line.starts_with("uciok")) {
        return make<UCIOkCommand>(UCIOkCommand::from_string(line));
    } else if (line.starts_with("readyok")) {
        return make<ReadyOkCommand>(ReadyOkCommand::from_string(line));
    } else if (line.starts_with("bestmove")) {
        return make<BestMoveCommand>(BestMoveCommand::from_string(line));
    } else if (line.starts_with("info")) {
        return make<InfoCommand>(InfoCommand::from_string(line));
    }

    dbg() << "command line: " << line;
    ASSERT_NOT_REACHED();
}

};
