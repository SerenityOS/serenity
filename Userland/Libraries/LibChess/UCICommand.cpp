/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UCICommand.h"
#include <AK/StringBuilder.h>

namespace Chess::UCI {

ErrorOr<NonnullOwnPtr<UCICommand>> UCICommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "uci");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) UCICommand);
}

ErrorOr<String> UCICommand::to_string() const
{
    return "uci\n"_short_string;
}

ErrorOr<NonnullOwnPtr<DebugCommand>> DebugCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "debug");
    VERIFY(tokens.size() == 2);
    if (tokens[1] == "on")
        return adopt_nonnull_own_or_enomem(new (nothrow) DebugCommand(Flag::On));
    if (tokens[1] == "off")
        return adopt_nonnull_own_or_enomem(new (nothrow) DebugCommand(Flag::Off));

    VERIFY_NOT_REACHED();
}

ErrorOr<String> DebugCommand::to_string() const
{
    if (flag() == Flag::On) {
        return "debug on\n"_string;
    } else {
        return "debug off\n"_string;
    }
}

ErrorOr<NonnullOwnPtr<IsReadyCommand>> IsReadyCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "isready");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) IsReadyCommand);
}

ErrorOr<String> IsReadyCommand::to_string() const
{
    return "isready\n"_string;
}

ErrorOr<NonnullOwnPtr<SetOptionCommand>> SetOptionCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "setoption");
    VERIFY(tokens[1] == "name");
    VERIFY(tokens.size() > 2);

    StringBuilder name;
    StringBuilder value;
    bool in_name = false;
    bool in_value = false;
    for (auto& part : tokens) {
        if (in_name) {
            if (part == "value") {
                in_name = false;
                in_value = true;
                continue;
            }
            TRY(name.try_append(part));
            TRY(name.try_append(' '));
            continue;
        }
        if (in_value) {
            TRY(value.try_append(part));
            TRY(value.try_append(' '));
            continue;
        }
        if (part == "name") {
            in_name = true;
            continue;
        }
    }

    VERIFY(!name.is_empty());

    return adopt_nonnull_own_or_enomem(new (nothrow) SetOptionCommand(
        TRY(String::from_utf8(name.string_view().trim_whitespace())),
        TRY(String::from_utf8(value.string_view().trim_whitespace()))));
}

ErrorOr<String> SetOptionCommand::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("setoption name "sv));
    TRY(builder.try_append(name()));
    if (value().has_value()) {
        TRY(builder.try_append(" value "sv));
        TRY(builder.try_append(value().value()));
    }
    TRY(builder.try_append('\n'));
    return builder.to_string();
}

ErrorOr<NonnullOwnPtr<PositionCommand>> PositionCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens.size() >= 3);
    VERIFY(tokens[0] == "position");
    VERIFY(tokens[2] == "moves");

    Optional<String> fen;
    if (tokens[1] != "startpos")
        fen = TRY(String::from_utf8(tokens[1]));

    Vector<Move> moves;
    for (size_t i = 3; i < tokens.size(); ++i) {
        TRY(moves.try_append(Move(tokens[i])));
    }
    return adopt_nonnull_own_or_enomem(new (nothrow) PositionCommand(move(fen), move(moves)));
}

ErrorOr<String> PositionCommand::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("position "sv));
    if (fen().has_value()) {
        TRY(builder.try_append(fen().value()));
    } else {
        TRY(builder.try_append("startpos "sv));
    }
    TRY(builder.try_append("moves"sv));
    for (auto& move : moves()) {
        TRY(builder.try_append(' '));
        TRY(builder.try_append(move.to_long_algebraic()));
    }
    TRY(builder.try_append('\n'));
    return builder.to_string();
}

ErrorOr<NonnullOwnPtr<GoCommand>> GoCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "go");

    auto go_command = TRY(adopt_nonnull_own_or_enomem(new (nothrow) GoCommand));
    for (size_t i = 1; i < tokens.size(); ++i) {
        if (tokens[i] == "searchmoves") {
            VERIFY_NOT_REACHED();
        } else if (tokens[i] == "ponder") {
            go_command->ponder = true;
        } else if (tokens[i] == "wtime") {
            VERIFY(i++ < tokens.size());
            go_command->wtime = tokens[i].to_int().value();
        } else if (tokens[i] == "btime") {
            VERIFY(i++ < tokens.size());
            go_command->btime = tokens[i].to_int().value();
        } else if (tokens[i] == "winc") {
            VERIFY(i++ < tokens.size());
            go_command->winc = tokens[i].to_int().value();
        } else if (tokens[i] == "binc") {
            VERIFY(i++ < tokens.size());
            go_command->binc = tokens[i].to_int().value();
        } else if (tokens[i] == "movestogo") {
            VERIFY(i++ < tokens.size());
            go_command->movestogo = tokens[i].to_int().value();
        } else if (tokens[i] == "depth") {
            VERIFY(i++ < tokens.size());
            go_command->depth = tokens[i].to_int().value();
        } else if (tokens[i] == "nodes") {
            VERIFY(i++ < tokens.size());
            go_command->nodes = tokens[i].to_int().value();
        } else if (tokens[i] == "mate") {
            VERIFY(i++ < tokens.size());
            go_command->mate = tokens[i].to_int().value();
        } else if (tokens[i] == "movetime") {
            VERIFY(i++ < tokens.size());
            go_command->movetime = tokens[i].to_int().value();
        } else if (tokens[i] == "infinite") {
            go_command->infinite = true;
        }
    }

    return go_command;
}

