/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UCIEndpoint.h"
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <LibCore/EventLoop.h>

namespace Chess::UCI {

void Endpoint::send_command(Command const& command)
{
    auto command_string = command.to_string().release_value_but_fixme_should_propagate_errors();
    dbgln_if(UCI_DEBUG, "{} Sent UCI Command: {}", class_name(), command_string);
    m_out->write_until_depleted(command_string.bytes()).release_value_but_fixme_should_propagate_errors();
}

void Endpoint::event(Core::Event& event)
{
    switch (static_cast<Command::Type>(event.type())) {
    case Command::Type::UCI:
        return handle_uci();
    case Command::Type::Debug:
        return handle_debug(static_cast<DebugCommand const&>(event));
    case Command::Type::IsReady:
        return handle_uci();
    case Command::Type::SetOption:
        return handle_setoption(static_cast<SetOptionCommand const&>(event));
    case Command::Type::Position:
        return handle_position(static_cast<PositionCommand const&>(event));
    case Command::Type::Go:
        return handle_go(static_cast<GoCommand const&>(event));
    case Command::Type::Stop:
        return handle_stop();
    case Command::Type::Id:
        return handle_id(static_cast<IdCommand const&>(event));
    case Command::Type::UCIOk:
        return handle_uciok();
    case Command::Type::ReadyOk:
        return handle_readyok();
    case Command::Type::BestMove:
        return handle_bestmove(static_cast<BestMoveCommand const&>(event));
    case Command::Type::Info:
        return handle_info(static_cast<InfoCommand const&>(event));
    case Command::Type::Quit:
        return handle_quit();
    case Command::Type::UCINewGame:
        return handle_ucinewgame();
    default:
        EventReceiver::event(event);
        break;
    }
}

void Endpoint::custom_event(Core::CustomEvent& custom_event)
{
    if (custom_event.custom_type() == EndpointEventType::UnexpectedEof)
        handle_unexpected_eof();
}

void Endpoint::set_in_notifier()
{
    m_in_notifier = Core::Notifier::construct(m_in_fd.value(), Core::Notifier::Type::Read);
    m_in_notifier->on_activation = [this] {
        if (!m_in->can_read_line().release_value_but_fixme_should_propagate_errors()) {
            Core::EventLoop::current().post_event(*this, make<Core::CustomEvent>(EndpointEventType::UnexpectedEof));
            m_in_notifier->set_enabled(false);
            return;
        }
        auto buffer = ByteBuffer::create_zeroed(4096).release_value_but_fixme_should_propagate_errors();

        while (m_in->can_read_line().release_value_but_fixme_should_propagate_errors()) {
            auto line = m_in->read_line(buffer).release_value_but_fixme_should_propagate_errors().trim_whitespace();
            if (line.is_empty())
                continue;

            auto maybe_command = read_command(line);
            if (maybe_command.is_error()) {
                dbgln_if(UCI_DEBUG, "{} Error while parsing UCI command: {}, error: {}", class_name(), maybe_command.error(), line);
                if (on_command_read_error)
                    on_command_read_error(move(line), maybe_command.release_error());

                continue;
            }

            Core::EventLoop::current().post_event(*this, maybe_command.release_value());
        }
    };
}

ErrorOr<NonnullOwnPtr<Command>> Endpoint::read_command(StringView line) const
{
    dbgln_if(UCI_DEBUG, "{} Received UCI Command: {}", class_name(), line);

    if (line == "uci") {
        return UCICommand::from_string(line);
    } else if (line.starts_with("debug"sv)) {
        return DebugCommand::from_string(line);
    } else if (line.starts_with("isready"sv)) {
        return IsReadyCommand::from_string(line);
    } else if (line.starts_with("setoption"sv)) {
        return SetOptionCommand::from_string(line);
    } else if (line.starts_with("position"sv)) {
        return PositionCommand::from_string(line);
    } else if (line.starts_with("go"sv)) {
        return GoCommand::from_string(line);
    } else if (line.starts_with("stop"sv)) {
        return StopCommand::from_string(line);
    } else if (line.starts_with("id"sv)) {
        return IdCommand::from_string(line);
    } else if (line.starts_with("uciok"sv)) {
        return UCIOkCommand::from_string(line);
    } else if (line.starts_with("readyok"sv)) {
        return ReadyOkCommand::from_string(line);
    } else if (line.starts_with("bestmove"sv)) {
        return BestMoveCommand::from_string(line);
    } else if (line.starts_with("info"sv)) {
        return InfoCommand::from_string(line);
    } else if (line.starts_with("quit"sv)) {
        return QuitCommand::from_string(line);
    } else if (line.starts_with("ucinewgame"sv)) {
        return UCINewGameCommand::from_string(line);
    }

    return Error::from_string_literal("Unknown command");
}

};
