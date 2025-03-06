/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/GenericLexer.h>
#include <AK/HashTable.h>
#include <AK/OwnPtr.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <ctype.h>

struct Range {
    int begin;
    int end;
};

struct StateTransition {
    Optional<ByteString> new_state;
    Optional<ByteString> action;
};

struct MatchedAction {
    Range range;
    StateTransition action;
};

struct State {
    ByteString name;
    Vector<MatchedAction> actions;
    Optional<ByteString> entry_action;
    Optional<ByteString> exit_action;
};

struct StateMachine {
    ByteString name;
    ByteString initial_state;
    Vector<State> states;
    Optional<State> anywhere;
    Optional<ByteString> namespaces;
};

static OwnPtr<StateMachine>
parse_state_machine(StringView input)
{
    auto state_machine = make<StateMachine>();
    GenericLexer lexer(input);

    auto consume_whitespace = [&] {
        bool consumed = true;
        while (consumed) {
            consumed = lexer.consume_while(isspace).length() > 0;
            if (lexer.consume_specific("//"sv)) {
                lexer.consume_line();
                consumed = true;
            }
        }
    };

    auto consume_identifier = [&] {
        consume_whitespace();
        return lexer.consume_while([](char c) { return isalnum(c) || c == '_'; });
    };

    auto get_hex_value = [&](char c) {
        if (isdigit(c))
            return c - '0';
        else
            return c - 'a' + 10;
    };

    auto consume_number = [&] {
        int num = 0;
        consume_whitespace();
        if (lexer.consume_specific("0x"sv)) {
            auto hex_digits = lexer.consume_while([](char c) {
                if (isdigit(c)) return true;
            else {
                c = tolower(c);
                return (c >= 'a' && c <= 'f');
              } });
            for (auto c : hex_digits)
                num = 16 * num + get_hex_value(c);
        } else {
            lexer.consume_specific('\'');
            if (lexer.next_is('\\')) {
                num = (int)lexer.consume_escaped_character('\\');
            } else {
                num = lexer.consume_until('\'').to_number<int>().value();
                lexer.ignore();
            }
            lexer.consume_specific('\'');
        }
        return num;
    };

    auto consume_condition = [&] {
        Range condition;
        consume_whitespace();
        if (lexer.consume_specific('[')) {
            consume_whitespace();
            condition.begin = consume_number();
            consume_whitespace();
            lexer.consume_specific(".."sv);
            consume_whitespace();
            condition.end = consume_number();
            consume_whitespace();
            lexer.consume_specific(']');
        } else {
            auto num = consume_number();
            condition.begin = num;
            condition.end = num;
        }
        return condition;
    };

    auto consume_action = [&]() {
        StateTransition action;
        consume_whitespace();
        lexer.consume_specific("=>"sv);
        consume_whitespace();
        lexer.consume_specific('(');
        consume_whitespace();
        if (!lexer.consume_specific("_"sv))
            action.new_state = consume_identifier();
        consume_whitespace();
        lexer.consume_specific(',');
        consume_whitespace();
        if (!lexer.consume_specific("_"sv))
            action.action = consume_identifier();
        consume_whitespace();
        lexer.consume_specific(')');
        return action;
    };

    auto consume_state_description
        = [&] {
              State state;
              consume_whitespace();
              state.name = consume_identifier();
              consume_whitespace();
              consume_whitespace();
              lexer.consume_specific('{');
              for (;;) {
                  consume_whitespace();
                  if (lexer.consume_specific('}')) {
                      break;
                  }
                  if (lexer.consume_specific("@entry"sv)) {
                      consume_whitespace();
                      state.entry_action = consume_identifier();
                  } else if (lexer.consume_specific("@exit"sv)) {
                      consume_whitespace();
                      state.exit_action = consume_identifier();
                  } else if (lexer.next_is('@')) {
                      auto directive = consume_identifier().to_byte_string();
                      fprintf(stderr, "Unimplemented @ directive %s\n", directive.characters());
                      exit(1);
                  } else {
                      MatchedAction matched_action;
                      matched_action.range = consume_condition();
                      matched_action.action = consume_action();
                      state.actions.append(matched_action);
                  }
              }
              return state;
          };

    while (!lexer.is_eof()) {
        consume_whitespace();
        if (lexer.is_eof())
            break;
        if (lexer.consume_specific("@namespace"sv)) {
            consume_whitespace();
            state_machine->namespaces = lexer.consume_while([](char c) { return isalpha(c) || c == ':'; });
        } else if (lexer.consume_specific("@begin"sv)) {
            consume_whitespace();
            state_machine->initial_state = consume_identifier();
        } else if (lexer.consume_specific("@name"sv)) {
            consume_whitespace();
            state_machine->name = consume_identifier();
        } else if (lexer.next_is("@anywhere")) {
            lexer.consume_specific('@');
            state_machine->anywhere = consume_state_description();
        } else if (lexer.consume_specific('@')) {
            auto directive = consume_identifier().to_byte_string();
            fprintf(stderr, "Unimplemented @ directive %s\n", directive.characters());
            exit(1);
        } else {
            auto description = consume_state_description();
            state_machine->states.append(description);
        }
    }

    if (state_machine->initial_state.is_empty()) {
        fprintf(stderr, "Missing @begin directive\n");
        exit(1);
    } else if (state_machine->name.is_empty()) {
        fprintf(stderr, "Missing @name directive\n");
        exit(1);
    }

    if (state_machine->anywhere.has_value()) {
        state_machine->anywhere.value().name = "_Anywhere";
    }
    return state_machine;
}

