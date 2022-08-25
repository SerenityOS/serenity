/*
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Encoding.h>
#include <LibPDF/Fonts/PS1FontProgram.h>
#include <LibPDF/Reader.h>
#include <ctype.h>
#include <math.h>

namespace PDF {

enum Command {
    HStem = 1,
    VStem = 3,
    VMoveTo,
    RLineTo,
    HLineTo,
    VLineTo,
    RRCurveTo,
    ClosePath,
    CallSubr,
    Return,
    Extended,
    HSbW,
    EndChar,
    RMoveTo = 21,
    HMoveTo,
    VHCurveTo = 30,
    HVCurveTo
};

enum ExtendedCommand {
    DotSection,
    VStem3,
    HStem3,
    Div = 12,
    CallOtherSubr = 16,
    Pop,
    SetCurrentPoint = 33,
};

PDFErrorOr<void> PS1FontProgram::parse(ReadonlyBytes const& bytes, size_t cleartext_length, size_t encrypted_length)
{
    Reader reader(bytes);
    if (reader.remaining() == 0)
        return error("Empty font program");

    reader.move_to(0);
    if (reader.remaining() < 2 || !reader.matches("%!"))
        return error("Not a font program");

    if (!seek_name(reader, CommonNames::Encoding))
        return error("Missing encoding array");

    if (TRY(parse_word(reader)) == "StandardEncoding") {
        m_encoding = Encoding::standard_encoding();
    } else {
        HashMap<u16, CharDescriptor> descriptors;

        while (reader.remaining()) {
            auto word = TRY(parse_word(reader));
            if (word == "readonly") {
                break;
            } else if (word == "dup") {
                u32 code_point = TRY(parse_int(reader));
                auto name = TRY(parse_word(reader));
                descriptors.set(code_point, { name.starts_with('/') ? name.substring_view(1) : name.view(), code_point });
            }
        }
        m_encoding = TRY(Encoding::create(descriptors));
    }

    bool found_font_matrix = seek_name(reader, "FontMatrix");
    if (found_font_matrix) {
        auto array = TRY(parse_number_array(reader, 6));
        m_font_matrix = { array[0], array[1], array[2], array[3], array[4], array[5] };
    } else {
        m_font_matrix = { 0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f };
    }

    auto decrypted = TRY(decrypt(reader.bytes().slice(cleartext_length, encrypted_length), 55665, 4));
    return parse_encrypted_portion(decrypted);
}

Gfx::Path PS1FontProgram::build_char(u32 code_point, Gfx::FloatPoint const& point, float width)
{
    if (!m_glyph_map.contains(code_point))
        return {};

    auto glyph = m_glyph_map.get(code_point).value();
    auto scale = width / (m_font_matrix.a() * glyph.width + m_font_matrix.e());
    auto transform = m_font_matrix;

    // Convert character space to device space.
    transform.scale(scale, -scale);
    transform.set_translation(point);

    return glyph.path.copy_transformed(transform);
}

PDFErrorOr<PS1FontProgram::Glyph> PS1FontProgram::parse_glyph(ReadonlyBytes const& data, GlyphParserState& state)
{
    auto push = [&](float value) -> PDFErrorOr<void> {
        if (state.sp >= state.stack.size())
            return error("Operand stack overflow");
        state.stack[state.sp++] = value;
        return {};
    };

    auto pop = [&]() -> float {
        return state.sp ? state.stack[--state.sp] : 0.0f;
    };

    auto& path = state.glyph.path;

    // Parse the stream of parameters and commands that make up a glyph outline.
    for (size_t i = 0; i < data.size(); ++i) {
        auto require = [&](unsigned num) -> PDFErrorOr<void> {
            if (i + num >= data.size())
                return error("Malformed glyph outline definition");
            return {};
        };

        int v = data[i];
        if (v == 255) {
            TRY(require(4));
            int a = data[++i];
            int b = data[++i];
            int c = data[++i];
            int d = data[++i];
            TRY(push((a << 24) + (b << 16) + (c << 8) + d));
        } else if (v >= 251) {
            TRY(require(1));
            auto w = data[++i];
            TRY(push(-((v - 251) * 256) - w - 108));
        } else if (v >= 247) {
            TRY(require(1));
            auto w = data[++i];
            TRY(push(((v - 247) * 256) + w + 108));
        } else if (v >= 32) {
            TRY(push(v - 139));
        } else {
            // Not a parameter but a command byte.
            switch (v) {
            case HStem:
            case VStem:
                state.sp = 0;
                break;

            case VMoveTo: {
                auto dy = pop();

                state.point.translate_by(0.0f, dy);

                if (state.flex_feature) {
                    state.flex_sequence[state.flex_index++] = state.point.x();
                    state.flex_sequence[state.flex_index++] = state.point.y();
                } else {
                    path.move_to(state.point);
                }
                state.sp = 0;
                break;
            }

            case RLineTo: {
                auto dy = pop();
                auto dx = pop();

                state.point.translate_by(dx, dy);
                path.line_to(state.point);
                state.sp = 0;
                break;
            }

            case HLineTo: {
                auto dx = pop();

                state.point.translate_by(dx, 0.0f);
                path.line_to(state.point);
                state.sp = 0;
                break;
            }

            case VLineTo: {
                auto dy = pop();

                state.point.translate_by(0.0f, dy);
                path.line_to(state.point);
                state.sp = 0;
                break;
            }

            case RRCurveTo: {
                auto dy3 = pop();
                auto dx3 = pop();
                auto dy2 = pop();
                auto dx2 = pop();
                auto dy1 = pop();
                auto dx1 = pop();

                auto& point = state.point;

                path.cubic_bezier_curve_to(
                    point + Gfx::FloatPoint(dx1, dy1),
                    point + Gfx::FloatPoint(dx1 + dx2, dy1 + dy2),
                    point + Gfx::FloatPoint(dx1 + dx2 + dx3, dy1 + dy2 + dy3));

                point.translate_by(dx1 + dx2 + dx3, dy1 + dy2 + dy3);
                state.sp = 0;
                break;
            }

            case ClosePath:
                path.close();
                state.sp = 0;
                break;

            case CallSubr: {
                auto subr_number = pop();
                if (static_cast<size_t>(subr_number) >= m_subroutines.size())
                    return error("Subroutine index out of range");

                // Subroutines 0-2 handle the flex feature.
                if (subr_number == 0) {
                    if (state.flex_index != 14)
                        break;

                    auto& flex = state.flex_sequence;

                    path.cubic_bezier_curve_to(
                        { flex[2], flex[3] },
                        { flex[4], flex[5] },
                        { flex[6], flex[7] });
                    path.cubic_bezier_curve_to(
                        { flex[8], flex[9] },
                        { flex[10], flex[11] },
                        { flex[12], flex[13] });

                    state.flex_feature = false;
                    state.sp = 0;
                } else if (subr_number == 1) {
                    state.flex_feature = true;
                    state.flex_index = 0;
                    state.sp = 0;
                } else if (subr_number == 2) {
                    state.sp = 0;
                } else {
                    auto subr = m_subroutines[subr_number];
                    if (subr.is_empty())
                        return error("Empty subroutine");

                    TRY(parse_glyph(subr, state));
                }
                break;
            }

            case Return:
                break;

            case Extended: {
                TRY(require(1));
                switch (data[++i]) {
                case DotSection:
                case VStem3:
                case HStem3:
                    // FIXME: Do something with these?
                    state.sp = 0;
                    break;

                case Div: {
                    auto num2 = pop();
                    auto num1 = pop();

                    TRY(push(num2 ? num1 / num2 : 0.0f));
                    break;
                }

                case CallOtherSubr: {
                    auto othersubr_number = pop();
                    auto n = static_cast<int>(pop());

                    if (othersubr_number == 0) {
                        state.postscript_stack[state.postscript_sp++] = pop();
                        state.postscript_stack[state.postscript_sp++] = pop();
                        pop();
                    } else if (othersubr_number == 3) {
                        state.postscript_stack[state.postscript_sp++] = 3;
                    } else {
                        for (int i = 0; i < n; ++i)
                            state.postscript_stack[state.postscript_sp++] = pop();
                    }

                    (void)othersubr_number;
                    break;
                }

                case Pop:
                    TRY(push(state.postscript_stack[--state.postscript_sp]));
                    break;

                case SetCurrentPoint: {
                    auto y = pop();
                    auto x = pop();

                    state.point = { x, y };
                    path.move_to(state.point);
                    state.sp = 0;
                    break;
                }

                default:
                    return error(String::formatted("Unhandled command: 12 {}", data[i]));
                }
                break;
            }

            case HSbW: {
                auto wx = pop();
                auto sbx = pop();

                state.glyph.width = wx;
                state.point = { sbx, 0.0f };
                state.sp = 0;
                break;
            }

            case EndChar:
                break;

            case RMoveTo: {
                auto dy = pop();
                auto dx = pop();

                state.point.translate_by(dx, dy);

                if (state.flex_feature) {
                    state.flex_sequence[state.flex_index++] = state.point.x();
                    state.flex_sequence[state.flex_index++] = state.point.y();
                } else {
                    path.move_to(state.point);
                }
                state.sp = 0;
                break;
            }

            case HMoveTo: {
                auto dx = pop();

                state.point.translate_by(dx, 0.0f);

                if (state.flex_feature) {
                    state.flex_sequence[state.flex_index++] = state.point.x();
                    state.flex_sequence[state.flex_index++] = state.point.y();
                } else {
                    path.move_to(state.point);
                }
                state.sp = 0;
                break;
            }

            case VHCurveTo: {
                auto dx3 = pop();
                auto dy2 = pop();
                auto dx2 = pop();
                auto dy1 = pop();

                auto& point = state.point;

                path.cubic_bezier_curve_to(
                    point + Gfx::FloatPoint(0.0f, dy1),
                    point + Gfx::FloatPoint(dx2, dy1 + dy2),
                    point + Gfx::FloatPoint(dx2 + dx3, dy1 + dy2));

                point.translate_by(dx2 + dx3, dy1 + dy2);
                state.sp = 0;
                break;
            }

            case HVCurveTo: {
                auto dy3 = pop();
                auto dy2 = pop();
                auto dx2 = pop();
                auto dx1 = pop();

                auto& point = state.point;

                path.cubic_bezier_curve_to(
                    point + Gfx::FloatPoint(dx1, 0.0f),
                    point + Gfx::FloatPoint(dx1 + dx2, dy2),
                    point + Gfx::FloatPoint(dx1 + dx2, dy2 + dy3));

                point.translate_by(dx1 + dx2, dy2 + dy3);
                state.sp = 0;
                break;
            }

            default:
                return error(String::formatted("Unhandled command: {}", v));
            }
        }
    }

    return state.glyph;
}

PDFErrorOr<void> PS1FontProgram::parse_encrypted_portion(ByteBuffer const& buffer)
{
    Reader reader(buffer);

    if (seek_name(reader, "lenIV"))
        m_lenIV = TRY(parse_int(reader));

    if (!seek_name(reader, "Subrs"))
        return error("Missing subroutine array");
    m_subroutines = TRY(parse_subroutines(reader));

    if (!seek_name(reader, "CharStrings"))
        return error("Missing char strings array");

    while (reader.remaining()) {
        auto word = TRY(parse_word(reader));
        VERIFY(!word.is_empty());

        if (word == "end")
            break;

        if (word[0] == '/') {
            auto encrypted_size = TRY(parse_int(reader));
            auto rd = TRY(parse_word(reader));
            if (rd == "-|" || rd == "RD") {
                auto line = TRY(decrypt(reader.bytes().slice(reader.offset(), encrypted_size), m_encryption_key, m_lenIV));
                reader.move_by(encrypted_size);
                auto name_mapping = m_encoding->name_mapping();
                auto code_point = name_mapping.ensure(word.substring_view(1));
                GlyphParserState state;
                m_glyph_map.set(code_point, TRY(parse_glyph(line, state)));
            }
        }
    }

    return {};
}

PDFErrorOr<Vector<ByteBuffer>> PS1FontProgram::parse_subroutines(Reader& reader)
{
    if (!reader.matches_number())
        return error("Expected array length");

    auto length = TRY(parse_int(reader));
    VERIFY(length <= 1024);

    Vector<ByteBuffer> array;
    TRY(array.try_resize(length));

    while (reader.remaining()) {
        auto word = TRY(parse_word(reader));
        if (word.is_empty())
            VERIFY(0);

        if (word == "dup") {
            auto index = TRY(parse_int(reader));
            auto entry = TRY(parse_word(reader));

            if (entry.is_empty())
                return error("Empty array entry");

            if (index >= length)
                return error("Array index out of bounds");

            if (isdigit(entry[0])) {
                auto maybe_encrypted_size = entry.to_int();
                if (!maybe_encrypted_size.has_value())
                    return error("Malformed array");
                auto rd = TRY(parse_word(reader));
                if (rd == "-|" || rd == "RD") {
                    array[index] = TRY(decrypt(reader.bytes().slice(reader.offset(), maybe_encrypted_size.value()), m_encryption_key, m_lenIV));
                    reader.move_by(maybe_encrypted_size.value());
                }
            } else {
                array[index] = TRY(ByteBuffer::copy(entry.bytes()));
            }
        } else if (word == "index") {
            break;
        }
    }

    return array;
}

PDFErrorOr<Vector<float>> PS1FontProgram::parse_number_array(Reader& reader, size_t length)
{
    Vector<float> array;
    TRY(array.try_resize(length));

    reader.consume_whitespace();

    if (!reader.consume('['))
        return error("Expected array to start with '['");

    reader.consume_whitespace();

    for (size_t i = 0; i < length; ++i)
        array.at(i) = TRY(parse_float(reader));

    if (!reader.consume(']'))
        return error("Expected array to end with ']'");

    return array;
}

PDFErrorOr<String> PS1FontProgram::parse_word(Reader& reader)
{
    reader.consume_whitespace();

    auto start = reader.offset();
    reader.move_while([&](char c) {
        return !reader.matches_whitespace() && c != '[' && c != ']';
    });
    auto end = reader.offset();

    if (reader.matches_whitespace())
        reader.consume();

    return StringView(reader.bytes().data() + start, end - start);
}

PDFErrorOr<float> PS1FontProgram::parse_float(Reader& reader)
{
    auto word = TRY(parse_word(reader));
    return strtof(String(word).characters(), nullptr);
}

PDFErrorOr<int> PS1FontProgram::parse_int(Reader& reader)
{
    auto maybe_int = TRY(parse_word(reader)).to_int();
    if (!maybe_int.has_value())
        return error("Invalid int");
    return maybe_int.value();
}

PDFErrorOr<ByteBuffer> PS1FontProgram::decrypt(ReadonlyBytes const& encrypted, u16 key, size_t skip)
{
    auto decrypted = TRY(ByteBuffer::create_uninitialized(encrypted.size() - skip));

    u16 R = key;
    u16 c1 = 52845;
    u16 c2 = 22719;

    for (size_t i = 0; i < encrypted.size(); ++i) {
        u8 C = encrypted[i];
        u8 P = C ^ (R >> 8);
        R = (C + R) * c1 + c2;
        if (i >= skip)
            decrypted[i - skip] = P;
    }

    return decrypted;
}

bool PS1FontProgram::seek_name(Reader& reader, String const& name)
{
    auto start = reader.offset();

    reader.move_to(0);
    while (reader.remaining()) {
        if (reader.consume('/') && reader.matches(name.characters())) {
            // Skip name
            reader.move_while([&](char) {
                return reader.matches_regular_character();
            });
            reader.consume_whitespace();
            return true;
        }
    }

    // Jump back to where we started
    reader.move_to(start);
    return false;
}

Error PS1FontProgram::error(
    String const& message
#ifdef PDF_DEBUG
    ,
    SourceLocation loc
#endif
)
{
#ifdef PDF_DEBUG
    dbgln("\033[31m{} Type 1 font error: {}\033[0m", loc, message);
#endif

    return Error { Error::Type::MalformedPDF, message };
}
}
