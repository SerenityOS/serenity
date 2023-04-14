/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AttributeParser.h"
#include <AK/FloatingPointStringConversions.h>
#include <AK/GenericShorthands.h>
#include <AK/StringBuilder.h>
#include <ctype.h>

namespace Web::SVG {

AttributeParser::AttributeParser(StringView source)
    : m_lexer(source)
{
}

Optional<Vector<Transform>> AttributeParser::parse_transform(StringView input)
{
    AttributeParser parser { input };
    return parser.parse_transform();
}

Vector<PathInstruction> AttributeParser::parse_path_data(StringView input)
{
    AttributeParser parser { input };
    parser.parse_whitespace();
    while (!parser.done())
        parser.parse_drawto();
    if (!parser.m_instructions.is_empty() && parser.m_instructions[0].type != PathInstructionType::Move) {
        // Invalid. "A path data segment (if there is one) must begin with a "moveto" command."
        return {};
    }
    return parser.m_instructions;
}

Optional<float> AttributeParser::parse_coordinate(StringView input)
{
    AttributeParser parser { input };
    parser.parse_whitespace();
    if (parser.match_coordinate()) {
        float result = parser.parse_coordinate();
        parser.parse_whitespace();
        if (parser.done())
            return result;
    }

    return {};
}

Optional<float> AttributeParser::parse_length(StringView input)
{
    AttributeParser parser { input };
    parser.parse_whitespace();
    if (parser.match_coordinate()) {
        float result = parser.parse_length();
        parser.parse_whitespace();
        if (parser.done())
            return result;
    }

    return {};
}

Optional<float> AttributeParser::parse_positive_length(StringView input)
{
    // FIXME: Where this is used, the spec usually (always?) says "A negative value is an error (see Error processing)."
    //        So, implement error processing! Maybe this should return ErrorOr.
    auto result = parse_length(input);
    if (result.has_value() && result.value() < 0)
        result.clear();
    return result;
}

Vector<Gfx::FloatPoint> AttributeParser::parse_points(StringView input)
{
    AttributeParser parser { input };

    parser.parse_whitespace();

    // FIXME: "If an odd number of coordinates is provided, then the element is in error, with the same user agent behavior
    //        as occurs with an incorrectly specified ‘path’ element. In such error cases the user agent will drop the last,
    //        odd coordinate and otherwise render the shape."
    //        The parser currently doesn't notice that there is a missing coordinate, so make it notice!
    auto coordinate_pair_sequence = parser.parse_coordinate_pair_sequence();

    parser.parse_whitespace();
    if (!parser.done())
        return {};

    // FIXME: This is awkward. Can we return Gfx::FloatPoints from some of these parsing methods instead of Vector<float>?
    Vector<Gfx::FloatPoint> points;
    points.ensure_capacity(coordinate_pair_sequence.size());

    for (auto const& pair : coordinate_pair_sequence)
        points.empend(pair[0], pair[1]);

    return points;
}

void AttributeParser::parse_drawto()
{
    if (match('M') || match('m')) {
        parse_moveto();
    } else if (match('Z') || match('z')) {
        parse_closepath();
    } else if (match('L') || match('l')) {
        parse_lineto();
    } else if (match('H') || match('h')) {
        parse_horizontal_lineto();
    } else if (match('V') || match('v')) {
        parse_vertical_lineto();
    } else if (match('C') || match('c')) {
        parse_curveto();
    } else if (match('S') || match('s')) {
        parse_smooth_curveto();
    } else if (match('Q') || match('q')) {
        parse_quadratic_bezier_curveto();
    } else if (match('T') || match('t')) {
        parse_smooth_quadratic_bezier_curveto();
    } else if (match('A') || match('a')) {
        parse_elliptical_arc();
    } else {
        dbgln("AttributeParser::parse_drawto failed to match: '{}'", ch());
        TODO();
    }
}

void AttributeParser::parse_moveto()
{
    bool absolute = consume() == 'M';
    parse_whitespace();
    for (auto& pair : parse_coordinate_pair_sequence())
        m_instructions.append({ PathInstructionType::Move, absolute, pair });
}

void AttributeParser::parse_closepath()
{
    bool absolute = consume() == 'Z';
    parse_whitespace();
    m_instructions.append({ PathInstructionType::ClosePath, absolute, {} });
}

void AttributeParser::parse_lineto()
{
    bool absolute = consume() == 'L';
    parse_whitespace();
    for (auto& pair : parse_coordinate_pair_sequence())
        m_instructions.append({ PathInstructionType::Line, absolute, pair });
}

void AttributeParser::parse_horizontal_lineto()
{
    bool absolute = consume() == 'H';
    parse_whitespace();
    m_instructions.append({ PathInstructionType::HorizontalLine, absolute, parse_coordinate_sequence() });
}

void AttributeParser::parse_vertical_lineto()
{
    bool absolute = consume() == 'V';
    parse_whitespace();
    m_instructions.append({ PathInstructionType::VerticalLine, absolute, parse_coordinate_sequence() });
}

void AttributeParser::parse_curveto()
{
    bool absolute = consume() == 'C';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::Curve, absolute, parse_coordinate_pair_triplet() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void AttributeParser::parse_smooth_curveto()
{
    bool absolute = consume() == 'S';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::SmoothCurve, absolute, parse_coordinate_pair_double() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void AttributeParser::parse_quadratic_bezier_curveto()
{
    bool absolute = consume() == 'Q';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::QuadraticBezierCurve, absolute, parse_coordinate_pair_double() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void AttributeParser::parse_smooth_quadratic_bezier_curveto()
{
    bool absolute = consume() == 'T';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::SmoothQuadraticBezierCurve, absolute, parse_coordinate_pair() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

void AttributeParser::parse_elliptical_arc()
{
    bool absolute = consume() == 'A';
    parse_whitespace();

    while (true) {
        m_instructions.append({ PathInstructionType::EllipticalArc, absolute, parse_elliptical_arg_argument() });
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_coordinate())
            break;
    }
}

float AttributeParser::parse_length()
{
    // https://www.w3.org/TR/SVG11/types.html#DataTypeLength
    return parse_number();
}

float AttributeParser::parse_coordinate()
{
    // https://www.w3.org/TR/SVG11/types.html#DataTypeCoordinate
    // coordinate ::= length
    return parse_length();
}

Vector<float> AttributeParser::parse_coordinate_pair()
{
    Vector<float> coordinates;
    coordinates.append(parse_coordinate());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.append(parse_coordinate());
    return coordinates;
}

Vector<float> AttributeParser::parse_coordinate_sequence()
{
    Vector<float> sequence;
    while (true) {
        sequence.append(parse_coordinate());
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_comma_whitespace() && !match_coordinate())
            break;
    }
    return sequence;
}

Vector<Vector<float>> AttributeParser::parse_coordinate_pair_sequence()
{
    Vector<Vector<float>> sequence;
    while (true) {
        sequence.append(parse_coordinate_pair());
        if (match_comma_whitespace())
            parse_comma_whitespace();
        if (!match_comma_whitespace() && !match_coordinate())
            break;
    }
    return sequence;
}

Vector<float> AttributeParser::parse_coordinate_pair_double()
{
    Vector<float> coordinates;
    coordinates.extend(parse_coordinate_pair());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.extend(parse_coordinate_pair());
    return coordinates;
}

Vector<float> AttributeParser::parse_coordinate_pair_triplet()
{
    Vector<float> coordinates;
    coordinates.extend(parse_coordinate_pair());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.extend(parse_coordinate_pair());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    coordinates.extend(parse_coordinate_pair());
    return coordinates;
}

Vector<float> AttributeParser::parse_elliptical_arg_argument()
{
    Vector<float> numbers;
    numbers.append(parse_nonnegative_number());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_nonnegative_number());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_number());
    parse_comma_whitespace();
    numbers.append(parse_flag());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.append(parse_flag());
    if (match_comma_whitespace())
        parse_comma_whitespace();
    numbers.extend(parse_coordinate_pair());

    return numbers;
}

void AttributeParser::parse_whitespace(bool must_match_once)
{
    bool matched = false;
    while (!done() && match_whitespace()) {
        consume();
        matched = true;
    }

    VERIFY(!must_match_once || matched);
}

void AttributeParser::parse_comma_whitespace()
{
    if (match(',')) {
        consume();
        parse_whitespace();
    } else {
        parse_whitespace(1);
        if (match(','))
            consume();
        parse_whitespace();
    }
}

// https://www.w3.org/TR/SVG11/types.html#DataTypeNumber
float AttributeParser::parse_number()
{
    auto sign = parse_sign();
    return sign * parse_nonnegative_number();
}

// https://www.w3.org/TR/SVG11/paths.html#PathDataBNF
float AttributeParser::parse_nonnegative_number()
{
    // NOTE: The grammar is almost a floating point except we cannot have a sign
    //       at the start. That condition should have been checked by the caller.
    VERIFY(!match('+') && !match('-'));

    auto remaining_source_text = m_lexer.remaining();
    char const* start = remaining_source_text.characters_without_null_termination();

    auto maybe_float = parse_first_floating_point<float>(start, start + remaining_source_text.length());
    VERIFY(maybe_float.parsed_value());
    m_lexer.ignore(maybe_float.end_ptr - start);

    return maybe_float.value;
}

float AttributeParser::parse_flag()
{
    if (!match('0') && !match('1'))
        VERIFY_NOT_REACHED();
    return consume() - '0';
}

int AttributeParser::parse_sign()
{
    if (match('-')) {
        consume();
        return -1;
    }
    if (match('+'))
        consume();
    return 1;
}

// https://drafts.csswg.org/css-transforms/#svg-syntax
Optional<Vector<Transform>> AttributeParser::parse_transform()
{
    // wsp:
    // Either a U+000A LINE FEED, U+000D CARRIAGE RETURN, U+0009 CHARACTER TABULATION, or U+0020 SPACE.
    auto wsp = [](char c) {
        return AK::first_is_one_of(c, '\n', '\r', '\t', '\f', ' ');
    };
    auto consume_whitespace = [&] {
        m_lexer.consume_while(wsp);
    };

    auto consume_comma_whitespace = [&] {
        consume_whitespace();
        m_lexer.consume_specific(',');
        consume_whitespace();
    };

    // FIXME: AttributeParser currently does not handle invalid parses in most cases (e.g. parse_number()) and just crashes.
    auto parse_optional_number = [&](float default_value = 0.0f) {
        consume_comma_whitespace();
        if (m_lexer.next_is(isdigit))
            return parse_number();
        return default_value;
    };

    auto parse_function = [&](auto body) -> Optional<Transform> {
        consume_whitespace();
        if (!m_lexer.consume_specific('('))
            return {};
        consume_whitespace();
        Transform transform { .operation = Transform::Operation { body() } };
        consume_whitespace();
        if (m_lexer.consume_specific(')'))
            return transform;
        return {};
    };

    // NOTE: This looks very similar to the CSS transform but the syntax is not compatible.
    Vector<Transform> transform_list;
    consume_whitespace();
    while (!done()) {
        Optional<Transform> maybe_transform;
        if (m_lexer.consume_specific("translate"sv)) {
            maybe_transform = parse_function([&] {
                Transform::Translate translate {};
                translate.x = parse_number();
                translate.y = parse_optional_number();
                return translate;
            });
        } else if (m_lexer.consume_specific("scale"sv)) {
            maybe_transform = parse_function([&] {
                Transform::Scale scale {};
                scale.x = parse_number();
                scale.y = parse_optional_number(scale.x);
                return scale;
            });
        } else if (m_lexer.consume_specific("rotate"sv)) {
            maybe_transform = parse_function([&] {
                Transform::Rotate rotate {};
                rotate.a = parse_number();
                rotate.x = parse_optional_number();
                rotate.y = parse_optional_number();
                return rotate;
            });
        } else if (m_lexer.consume_specific("skewX"sv)) {
            maybe_transform = parse_function([&] {
                Transform::SkewX skew_x {};
                skew_x.a = parse_number();
                return skew_x;
            });
        } else if (m_lexer.consume_specific("skewY"sv)) {
            maybe_transform = parse_function([&] {
                Transform::SkewY skew_y {};
                skew_y.a = parse_number();
                return skew_y;
            });
        } else if (m_lexer.consume_specific("matrix"sv)) {
            maybe_transform = parse_function([&] {
                Transform::Matrix matrix;
                matrix.a = parse_number();
                consume_comma_whitespace();
                matrix.b = parse_number();
                consume_comma_whitespace();
                matrix.c = parse_number();
                consume_comma_whitespace();
                matrix.d = parse_number();
                consume_comma_whitespace();
                matrix.e = parse_number();
                consume_comma_whitespace();
                matrix.f = parse_number();
                return matrix;
            });
        }
        if (maybe_transform.has_value())
            transform_list.append(*maybe_transform);
        else
            return {};
        consume_comma_whitespace();
    }
    return transform_list;
}

bool AttributeParser::match_whitespace() const
{
    if (done())
        return false;
    char c = ch();
    return c == 0x9 || c == 0x20 || c == 0xa || c == 0xc || c == 0xd;
}

bool AttributeParser::match_comma_whitespace() const
{
    return match_whitespace() || match(',');
}

bool AttributeParser::match_coordinate() const
{
    return match_length();
}

bool AttributeParser::match_length() const
{
    return !done() && (isdigit(ch()) || ch() == '-' || ch() == '+' || ch() == '.');
}

}