ErrorOr<String> GoCommand::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("go"sv));

    if (searchmoves.has_value()) {
        TRY(builder.try_append(" searchmoves"sv));
        for (auto& move : searchmoves.value()) {
            TRY(builder.try_append(' '));
            TRY(builder.try_append(move.to_long_algebraic()));
        }
    }

    if (ponder)
        TRY(builder.try_append(" ponder"sv));
    if (wtime.has_value())
        TRY(builder.try_appendff(" wtime {}", wtime.value()));
    if (btime.has_value())
        TRY(builder.try_appendff(" btime {}", btime.value()));
    if (winc.has_value())
        TRY(builder.try_appendff(" winc {}", winc.value()));
    if (binc.has_value())
        TRY(builder.try_appendff(" binc {}", binc.value()));
    if (movestogo.has_value())
        TRY(builder.try_appendff(" movestogo {}", movestogo.value()));
    if (depth.has_value())
        TRY(builder.try_appendff(" depth {}", depth.value()));
    if (nodes.has_value())
        TRY(builder.try_appendff(" nodes {}", nodes.value()));
    if (mate.has_value())
        TRY(builder.try_appendff(" mate {}", mate.value()));
    if (movetime.has_value())
        TRY(builder.try_appendff(" movetime {}", movetime.value()));
    if (infinite)
        TRY(builder.try_append(" infinite"sv));

    TRY(builder.try_append('\n'));
    return builder.to_string();
}

ErrorOr<NonnullOwnPtr<StopCommand>> StopCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "stop");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) StopCommand);
}

ErrorOr<String> StopCommand::to_string() const
{
    return "stop\n"_short_string;
}

ErrorOr<NonnullOwnPtr<IdCommand>> IdCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "id");
    StringBuilder value;
    for (size_t i = 2; i < tokens.size(); ++i) {
        if (i != 2)
            TRY(value.try_append(' '));

        TRY(value.try_append(tokens[i]));
    }

    if (tokens[1] == "name") {
        return adopt_nonnull_own_or_enomem(new (nothrow) IdCommand(Type::Name, TRY(value.to_string())));
    } else if (tokens[1] == "author") {
        return adopt_nonnull_own_or_enomem(new (nothrow) IdCommand(Type::Author, TRY(value.to_string())));
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<String> IdCommand::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("id "sv));
    if (field_type() == Type::Name) {
        TRY(builder.try_append("name "sv));
    } else {
        TRY(builder.try_append("author "sv));
    }
    TRY(builder.try_append(value()));
    TRY(builder.try_append('\n'));
    return builder.to_string();
}

ErrorOr<NonnullOwnPtr<UCIOkCommand>> UCIOkCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "uciok");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) UCIOkCommand);
}

ErrorOr<String> UCIOkCommand::to_string() const
{
    return "uciok\n"_short_string;
}

ErrorOr<NonnullOwnPtr<ReadyOkCommand>> ReadyOkCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "readyok");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) ReadyOkCommand);
}

ErrorOr<String> ReadyOkCommand::to_string() const
{
    return "readyok\n"_string;
}

ErrorOr<NonnullOwnPtr<BestMoveCommand>> BestMoveCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "bestmove");
    VERIFY(tokens.size() == 2);
    return adopt_nonnull_own_or_enomem(new (nothrow) BestMoveCommand(Move(tokens[1])));
}

ErrorOr<String> BestMoveCommand::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("bestmove "sv));
    TRY(builder.try_append(move().to_long_algebraic()));
    TRY(builder.try_append('\n'));
    return builder.to_string();
}

ErrorOr<NonnullOwnPtr<InfoCommand>> InfoCommand::from_string([[maybe_unused]] StringView command)
{
    // FIXME: Implement this.
    VERIFY_NOT_REACHED();
}

ErrorOr<String> InfoCommand::to_string() const
{
    // FIXME: Implement this.
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullOwnPtr<QuitCommand>> QuitCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "quit");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) QuitCommand);
}

ErrorOr<String> QuitCommand::to_string() const
{
    return "quit\n"_short_string;
}

}
