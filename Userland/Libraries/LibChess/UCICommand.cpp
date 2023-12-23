/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
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
    return "uci\n"_string;
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

    auto name_string = TRY(String::from_utf8(name.string_view().trim_whitespace()));
    auto value_string = TRY(String::from_utf8(value.string_view().trim_whitespace()));
    return adopt_nonnull_own_or_enomem(new (nothrow) SetOptionCommand(name_string, value_string));
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
        TRY(builder.try_append(TRY(move.to_long_algebraic())));
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
            go_command->wtime = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "btime") {
            VERIFY(i++ < tokens.size());
            go_command->btime = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "winc") {
            VERIFY(i++ < tokens.size());
            go_command->winc = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "binc") {
            VERIFY(i++ < tokens.size());
            go_command->binc = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "movestogo") {
            VERIFY(i++ < tokens.size());
            go_command->movestogo = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "depth") {
            VERIFY(i++ < tokens.size());
            go_command->depth = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "nodes") {
            VERIFY(i++ < tokens.size());
            go_command->nodes = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "mate") {
            VERIFY(i++ < tokens.size());
            go_command->mate = tokens[i].to_number<int>().value();
        } else if (tokens[i] == "movetime") {
            VERIFY(i++ < tokens.size());
            go_command->movetime = tokens[i].to_number<int>().value();
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
            TRY(builder.try_append(TRY(move.to_long_algebraic())));
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
    return "stop\n"_string;
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

    auto value_string = TRY(value.to_string());
    if (tokens[1] == "name") {
        return adopt_nonnull_own_or_enomem(new (nothrow) IdCommand(Type::Name, value_string));
    } else if (tokens[1] == "author") {
        return adopt_nonnull_own_or_enomem(new (nothrow) IdCommand(Type::Author, value_string));
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
    return "uciok\n"_string;
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
    VERIFY(tokens.size() == 2 || tokens.size() == 4);
    auto best_move = Move(tokens[1]);
    Optional<Move> move_to_ponder;
    if (tokens.size() == 4) {
        VERIFY(tokens[2] == "ponder");
        move_to_ponder = Move(tokens[3]);
    }

    return adopt_nonnull_own_or_enomem(new (nothrow) BestMoveCommand(best_move, move_to_ponder));
}

ErrorOr<String> BestMoveCommand::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("bestmove "sv));
    TRY(builder.try_append(TRY(move().to_long_algebraic())));
    if (move_to_ponder().has_value()) {
        TRY(builder.try_append(" ponder "sv));
        TRY(builder.try_append(TRY(move_to_ponder()->to_long_algebraic())));
    }
    TRY(builder.try_append('\n'));
    return builder.to_string();
}

ErrorOr<NonnullOwnPtr<InfoCommand>> InfoCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "info");

    auto info_command = TRY(try_make<InfoCommand>());

    auto parse_integer_token = [](StringView value_token) -> ErrorOr<int> {
        auto value_as_integer = value_token.to_number<int>();
        if (!value_as_integer.has_value())
            return Error::from_string_literal("Expected integer token");

        return value_as_integer.release_value();
    };

    auto parse_line = [](auto const& move_tokens) -> ErrorOr<Vector<Chess::Move>> {
        Vector<Chess::Move> moves;
        TRY(moves.try_ensure_capacity(move_tokens.size()));
        for (auto move_token : move_tokens)
            moves.unchecked_append({ move_token });

        return moves;
    };

    size_t i = 1;
    while (i < tokens.size()) {
        auto name = tokens[i++];
        if (name == "depth"sv) {
            info_command->m_depth = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "seldepth"sv) {
            info_command->m_seldepth = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "time"sv) {
            info_command->m_time = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "nodes"sv) {
            info_command->m_nodes = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "multipv"sv) {
            info_command->m_multipv = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "score"sv) {
            auto score_type_string = tokens[i++];
            ScoreType score_type;
            if (score_type_string == "cp"sv) {
                score_type = ScoreType::Centipawns;
            } else if (score_type_string == "mate"sv) {
                score_type = ScoreType::Mate;
            } else {
                return Error::from_string_literal("Invalid score type");
            }
            auto score_value = TRY(parse_integer_token(tokens[i++]));
            auto maybe_score_bound_string = tokens[i];
            auto score_bound = ScoreBound::None;
            if (maybe_score_bound_string == "upperbound"sv)
                score_bound = ScoreBound::Upper;
            else if (maybe_score_bound_string == "lowerbound"sv)
                score_bound = ScoreBound::Lower;

            if (score_bound != ScoreBound::None)
                i++;

            info_command->m_score = Score { score_type, score_value, score_bound };
        } else if (name == "currmove"sv) {
            info_command->m_currmove = Chess::Move { tokens[i++] };
        } else if (name == "currmovenumber"sv) {
            info_command->m_currmovenumber = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "hashfull"sv) {
            info_command->m_hashfull = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "nps"sv) {
            info_command->m_nps = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "tbhits"sv) {
            info_command->m_tbhits = TRY(parse_integer_token(tokens[i++]));
        } else if (name == "cpuload"sv) {
            info_command->m_cpuload = TRY(parse_integer_token(tokens[i++]));
        }
        // We assume the info types: pv, string, refutation, and currline, are the final info type in a command.
        else if (name == "pv"sv) {
            info_command->m_pv = TRY(parse_line(tokens.span().slice(i)));
            break;
        } else if (name == "string"sv) {
            info_command->m_string = TRY(String::join(' ', tokens.span().slice(i)));
            break;
        } else if (name == "refutation"sv) {
            info_command->m_refutation = TRY(parse_line(tokens.span().slice(i)));
            break;
        } else if (name == "currline"sv) {
            info_command->m_currline = TRY(parse_line(tokens.span().slice(i)));
            break;
        } else {
            return Error::from_string_literal("Unknown info type");
        }
    }

    return info_command;
}

