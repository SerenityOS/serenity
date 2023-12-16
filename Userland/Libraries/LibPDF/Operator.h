/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibPDF/Value.h>

#define ENUMERATE_OPERATORS(V)                                                           \
    V(SaveState, save_state, q)                                                          \
    V(RestoreState, restore_state, Q)                                                    \
    V(ConcatenateMatrix, concatenate_matrix, cm)                                         \
    V(SetLineWidth, set_line_width, w)                                                   \
    V(SetLineCap, set_line_cap, J)                                                       \
    V(SetLineJoin, set_line_join, j)                                                     \
    V(SetMiterLimit, set_miter_limit, M)                                                 \
    V(SetDashPattern, set_dash_pattern, d)                                               \
    V(SetColorRenderingIntent, set_color_rendering_intent, ri)                           \
    V(SetFlatnessTolerance, set_flatness_tolerance, i)                                   \
    V(SetGraphicsStateFromDict, set_graphics_state_from_dict, gs)                        \
    V(PathMove, path_move, m)                                                            \
    V(PathLine, path_line, l)                                                            \
    V(PathCubicBezierCurve, path_cubic_bezier_curve, c)                                  \
    V(PathCubicBezierCurveNoFirstControl, path_cubic_bezier_curve_no_first_control, v)   \
    V(PathCubicBezierCurveNoSecondControl, path_cubic_bezier_curve_no_second_control, y) \
    V(PathClose, path_close, h)                                                          \
    V(PathAppendRect, path_append_rect, re)                                              \
    V(PathStroke, path_stroke, S)                                                        \
    V(PathCloseAndStroke, path_close_and_stroke, s)                                      \
    V(PathFillNonZero, path_fill_nonzero, f)                                             \
    V(PathFillNonZeroDeprecated, path_fill_nonzero_deprecated, F)                        \
    V(PathFillEvenOdd, path_fill_evenodd, f*)                                            \
    V(PathFillStrokeNonZero, path_fill_stroke_nonzero, B)                                \
    V(PathFillStrokeEvenOdd, path_fill_stroke_evenodd, B*)                               \
    V(PathCloseFillStrokeNonZero, path_close_fill_stroke_nonzero, b)                     \
    V(PathCloseFillStrokeEvenOdd, path_close_fill_stroke_evenodd, b*)                    \
    V(PathEnd, path_end, n)                                                              \
    V(PathIntersectClipNonZero, path_intersect_clip_nonzero, W)                          \
    V(PathIntersectClipEvenOdd, path_intersect_clip_evenodd, W*)                         \
    V(TextBegin, text_begin, BT)                                                         \
    V(TextEnd, text_end, ET)                                                             \
    V(TextSetCharSpace, text_set_char_space, Tc)                                         \
    V(TextSetWordSpace, text_set_word_space, Tw)                                         \
    V(TextSetHorizontalScale, text_set_horizontal_scale, Tz)                             \
    V(TextSetLeading, text_set_leading, TL)                                              \
    V(TextSetFont, text_set_font, Tf)                                                    \
    V(TextSetRenderingMode, text_set_rendering_mode, Tr)                                 \
    V(TextSetRise, text_set_rise, Ts)                                                    \
    V(TextNextLineOffset, text_next_line_offset, Td)                                     \
    V(TextNextLineAndSetLeading, text_next_line_and_set_leading, TD)                     \
    V(TextSetMatrixAndLineMatrix, text_set_matrix_and_line_matrix, Tm)                   \
    V(TextNextLine, text_next_line, T*)                                                  \
    V(TextShowString, text_show_string, Tj)                                              \
    V(TextShowStringArray, text_show_string_array, TJ)                                   \
    V(Type3FontSetGlyphWidth, type3_font_set_glyph_width, d0)                            \
    V(Type3FontSetGlyphWidthAndBBox, type3_font_set_glyph_width_and_bbox, d1)            \
    V(SetStrokingSpace, set_stroking_space, CS)                                          \
    V(SetPaintingSpace, set_painting_space, cs)                                          \
    V(SetStrokingColor, set_stroking_color, SC)                                          \
    V(SetStrokingColorExtended, set_stroking_color_extended, SCN)                        \
    V(SetPaintingColor, set_painting_color, sc)                                          \
    V(SetPaintingColorExtended, set_painting_color_extended, scn)                        \
    V(SetStrokingColorAndSpaceToGray, set_stroking_color_and_space_to_gray, G)           \
    V(SetPaintingColorAndSpaceToGray, set_painting_color_and_space_to_gray, g)           \
    V(SetStrokingColorAndSpaceToRGB, set_stroking_color_and_space_to_rgb, RG)            \
    V(SetPaintingColorAndSpaceToRGB, set_painting_color_and_space_to_rgb, rg)            \
    V(SetStrokingColorAndSpaceToCMYK, set_stroking_color_and_space_to_cmyk, K)           \
    V(SetPaintingColorAndSpaceToCMYK, set_painting_color_and_space_to_cmyk, k)           \
    V(Shade, shade, sh)                                                                  \
    V(InlineImageBegin, inline_image_begin, BI)                                          \
    V(InlineImageBeginData, inline_image_begin_data, ID)                                 \
    V(InlineImageEnd, inline_image_end, EI)                                              \
    V(PaintXObject, paint_xobject, Do)                                                   \
    V(MarkedContentPoint, marked_content_point, MP)                                      \
    V(MarkedContentDesignate, marked_content_designate, DP)                              \
    V(MarkedContentBegin, marked_content_begin, BMC)                                     \
    V(MarkedContentBeginWithPropertyList, marked_content_begin_with_property_list, BDC)  \
    V(MarkedContentEnd, marked_content_end, EMC)                                         \
    V(CompatibilityBegin, compatibility_begin, BX)                                       \
    V(CompatibilityEnd, compatibility_end, EX)

