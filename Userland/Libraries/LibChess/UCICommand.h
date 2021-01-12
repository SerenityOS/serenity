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

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibChess/Chess.h>
#include <LibCore/Event.h>

namespace Chess::UCI {

class Command : public Core::Event {
public:
    enum Type {
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

    explicit Command(Type type)
        : Core::Event(type)
    {
    }

    virtual String to_string() const = 0;

    virtual ~Command() { }
};

class UCICommand : public Command {
public:
    explicit UCICommand()
        : Command(Command::Type::UCI)
    {
    }

    static UCICommand from_string(const StringView& command);

    virtual String to_string() const;
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

    static DebugCommand from_string(const StringView& command);

    virtual String to_string() const;

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

    static IsReadyCommand from_string(const StringView& command);

    virtual String to_string() const;
};

class SetOptionCommand : public Command {
public:
    explicit SetOptionCommand(const StringView& name, Optional<String> value = {})
        : Command(Command::Type::SetOption)
        , m_name(name)
        , m_value(value)
    {
    }

    static SetOptionCommand from_string(const StringView& command);

    virtual String to_string() const;

    const String& name() const { return m_name; }
    const Optional<String>& value() const { return m_value; }

private:
    String m_name;
    Optional<String> m_value;
};

class PositionCommand : public Command {
public:
    explicit PositionCommand(const Optional<String>& fen, const Vector<Chess::Move>& moves)
        : Command(Command::Type::Position)
        , m_fen(fen)
        , m_moves(moves)
    {
    }

    static PositionCommand from_string(const StringView& command);

    virtual String to_string() const;

    const Optional<String>& fen() const { return m_fen; }
    const Vector<Chess::Move>& moves() const { return m_moves; }

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

    static GoCommand from_string(const StringView& command);

    virtual String to_string() const;

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

    static StopCommand from_string(const StringView& command);

    virtual String to_string() const;
};

class IdCommand : public Command {
public:
    enum class Type {
        Name,
        Author,
    };

    explicit IdCommand(Type field_type, const StringView& value)
        : Command(Command::Type::Id)
        , m_field_type(field_type)
        , m_value(value)
    {
    }

    static IdCommand from_string(const StringView& command);

    virtual String to_string() const;

    Type field_type() const { return m_field_type; }
    const String& value() const { return m_value; }

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

    static UCIOkCommand from_string(const StringView& command);

    virtual String to_string() const;
};

class ReadyOkCommand : public Command {
public:
    explicit ReadyOkCommand()
        : Command(Command::Type::ReadyOk)
    {
    }

    static ReadyOkCommand from_string(const StringView& command);

    virtual String to_string() const;
};

class BestMoveCommand : public Command {
public:
    explicit BestMoveCommand(const Chess::Move& move)
        : Command(Command::Type::BestMove)
        , m_move(move)
    {
    }

    static BestMoveCommand from_string(const StringView& command);

    virtual String to_string() const;

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

    static InfoCommand from_string(const StringView& command);

    virtual String to_string() const;

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

}