ErrorOr<String> InfoCommand::to_string() const
{
    StringBuilder builder;

    auto append_moves = [&](Vector<Chess::Move> const& moves) -> ErrorOr<void> {
        bool first = true;
        for (auto const& move : moves) {
            if (!first)
                TRY(builder.try_append(' '));

            first = false;
            TRY(builder.try_append(TRY(move.to_long_algebraic())));
        }
        return {};
    };

    TRY(builder.try_append("info"sv));
    if (m_depth.has_value())
        TRY(builder.try_appendff(" depth {}", m_depth.value()));
    if (m_seldepth.has_value())
        TRY(builder.try_appendff(" seldepth {}", m_seldepth.value()));
    if (m_time.has_value())
        TRY(builder.try_appendff(" time {}", m_time.value()));
    if (m_nodes.has_value())
        TRY(builder.try_appendff(" nodes {}", m_nodes.value()));
    if (m_multipv.has_value())
        TRY(builder.try_appendff(" multipv {}", m_multipv.value()));
    if (m_score.has_value()) {
        TRY(builder.try_append(" score"sv));
        switch (m_score->type) {
        case ScoreType::Centipawns:
            TRY(builder.try_append(" cp"sv));
            break;
        case ScoreType::Mate:
            TRY(builder.try_append(" mate"sv));
            break;
        }

        TRY(builder.try_appendff(" {}", m_score->value));

        switch (m_score->bound) {
        case ScoreBound::None:
            break;
        case ScoreBound::Lower:
            TRY(builder.try_append(" lowerbound"sv));
            break;
        case ScoreBound::Upper:
            TRY(builder.try_append(" upperbound"sv));
            break;
        }
    }
    if (m_currmove.has_value())
        TRY(builder.try_appendff(" currmove {}", TRY(m_currmove->to_long_algebraic())));
    if (m_currmovenumber.has_value())
        TRY(builder.try_appendff(" currmovenumber {}", m_currmovenumber.value()));
    if (m_hashfull.has_value())
        TRY(builder.try_appendff(" hashfull {}", m_hashfull.value()));
    if (m_nps.has_value())
        TRY(builder.try_appendff(" nps {}", m_nps.value()));
    if (m_tbhits.has_value())
        TRY(builder.try_appendff(" tbhits {}", m_tbhits.value()));
    if (m_cpuload.has_value())
        TRY(builder.try_appendff(" cpuload {}", m_cpuload.value()));
    if (m_string.has_value())
        TRY(builder.try_appendff(" string {}", m_string.value()));
    if (m_pv.has_value()) {
        TRY(builder.try_append(" pv "sv));
        TRY(append_moves(m_pv.value()));
    }
    if (m_refutation.has_value()) {
        TRY(builder.try_append(" refutation "sv));
        TRY(append_moves(m_refutation.value()));
    }
    if (m_currline.has_value()) {
        TRY(builder.try_append(" currline "sv));
        TRY(append_moves(m_currline.value()));
    }

    return builder.to_string();
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
    return "quit\n"_string;
}

ErrorOr<NonnullOwnPtr<UCINewGameCommand>> UCINewGameCommand::from_string(StringView command)
{
    auto tokens = command.split_view(' ');
    VERIFY(tokens[0] == "ucinewgame");
    VERIFY(tokens.size() == 1);
    return adopt_nonnull_own_or_enomem(new (nothrow) UCINewGameCommand);
}

ErrorOr<String> UCINewGameCommand::to_string() const
{
    return "ucinewgame\n"_string;
}

}
