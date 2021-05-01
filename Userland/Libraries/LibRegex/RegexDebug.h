/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/StringBuilder.h"
#include "LibRegex/RegexMatcher.h"
#include <AK/Debug.h>

namespace regex {

class RegexDebug {
public:
    RegexDebug(FILE* file = stdout)
        : m_file(file)
    {
    }

    virtual ~RegexDebug() = default;

    template<typename T>
    void print_raw_bytecode(Regex<T>& regex) const
    {
        auto& bytecode = regex.parser_result.bytecode;
        size_t index { 0 };
        for (auto& value : bytecode) {
            fprintf(m_file, "OpCode i=%3lu [0x%02X]\n", index, (u32)value);
            ++index;
        }
    }

    template<typename T>
    void print_bytecode(const Regex<T>& regex) const
    {
        MatchState state;
        auto& bytecode = regex.parser_result.bytecode;

        for (;;) {
            auto* opcode = bytecode.get_opcode(state);
            if (!opcode) {
                dbgln("Wrong opcode... failed!");
                return;
            }

            print_opcode("PrintBytecode", *opcode, state);
            fprintf(m_file, "%s", m_debug_stripline.characters());

            if (is<OpCode_Exit>(*opcode))
                break;

            state.instruction_position += opcode->size();
        }

        fflush(m_file);
    }

    void print_opcode(const String& system, OpCode& opcode, MatchState& state, size_t recursion = 0, bool newline = true) const
    {
        fprintf(m_file, "%-15s | %-5lu | %-9lu | %-35s | %-30s | %-20s%s",
            system.characters(),
            state.instruction_position,
            recursion,
            opcode.to_string().characters(),
            opcode.arguments_string().characters(),
            String::formatted("ip: {:3},   sp: {:3}", state.instruction_position, state.string_position).characters(),
            newline ? "\n" : "");

        if (newline && is<OpCode_Compare>(opcode)) {
            for (auto& line : to<OpCode_Compare>(opcode).variable_arguments_to_string()) {
                fprintf(m_file, "%-15s | %-5s | %-9s | %-35s | %-30s | %-20s%s", "", "", "", "", line.characters(), "", "\n");
            }
        }
    }

    void print_result(const OpCode& opcode, const ByteCode& bytecode, const MatchInput& input, MatchState& state, ExecutionResult result) const
    {
        StringBuilder builder;
        builder.append(execution_result_name(result));
        builder.appendff(", fc: {}, ss: {}", input.fail_counter, input.saved_positions.size());
        if (result == ExecutionResult::Succeeded) {
            builder.appendf(", ip: %lu/%lu, sp: %lu/%lu", state.instruction_position, bytecode.size() - 1, state.string_position, input.view.length() - 1);
        } else if (result == ExecutionResult::Fork_PrioHigh) {
            builder.appendf(", next ip: %lu", state.fork_at_position + opcode.size());
        } else if (result != ExecutionResult::Failed) {
            builder.appendf(", next ip: %lu", state.instruction_position + opcode.size());
        }

        fprintf(m_file, " | %-20s\n", builder.to_string().characters());

        if (is<OpCode_Compare>(opcode)) {
            for (auto& line : to<OpCode_Compare>(opcode).variable_arguments_to_string(input)) {
                fprintf(m_file, "%-15s | %-5s | %-9s | %-35s | %-30s | %-20s%s", "", "", "", "", line.characters(), "", "\n");
            }
        }

        fprintf(m_file, "%s", m_debug_stripline.characters());
    }

    void print_header()
    {
        StringBuilder builder;
        builder.appendf("%-15s | %-5s | %-9s | %-35s | %-30s | %-20s | %-20s\n", "System", "Index", "Recursion", "OpCode", "Arguments", "State", "Result");
        auto length = builder.length();
        for (size_t i = 0; i < length; ++i) {
            builder.append('=');
        }
        auto str = builder.to_string();
        VERIFY(!str.is_empty());

        fprintf(m_file, "%s\n", str.characters());
        fflush(m_file);

        builder.clear();
        for (size_t i = 0; i < length; ++i) {
            builder.append('-');
        }
        builder.append('\n');
        m_debug_stripline = builder.to_string();
    }

private:
    String m_debug_stripline;
    FILE* m_file;
};

}

using regex::RegexDebug;