void output_header(StateMachine const&, SourceGenerator&);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView path;
    StringView output_file = "-"sv;

    Core::ArgsParser parser;
    parser.add_positional_argument(path, "Path to parser description", "input", Core::ArgsParser::Required::Yes);
    parser.add_option(output_file, "Place to write file", "output", 'o', "output-file");
    parser.parse(arguments);

    auto output = TRY(Core::File::open_file_or_standard_stream(output_file, Core::File::OpenMode::Write));

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    auto content = TRY(file->read_until_eof());
    auto state_machine = parse_state_machine(content);

    StringBuilder builder;
    SourceGenerator generator { builder };
    output_header(*state_machine, generator);

    TRY(output->write_until_depleted(generator.as_string_view().bytes()));
    return 0;
}

static HashTable<ByteString> actions(StateMachine const& machine)
{
    HashTable<ByteString> table;

    auto do_state = [&](State const& state) {
        if (state.entry_action.has_value())
            table.set(state.entry_action.value());
        if (state.exit_action.has_value())
            table.set(state.exit_action.value());
        for (auto action : state.actions) {
            if (action.action.action.has_value())
                table.set(action.action.action.value());
        }
    };
    for (auto state : machine.states) {
        do_state(state);
    }
    if (machine.anywhere.has_value())
        do_state(machine.anywhere.value());
    return table;
}

static void generate_lookup_table(StateMachine const& machine, SourceGenerator& generator)
{
    generator.append(R"~~~(
    static constexpr StateTransition STATE_TRANSITION_TABLE[][256] = {
)~~~");

    auto generate_for_state = [&](State const& s) {
        auto table_generator = generator.fork();
        table_generator.set("active_state", s.name);
        table_generator.append("/* @active_state@ */ { ");
        VERIFY(!s.name.is_empty());
        Vector<StateTransition> row;
        for (int i = 0; i < 256; i++)
            row.append({ s.name, "_Ignore" });
        for (auto action : s.actions) {
            for (int range_element = action.range.begin; range_element <= action.range.end; range_element++) {
                row[range_element] = { action.action.new_state, action.action.action };
            }
        }
        for (int i = 0; i < 256; ++i) {
            auto cell_generator = table_generator.fork();
            cell_generator.set("cell_new_state", row[i].new_state.value_or(s.name));
            cell_generator.set("cell_action", row[i].action.value_or("_Ignore"));
            cell_generator.append(" {State::@cell_new_state@, Action::@cell_action@}, ");
        }
        table_generator.append("},\n");
    };
    if (machine.anywhere.has_value()) {
        generate_for_state(machine.anywhere.value());
    }
    for (auto s : machine.states) {
        generate_for_state(s);
    }
    generator.append(R"~~~(
    };
)~~~");
}

