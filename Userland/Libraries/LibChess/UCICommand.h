/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibChess/Chess.h>
#include <LibCore/Event.h>

namespace Chess::UCI {

class Command : public Core::Event {
public:
    enum class Type {
        // GUI to engine commands.
        UCI = 12000,
        Debug,
        IsReady,
        SetOption,
        Register,
        UCINewGame,
        Position,
        Go,
        Stop,
        PonderHit,
        Quit,
        // Engine to GUI commands.
        Id,
        UCIOk,
        ReadyOk,
        BestMove,
        CopyProtection,
        Registration,
        Info,
        Option,
    };
    virtual ErrorOr<String> to_string() const = 0;

    virtual ~Command() = default;

protected:
    explicit Command(Type type)
        : Core::Event(to_underlying(type))
    {
    }
};

class UCICommand : public Command {
public:
    explicit UCICommand()
        : Command(Command::Type::UCI)
    {
    }

    static ErrorOr<NonnullOwnPtr<UCICommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;
};

class DebugCommand : public Command {
public:
    enum class Flag {
        On,
        Off
    };

    explicit DebugCommand(Flag flag)
        : Command(Command::Type::Debug)
        , m_flag(flag)
    {
    }

    static ErrorOr<NonnullOwnPtr<DebugCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    Flag flag() const { return m_flag; }

private:
    Flag m_flag;
};

class IsReadyCommand : public Command {
public:
    explicit IsReadyCommand()
        : Command(Command::Type::IsReady)
    {
    }

    static ErrorOr<NonnullOwnPtr<IsReadyCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;
};

class SetOptionCommand : public Command {
public:
    explicit SetOptionCommand(String name, Optional<String> value = {})
        : Command(Command::Type::SetOption)
        , m_name(name)
        , m_value(value)
    {
    }

    static ErrorOr<NonnullOwnPtr<SetOptionCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    String const& name() const { return m_name; }
    Optional<String> const& value() const { return m_value; }

private:
    String m_name;
    Optional<String> m_value;
};

class PositionCommand : public Command {
public:
    explicit PositionCommand(Optional<String> const& fen, Vector<Chess::Move> const& moves)
        : Command(Command::Type::Position)
        , m_fen(fen)
        , m_moves(moves)
    {
    }

    static ErrorOr<NonnullOwnPtr<PositionCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    Optional<String> const& fen() const { return m_fen; }
    Vector<Chess::Move> const& moves() const { return m_moves; }

private:
    Optional<String> m_fen;
    Vector<Chess::Move> m_moves;
};

class GoCommand : public Command {
public:
    explicit GoCommand()
        : Command(Command::Type::Go)
    {
    }

    static ErrorOr<NonnullOwnPtr<GoCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    Optional<Vector<Chess::Move>> searchmoves;
    bool ponder { false };
    Optional<int> wtime;
    Optional<int> btime;
    Optional<int> winc;
    Optional<int> binc;
    Optional<int> movestogo;
    Optional<int> depth;
    Optional<int> nodes;
    Optional<int> mate;
    Optional<int> movetime;
    bool infinite { false };
};

class StopCommand : public Command {
public:
    explicit StopCommand()
        : Command(Command::Type::Stop)
    {
    }

    static ErrorOr<NonnullOwnPtr<StopCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;
};

class IdCommand : public Command {
public:
    enum class Type {
        Name,
        Author,
    };

    explicit IdCommand(Type field_type, String value)
        : Command(Command::Type::Id)
        , m_field_type(field_type)
        , m_value(value)
    {
    }

    static ErrorOr<NonnullOwnPtr<IdCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    Type field_type() const { return m_field_type; }
    String const& value() const { return m_value; }

private:
    Type m_field_type;
    String m_value;
};

class UCIOkCommand : public Command {
public:
    explicit UCIOkCommand()
        : Command(Command::Type::UCIOk)
    {
    }

    static ErrorOr<NonnullOwnPtr<UCIOkCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;
};

class ReadyOkCommand : public Command {
public:
    explicit ReadyOkCommand()
        : Command(Command::Type::ReadyOk)
    {
    }

    static ErrorOr<NonnullOwnPtr<ReadyOkCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;
};

class BestMoveCommand : public Command {
public:
    explicit BestMoveCommand(Chess::Move const& move)
        : Command(Command::Type::BestMove)
        , m_move(move)
    {
    }

    static ErrorOr<NonnullOwnPtr<BestMoveCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    Chess::Move move() const { return m_move; }

private:
    Chess::Move m_move;
};

class InfoCommand : public Command {
public:
    explicit InfoCommand()
        : Command(Command::Type::BestMove)
    {
    }

    static ErrorOr<NonnullOwnPtr<InfoCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;

    Optional<int> depth;
    Optional<int> seldepth;
    Optional<int> time;
    Optional<int> nodes;
    Optional<Vector<Chess::Move>> pv;
    // FIXME: Add multipv.
    Optional<int> score_cp;
    Optional<int> score_mate;
    // FIXME: Add score bounds.
    Optional<Chess::Move> currmove;
    Optional<int> currmove_number;
    // FIXME: Add additional fields.
};

class QuitCommand : public Command {
public:
    explicit QuitCommand()
        : Command(Command::Type::Quit)
    {
    }

    static ErrorOr<NonnullOwnPtr<QuitCommand>> from_string(StringView command);

    virtual ErrorOr<String> to_string() const override;
};

}
