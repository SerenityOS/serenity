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

PDFErrorOr<NonnullRefPtr<Type1FontProgram>> PS1FontProgram::create(ReadonlyBytes const& bytes, RefPtr<Encoding> encoding, size_t cleartext_length, size_t encrypted_length)
{
    Reader reader(bytes);
    if (reader.remaining() == 0)
        return error("Empty font program");

    reader.move_to(0);
    if (reader.remaining() < 2 || !reader.matches("%!"))
        return error("Not a font program");

    if (!seek_name(reader, CommonNames::Encoding))
        return error("Missing encoding array");

    auto font_program = adopt_ref(*new PS1FontProgram());
    if (encoding) {
        // 9.6.6.2 Encodings for Type 1 Fonts:
        // An Encoding entry may override a Type 1 font’s mapping from character codes to character names.
        font_program->set_encoding(move(encoding));
    } else {
        if (TRY(parse_word(reader)) == "StandardEncoding") {
            font_program->set_encoding(Encoding::standard_encoding());
        } else {
            auto encoding = Encoding::create();
            while (reader.remaining()) {
                auto word = TRY(parse_word(reader));
                if (word == "readonly") {
                    break;
                } else if (word == "dup") {
                    u8 char_code = TRY(parse_int(reader));
                    auto name = TRY(parse_word(reader));
                    encoding->set(char_code, name.starts_with('/') ? name.substring_view(1) : name.view());
                }
            }
            font_program->set_encoding(move(encoding));
        }
    }

    bool found_font_matrix = seek_name(reader, "FontMatrix");
    if (found_font_matrix) {
        auto array = TRY(parse_number_array(reader, 6));
        font_program->set_font_matrix({ array[0], array[1], array[2], array[3], array[4], array[5] });
    } else {
        font_program->set_font_matrix({ 0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f });
    }

    auto decrypted = TRY(decrypt(reader.bytes().slice(cleartext_length, encrypted_length), 55665, 4));
    TRY(font_program->parse_encrypted_portion(decrypted));
    return font_program;
}

PDFErrorOr<void> PS1FontProgram::parse_encrypted_portion(ByteBuffer const& buffer)
{
    Reader reader(buffer);

    if (seek_name(reader, "lenIV"))
        m_lenIV = TRY(parse_int(reader));

    Vector<ByteBuffer> subroutines;
    if (seek_name(reader, "Subrs"))
        subroutines = TRY(parse_subroutines(reader));

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
                auto glyph_name = word.substring_view(1);
                GlyphParserState state;
                TRY(add_glyph(glyph_name, TRY(parse_glyph(line, subroutines, {}, state, false))));
            }
        }
    }

    consolidate_glyphs();
    return {};
}

PDFErrorOr<Vector<ByteBuffer>> PS1FontProgram::parse_subroutines(Reader& reader) const
{
    if (!reader.matches_number())
        return error("Expected array length");

    auto length = TRY(parse_int(reader));
    VERIFY(length >= 0);

    Vector<ByteBuffer> array;
    TRY(array.try_resize(length));

    while (reader.remaining()) {
        auto word = TRY(parse_word(reader));
        VERIFY(!word.is_empty());

        if (word == "dup") {
            auto index = TRY(parse_int(reader));
            auto entry = TRY(parse_word(reader));

            if (entry.is_empty())
                return error("Empty array entry");

            if (index >= length)
                return error("Array index out of bounds");

            if (isdigit(entry[0])) {
                auto maybe_encrypted_size = entry.to_number<int>();
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
        } else if (word == "index" || word == "def" || word == "ND") {
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

PDFErrorOr<ByteString> PS1FontProgram::parse_word(Reader& reader)
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
    return strtof(ByteString(word).characters(), nullptr);
}

PDFErrorOr<int> PS1FontProgram::parse_int(Reader& reader)
{
    auto maybe_int = TRY(parse_word(reader)).to_number<int>();
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

bool PS1FontProgram::seek_name(Reader& reader, ByteString const& name)
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
    ByteString const& message
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
