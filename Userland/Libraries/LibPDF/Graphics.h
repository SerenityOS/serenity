/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

#define ENUMERATE_GRAPHICS_COMMANDS(V)                                 \
    V(SaveState, save_state, q)                                        \
    V(RestoreState, restore_state, Q)                                  \
    V(ConcatenateMatrix, concatenate_matrix, cm)                       \
    V(SetLineWidth, set_line_width, w)                                 \
    V(SetLineCap, set_line_cap, J)                                     \
    V(SetLineJoin, set_line_join, j)                                   \
    V(SetMiterLimit, set_miter_limit, M)                               \
    V(SetDashPattern, set_dash_pattern, d)                             \
    V(PathBegin, path_begin, m)                                        \
    V(PathEnd, path_end, n)                                            \
    V(PathLine, path_line, l)                                          \
    V(PathClose, path_close, h)                                        \
    V(PathAppendRect, path_append_rect, re)                            \
    V(PathStroke, path_stroke, S)                                      \
    V(PathCloseAndStroke, path_close_and_stroke, s)                    \
    V(PathFillNonZero, path_fill_nonzero, f)                           \
    V(PathFillNonZeroDeprecated, path_fill_nonzero_deprecated, F)      \
    V(PathFillEvenOdd, path_fill_evenodd, f*)                          \
    V(PathFillStrokeNonZero, path_fill_stroke_nonzero, B)              \
    V(PathFillStrokeEvenOdd, path_fill_stroke_evenodd, B*)             \
    V(PathCloseFillStrokeNonZero, path_close_fill_stroke_nonzero, b)   \
    V(PathCloseFillStrokeEvenOdd, path_close_fill_stroke_evenodd, b*)  \
    V(TextSetCharSpace, text_set_char_space, Tc)                       \
    V(TextSetWordSpace, text_set_word_space, Tw)                       \
    V(TextSetHorizontalScale, text_set_horizontal_scale, Tz)           \
    V(TextSetLeading, text_set_leading, TL)                            \
    V(TextSetFont, text_set_font, Tf)                                  \
    V(TextSetRenderingMode, text_set_rendering_mode, Tr)               \
    V(TextSetRise, text_set_rise, Ts)                                  \
    V(TextBegin, text_begin, BT)                                       \
    V(TextEnd, text_end, ET)                                           \
    V(TextNextLineOffset, text_next_line_offset, Td)                   \
    V(TextNextLineAndSetLeading, text_next_line_and_set_leading, TD)   \
    V(TextSetMatrixAndLineMatrix, text_set_matrix_and_line_matrix, Tm) \
    V(TextNextLine, text_next_line, T*)                                \
    V(TextShowString, text_show_string, Tj)

namespace PDF {

enum class Command {
#define V(name, snake_name, symbol) name,
    ENUMERATE_GRAPHICS_COMMANDS(V)
#undef V
        TextNextLineShowString,
};

class GraphicsCommand {
public:
    static Command command_from_string(const String& string)
    {
#define V(name, snake_name, symbol) \
    if (string == #symbol)          \
        return Command::name;
        ENUMERATE_GRAPHICS_COMMANDS(V)
#undef V

        if (string == "'")
            return Command::TextNextLineShowString;

        dbgln("unsupported graphics command {}", string);
        VERIFY_NOT_REACHED();
    }

    static const char* command_name(Command command)
    {
#define V(name, snake_name, symbol) \
    if (command == Command::name)   \
        return #name;
        ENUMERATE_GRAPHICS_COMMANDS(V)
#undef V

        if (command == Command::TextNextLineShowString)
            return "TextNextLineShowString";

        VERIFY_NOT_REACHED();
    }

    static const char* command_symbol(Command command)
    {
#define V(name, snake_name, symbol) \
    if (command == Command::name)   \
        return #symbol;
        ENUMERATE_GRAPHICS_COMMANDS(V)
#undef V

        if (command == Command::TextNextLineShowString)
            return "'";

        VERIFY_NOT_REACHED();
    }

    GraphicsCommand(Command command, Vector<Value> arguments)
        : m_command(command)
        , m_arguments(move(arguments))
    {
    }

    ALWAYS_INLINE Command command() const { return m_command; }
    ALWAYS_INLINE const Vector<Value>& arguments() const { return m_arguments; }

private:
    Command m_command;
    Vector<Value> m_arguments;
};

}

namespace AK {

template<>
struct Formatter<PDF::GraphicsCommand> : Formatter<StringView> {
    void format(FormatBuilder& format_builder, const PDF::GraphicsCommand& command)
    {
        StringBuilder builder;
        builder.appendff("{} [ ", PDF::GraphicsCommand::command_name(command.command()));
        for (auto& argument : command.arguments())
            builder.appendff(" {}", argument);
        builder.append(" ]");
        Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

}
