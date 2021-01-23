/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "AK/StringBuilder.h"
#include "LibRegex/RegexMatcher.h"
#include <AK/Debug.h>

#if REGEX_DEBUG

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
    void print_bytecode(Regex<T>& regex) const
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
            String::format("ip: %3lu,   sp: %3lu", state.instruction_position, state.string_position).characters(),
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

        fprintf(m_file, "%s\n", builder.to_string().characters());
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

#endif