void output_header(StateMachine const& machine, SourceGenerator& generator)
{
    generator.set("class_name", machine.name);
    generator.set("initial_state", machine.initial_state);
    generator.set("state_count", ByteString::number(machine.states.size() + 1));

    generator.append(R"~~~(
#pragma once

#include <AK/Function.h>
#include <AK/Platform.h>
#include <AK/Types.h>
                     )~~~");
    if (machine.namespaces.has_value()) {
        generator.set("namespace", machine.namespaces.value());
        generator.append(R"~~~(
namespace @namespace@ {
)~~~");
    }
    generator.append(R"~~~(
class @class_name@ {
public:
    enum class Action : u8 {
        _Ignore,
)~~~");
    for (auto a : actions(machine)) {
        if (a.is_empty())
            continue;
        auto action_generator = generator.fork();
        action_generator.set("action.name", a);
        action_generator.append(R"~~~(
        @action.name@,
    )~~~");
    }

    generator.append(R"~~~(
    }; // end Action

    using Handler = Function<void(Action, u8)>;

    @class_name@(Handler handler)
    : m_handler(move(handler))
    {
    }

    void advance(u8 byte)
    {
        auto next_state = lookup_state_transition(byte);
        bool state_will_change = next_state.new_state != m_state && next_state.new_state != State::_Anywhere;

        // only run exit directive if state is being changed
        if (state_will_change) {
            switch (m_state) {
)~~~");
    for (auto s : machine.states) {
        auto state_generator = generator.fork();
        if (s.exit_action.has_value()) {
            state_generator.set("state_name", s.name);
            state_generator.set("action", s.exit_action.value());
            state_generator.append(R"~~~(
            case State::@state_name@:
                m_handler(Action::@action@, byte);
                break;
)~~~");
        }
    }
    generator.append(R"~~~(
            default:
                break;
            }
        }

        if (next_state.action != Action::_Ignore)
            m_handler(next_state.action, byte);
        m_state = next_state.new_state;

        // only run entry directive if state is being changed
        if (state_will_change)
        {
            switch (next_state.new_state)
            {
)~~~");
    for (auto state : machine.states) {
        auto state_generator = generator.fork();
        if (state.entry_action.has_value()) {
            state_generator.set("state_name", state.name);
            state_generator.set("action", state.entry_action.value());
            state_generator.append(R"~~~(
            case State::@state_name@:
                m_handler(Action::@action@, byte);
                break;
)~~~");
        }
    }
    generator.append(R"~~~(
            default:
                break;
            }
        }
    }

private:
    enum class State : u8 {
        _Anywhere,
)~~~");

    for (auto s : machine.states) {
        auto state_generator = generator.fork();
        state_generator.set("state.name", s.name);
        state_generator.append(R"~~~(
        @state.name@,
)~~~");
    }
    generator.append(R"~~~(
    }; // end State

    struct StateTransition {
        State new_state;
        Action action;
    };

    State m_state { State::@initial_state@ };

    Handler m_handler;

    ALWAYS_INLINE StateTransition lookup_state_transition(u8 byte)
    {
        VERIFY((u8)m_state < @state_count@);
)~~~");
    if (machine.anywhere.has_value()) {
        generator.append(R"~~~(
        auto anywhere_state = STATE_TRANSITION_TABLE[0][byte];
        if (anywhere_state.new_state != State::_Anywhere || anywhere_state.action != Action::_Ignore)
            return anywhere_state;
        else
)~~~");
    }
    generator.append(R"~~~(
            return STATE_TRANSITION_TABLE[(u8)m_state][byte];
    }
)~~~");

    auto table_generator = generator.fork();
    generate_lookup_table(machine, table_generator);
    generator.append(R"~~~(
}; // end @class_name@
)~~~");

    if (machine.namespaces.has_value()) {
        generator.append(R"~~~(
} // end namespace
)~~~");
    }
}