namespace PDF {

enum class OperatorType {
#define V(name, snake_name, symbol) name,
    ENUMERATE_OPERATORS(V)
#undef V
        TextNextLineShowString,
    TextNextLineShowStringSetSpacing,
};

class Operator {
public:
    static OperatorType operator_type_from_symbol(StringView symbol_string)
    {
#define V(name, snake_name, symbol) \
    if (symbol_string == #symbol)   \
        return OperatorType::name;
        ENUMERATE_OPERATORS(V)
#undef V

        if (symbol_string == "'")
            return OperatorType::TextNextLineShowString;
        if (symbol_string == "\"")
            return OperatorType::TextNextLineShowStringSetSpacing;

        dbgln("unsupported graphics symbol {}", symbol_string);
        VERIFY_NOT_REACHED();
    }

    static char const* operator_name(OperatorType operator_type)
    {
#define V(name, snake_name, symbol)          \
    if (operator_type == OperatorType::name) \
        return #name;
        ENUMERATE_OPERATORS(V)
#undef V

        if (operator_type == OperatorType::TextNextLineShowString)
            return "TextNextLineShowString";
        if (operator_type == OperatorType::TextNextLineShowStringSetSpacing)
            return "TextNextLineShowStringSetSpacing";

        VERIFY_NOT_REACHED();
    }

    static char const* operator_symbol(OperatorType operator_type)
    {
#define V(name, snake_name, symbol)          \
    if (operator_type == OperatorType::name) \
        return #symbol;
        ENUMERATE_OPERATORS(V)
#undef V

        if (operator_type == OperatorType::TextNextLineShowString)
            return "'";
        if (operator_type == OperatorType::TextNextLineShowStringSetSpacing)
            return "\"";

        VERIFY_NOT_REACHED();
    }

    Operator(OperatorType operator_type, Vector<Value> arguments)
        : m_operator_type(operator_type)
        , m_arguments(move(arguments))
    {
    }

    [[nodiscard]] ALWAYS_INLINE OperatorType type() const { return m_operator_type; }
    [[nodiscard]] ALWAYS_INLINE Vector<Value> const& arguments() const { return m_arguments; }

private:
    OperatorType m_operator_type;
    Vector<Value> m_arguments;
};

}

namespace AK {

template<>
struct Formatter<PDF::Operator> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::Operator const& op)
    {
        StringBuilder builder;
        builder.appendff("{} ({})",
            PDF::Operator::operator_name(op.type()),
            PDF::Operator::operator_symbol(op.type()));

        if (!op.arguments().is_empty()) {
            builder.append(" ["sv);
            for (auto& argument : op.arguments())
                builder.appendff(" {}", argument);
            builder.append(" ]"sv);
        }

        return Formatter<StringView>::format(format_builder, builder.to_byte_string());
    }
};

}
