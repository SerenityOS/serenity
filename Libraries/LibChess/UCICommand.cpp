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

#include "UCICommand.h"
#include <AK/StringBuilder.h>

namespace Chess::UCI {

UCICommand UCICommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "uci");
    ASSERT(tokens.size() == 1);
    return UCICommand();
}

String UCICommand::to_string() const
{
    return "uci\n";
}

DebugCommand DebugCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "debug");
    ASSERT(tokens.size() == 2);
    if (tokens[1] == "on")
        return DebugCommand(Flag::On);
    if (tokens[1] == "off")
        return DebugCommand(Flag::On);

    ASSERT_NOT_REACHED();
}

String DebugCommand::to_string() const
{
    if (flag() == Flag::On) {
        return "debug on\n";
    } else {
        return "debug off\n";
    }
}

IsReadyCommand IsReadyCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "isready");
    ASSERT(tokens.size() == 1);
    return IsReadyCommand();
}

String IsReadyCommand::to_string() const
{
    return "isready\n";
}

SetOptionCommand SetOptionCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "setoption");
    ASSERT(tokens[1] == "name");
    if (tokens.size() == 3) {
        return SetOptionCommand(tokens[1]);
    } else if (tokens.size() == 4) {
        ASSERT(tokens[2] == "value");
        return SetOptionCommand(tokens[1], tokens[3]);
    }
    ASSERT_NOT_REACHED();
}

String SetOptionCommand::to_string() const
{
    StringBuilder builder;
    builder.append("setoption name ");
    builder.append(name());
    if (value().has_value()) {
        builder.append(" value ");
        builder.append(value().value());
    }
    builder.append('\n');
    return builder.build();
}

PositionCommand PositionCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens.size() >= 3);
    ASSERT(tokens[0] == "position");
    ASSERT(tokens[2] == "moves");

    Optional<String> fen;
    if (tokens[1] != "startpos")
        fen = tokens[1];

    Vector<Move> moves;
    for (size_t i = 3; i < tokens.size(); ++i) {
        moves.append(Move(tokens[i]));
    }
    return PositionCommand(fen, moves);
}

String PositionCommand::to_string() const
{
    StringBuilder builder;
    builder.append("position ");
    if (fen().has_value()) {
        builder.append(fen().value());
    } else {
        builder.append("startpos ");
    }
    builder.append("moves");
    for (auto& move : moves()) {
        builder.append(' ');
        builder.append(move.to_long_algebraic());
    }
    builder.append('\n');
    return builder.build();
}

GoCommand GoCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "go");

    GoCommand go_command;
    for (size_t i = 1; i < tokens.size(); ++i) {
        if (tokens[i] == "searchmoves") {
            ASSERT_NOT_REACHED();
        } else if (tokens[i] == "ponder") {
            go_command.ponder = true;
        } else if (tokens[i] == "wtime") {
            ASSERT(i++ < tokens.size());
            go_command.wtime = tokens[i].to_int().value();
        } else if (tokens[i] == "btime") {
            ASSERT(i++ < tokens.size());
            go_command.btime = tokens[i].to_int().value();
        } else if (tokens[i] == "winc") {
            ASSERT(i++ < tokens.size());
            go_command.winc = tokens[i].to_int().value();
        } else if (tokens[i] == "binc") {
            ASSERT(i++ < tokens.size());
            go_command.binc = tokens[i].to_int().value();
        } else if (tokens[i] == "movestogo") {
            ASSERT(i++ < tokens.size());
            go_command.movestogo = tokens[i].to_int().value();
        } else if (tokens[i] == "depth") {
            ASSERT(i++ < tokens.size());
            go_command.depth = tokens[i].to_int().value();
        } else if (tokens[i] == "nodes") {
            ASSERT(i++ < tokens.size());
            go_command.nodes = tokens[i].to_int().value();
        } else if (tokens[i] == "mate") {
            ASSERT(i++ < tokens.size());
            go_command.mate = tokens[i].to_int().value();
        } else if (tokens[i] == "movetime") {
            ASSERT(i++ < tokens.size());
            go_command.movetime = tokens[i].to_int().value();
        } else if (tokens[i] == "infinite") {
            go_command.infinite = true;
        }
    }

    return go_command;
}

String GoCommand::to_string() const
{
    StringBuilder builder;
    builder.append("go");

    if (searchmoves.has_value()) {
        builder.append(" searchmoves");
        for (auto& move : searchmoves.value()) {
            builder.append(' ');
            builder.append(move.to_long_algebraic());
        }
    }

    if (ponder)
        builder.append(" ponder");
    if (wtime.has_value())
        builder.appendf(" wtime %i", wtime.value());
    if (btime.has_value())
        builder.appendf(" btime %i", btime.value());
    if (winc.has_value())
        builder.appendf(" winc %i", winc.value());
    if (binc.has_value())
        builder.appendf(" binc %i", binc.value());
    if (movestogo.has_value())
        builder.appendf(" movestogo %i", movestogo.value());
    if (depth.has_value())
        builder.appendf(" depth %i", depth.value());
    if (nodes.has_value())
        builder.appendf(" nodes %i", nodes.value());
    if (mate.has_value())
        builder.appendf(" mate %i", mate.value());
    if (movetime.has_value())
        builder.appendf(" movetime %i", movetime.value());
    if (infinite)
        builder.append(" infinite");

    builder.append('\n');
    return builder.build();
}

StopCommand StopCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "stop");
    ASSERT(tokens.size() == 1);
    return StopCommand();
}

String StopCommand::to_string() const
{
    return "stop\n";
}

IdCommand IdCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "id");
    StringBuilder value;
    for (size_t i = 2; i < tokens.size(); ++i) {
        if (i != 2)
            value.append(' ');

        value.append(tokens[i]);
    }

    if (tokens[1] == "name") {
        return IdCommand(Type::Name, value.build());
    } else if (tokens[1] == "author") {
        return IdCommand(Type::Author, value.build());
    }
    ASSERT_NOT_REACHED();
}

String IdCommand::to_string() const
{
    StringBuilder builder;
    builder.append("id ");
    if (field_type() == Type::Name) {
        builder.append("name ");
    } else {
        builder.append("author ");
    }
    builder.append(value());
    builder.append('\n');
    return builder.build();
}

UCIOkCommand UCIOkCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "uciok");
    ASSERT(tokens.size() == 1);
    return UCIOkCommand();
}

String UCIOkCommand::to_string() const
{
    return "uciok\n";
}

ReadyOkCommand ReadyOkCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "readyok");
    ASSERT(tokens.size() == 1);
    return ReadyOkCommand();
}

String ReadyOkCommand::to_string() const
{
    return "readyok\n";
}

BestMoveCommand BestMoveCommand::from_string(const StringView& command)
{
    auto tokens = command.split_view(' ');
    ASSERT(tokens[0] == "bestmove");
    ASSERT(tokens.size() == 2);
    return BestMoveCommand(Move(tokens[1]));
}

String BestMoveCommand::to_string() const
{
    StringBuilder builder;
    builder.append("bestmove ");
    builder.append(move().to_long_algebraic());
    builder.append('\n');
    return builder.build();
}

InfoCommand InfoCommand::from_string(const StringView& command)
{
    (void)command;
    // FIXME: Implement this.
    ASSERT_NOT_REACHED();
}

String InfoCommand::to_string() const
{
    // FIXME: Implement this.
    ASSERT_NOT_REACHED();
    return "info";
}

}
