/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Encoding.h>
#include <LibPDF/Error.h>
#include <LibPDF/Fonts/CFF.h>
#include <LibPDF/Reader.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<CFF>> CFF::create(ReadonlyBytes const& cff_bytes, RefPtr<Encoding> encoding)
{
    Reader reader(cff_bytes);

    // Header
    // skip major, minor version
    reader.consume(2);
    auto header_size = TRY(reader.try_read<Card8>());
    // skip offset size
    reader.consume(1);
    reader.move_to(header_size);

    // Name INDEX
    Vector<String> font_names;
    TRY(parse_index(reader, [&](ReadonlyBytes const& data) -> PDFErrorOr<void> {
        auto string = TRY(String::from_utf8(data));
        return TRY(font_names.try_append(string));
    }));

    auto cff = adopt_ref(*new CFF());
    cff->set_font_matrix({ 0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f });

    // Top DICT INDEX
    int charset_offset = 0;
    Vector<u8> encoding_codes;
    auto charstrings_offset = 0;
    Vector<ByteBuffer> subroutines;
    int defaultWidthX = 0;
    int nominalWidthX = 0;
    TRY(parse_index(reader, [&](ReadonlyBytes const& element_data) {
        Reader element_reader { element_data };
        return parse_dict<TopDictOperator>(element_reader, [&](TopDictOperator op, Vector<DictOperand> const& operands) -> PDFErrorOr<void> {
            switch (op) {
            case TopDictOperator::Encoding: {
                auto encoding_offset = 0;
                if (!operands.is_empty())
                    encoding_offset = operands[0].get<int>();
                encoding_codes = TRY(parse_encoding(Reader(cff_bytes.slice(encoding_offset))));
                break;
            }
            case TopDictOperator::Charset: {
                if (!operands.is_empty())
                    charset_offset = operands[0].get<int>();
                break;
            }
            case TopDictOperator::CharStrings: {
                if (!operands.is_empty())
                    charstrings_offset = operands[0].get<int>();
                break;
            }
            case TopDictOperator::Private: {
                auto private_dict_size = operands[0].get<int>();
                auto private_dict_offset = operands[1].get<int>();
                Reader priv_dict_reader { cff_bytes.slice(private_dict_offset, private_dict_size) };
                TRY(parse_dict<PrivDictOperator>(priv_dict_reader, [&](PrivDictOperator op, Vector<DictOperand> const& operands) -> PDFErrorOr<void> {
                    switch (op) {
                    case PrivDictOperator::Subrs: {
                        auto subrs_offset = operands[0].get<int>();
                        Reader subrs_reader { cff_bytes.slice(private_dict_offset + subrs_offset) };
                        dbgln("Parsing Subrs INDEX");
                        TRY(parse_index(subrs_reader, [&](ReadonlyBytes const& subroutine_bytes) -> PDFErrorOr<void> {
                            return TRY(subroutines.try_append(TRY(ByteBuffer::copy(subroutine_bytes))));
                        }));
                        break;
                    }
                    case PrivDictOperator::DefaultWidthX:
                        defaultWidthX = operands[0].get<int>();
                        break;
                    case PrivDictOperator::NominalWidthX:
                        nominalWidthX = operands[0].get<int>();
                        break;
                    }
                    return {};
                }));
                break;
            }
            default:;
            }
            return {};
        });
    }));

    // Create glyphs (now that we have the subroutines) and associate missing information to store them and their encoding
    auto glyphs = TRY(parse_charstrings(Reader(cff_bytes.slice(charstrings_offset)), subroutines));
    auto charset = TRY(parse_charset(Reader { cff_bytes.slice(charset_offset) }, glyphs.size()));

    // Adjust glyphs' widths as they are deltas from nominalWidthX
    for (auto& glyph : glyphs) {
        if (!glyph.has_width())
            glyph.set_width(float(defaultWidthX));
        else
            glyph.set_width(glyph.width() + float(nominalWidthX));
    }

    for (size_t i = 0; i < glyphs.size(); i++) {
        if (i == 0) {
            TRY(cff->add_glyph(0, move(glyphs[0])));
            continue;
        }
        auto const& name = charset[i - 1];
        TRY(cff->add_glyph(name, move(glyphs[i])));
    }
    cff->consolidate_glyphs();

    // Encoding given or read
    if (encoding) {
        cff->set_encoding(move(encoding));
    } else {
        auto encoding = Encoding::create();
        for (size_t i = 0; i < glyphs.size(); i++) {
            if (i == 0) {
                encoding->set(0, ".notdef");
                continue;
            }
            auto code = encoding_codes[i - 1];
            auto char_name = charset[i - 1];
            encoding->set(code, char_name);
        }
        cff->set_encoding(move(encoding));
    }

    return cff;
}

/// Appendix C: Predefined Charsets
static constexpr Array s_cff_builtin_names {
    ".notdef"sv,
    "space"sv,
    "exclam"sv,
    "quotedbl"sv,
    "numbersign"sv,
    "dollar"sv,
    "percent"sv,
    "ampersand"sv,
    "quoteright"sv,
    "parenleft"sv,
    "parenright"sv,
    "asterisk"sv,
    "plus"sv,
    "comma"sv,
    "hyphen"sv,
    "period"sv,
    "slash"sv,
    "zero"sv,
    "one"sv,
    "two"sv,
    "three"sv,
    "four"sv,
    "five"sv,
    "six"sv,
    "seven"sv,
    "eight"sv,
    "nine"sv,
    "colon"sv,
    "semicolon"sv,
    "less"sv,
    "equal"sv,
    "greater"sv,
    "question"sv,
    "at"sv,
    "A"sv,
    "B"sv,
    "C"sv,
    "D"sv,
    "E"sv,
    "F"sv,
    "G"sv,
    "H"sv,
    "I"sv,
    "J"sv,
    "K"sv,
    "L"sv,
    "M"sv,
    "N"sv,
    "O"sv,
    "P"sv,
    "Q"sv,
    "R"sv,
    "S"sv,
    "T"sv,
    "U"sv,
    "V"sv,
    "W"sv,
    "X"sv,
    "Y"sv,
    "Z"sv,
    "bracketleft"sv,
    "backslash"sv,
    "bracketright"sv,
    "asciicircum"sv,
    "underscore"sv,
    "quoteleft"sv,
    "a"sv,
    "b"sv,
    "c"sv,
    "d"sv,
    "e"sv,
    "f"sv,
    "g"sv,
    "h"sv,
    "i"sv,
    "j"sv,
    "k"sv,
    "l"sv,
    "m"sv,
    "n"sv,
    "o"sv,
    "p"sv,
    "q"sv,
    "r"sv,
    "s"sv,
    "t"sv,
    "u"sv,
    "v"sv,
    "w"sv,
    "x"sv,
    "y"sv,
    "z"sv,
    "braceleft"sv,
    "bar"sv,
    "braceright"sv,
    "asciitilde"sv,
    "exclamdown"sv,
    "cent"sv,
    "sterling"sv,
    "fraction"sv,
    "yen"sv,
    "florin"sv,
    "section"sv,
    "currency"sv,
    "quotesingle"sv,
    "quotedblleft"sv,
    "guillemotleft"sv,
    "guilsinglleft"sv,
    "guilsinglright"sv,
    "fi"sv,
    "fl"sv,
    "endash"sv,
    "dagger"sv,
    "daggerdbl"sv,
    "periodcentered"sv,
    "paragraph"sv,
    "bullet"sv,
    "quotesinglbase"sv,
    "quotedblbase"sv,
    "quotedblright"sv,
    "guillemotright"sv,
    "ellipsis"sv,
    "perthousand"sv,
    "questiondown"sv,
    "grave"sv,
    "acute"sv,
    "circumflex"sv,
    "tilde"sv,
    "macron"sv,
    "breve"sv,
    "dotaccent"sv,
    "dieresis"sv,
    "ring"sv,
    "cedilla"sv,
    "hungarumlaut"sv,
    "ogonek"sv,
    "caron"sv,
    "emdash"sv,
    "AE"sv,
    "ordfeminine"sv,
    "Lslash"sv,
    "Oslash"sv,
    "OE"sv,
    "ordmasculine"sv,
    "ae"sv,
    "dotlessi"sv,
    "lslash"sv,
    "oslash"sv,
    "oe"sv,
    "germandbls"sv,
    "onesuperior"sv,
    "logicalnot"sv,
    "mu"sv,
    "trademark"sv,
    "Eth"sv,
    "onehalf"sv,
    "plusminus"sv,
    "Thorn"sv,
    "onequarter"sv,
    "divide"sv,
    "brokenbar"sv,
    "degree"sv,
    "thorn"sv,
    "threequarters"sv,
    "twosuperior"sv,
    "registered"sv,
    "minus"sv,
    "eth"sv,
    "multiply"sv,
    "threesuperior"sv,
    "copyright"sv,
    "Aacute"sv,
    "Acircumflex"sv,
    "Adieresis"sv,
    "Agrave"sv,
    "Aring"sv,
    "Atilde"sv,
    "Ccedilla"sv,
    "Eacute"sv,
    "Ecircumflex"sv,
    "Edieresis"sv,
    "Egrave"sv,
    "Iacute"sv,
    "Icircumflex"sv,
    "Idieresis"sv,
    "Igrave"sv,
    "Ntilde"sv,
    "Oacute"sv,
    "Ocircumflex"sv,
    "Odieresis"sv,
    "Ograve"sv,
    "Otilde"sv,
    "Scaron"sv,
    "Uacute"sv,
    "Ucircumflex"sv,
    "Udieresis"sv,
    "Ugrave"sv,
    "Yacute"sv,
    "Ydieresis"sv,
    "Zcaron"sv,
    "aacute"sv,
    "acircumflex"sv,
    "adieresis"sv,
    "agrave"sv,
    "aring"sv,
    "atilde"sv,
    "ccedilla"sv,
    "eacute"sv,
    "ecircumflex"sv,
    "edieresis"sv,
    "egrave"sv,
    "iacute"sv,
    "icircumflex"sv,
    "idieresis"sv,
    "igrave"sv,
    "ntilde"sv,
    "oacute"sv,
    "ocircumflex"sv,
    "odieresis"sv,
    "ograve"sv,
    "otilde"sv,
    "scaron"sv,
    "uacute"sv,
    "ucircumflex"sv,
    "udieresis"sv,
    "ugrave"sv,
    "yacute"sv,
    "ydieresis"sv,
    "zcaron"sv,
};

PDFErrorOr<Vector<DeprecatedFlyString>> CFF::parse_charset(Reader&& reader, size_t glyph_count)
{
    Vector<DeprecatedFlyString> names;
    auto resolve = [](SID sid) {
        if (sid < s_cff_builtin_names.size())
            return DeprecatedFlyString(s_cff_builtin_names[sid]);
        dbgln("Cound't find string for SID {}, going with space", sid);
        return DeprecatedFlyString("space");
    };

    auto format = TRY(reader.try_read<Card8>());
    if (format == 0) {
        for (u8 i = 0; i < glyph_count - 1; i++) {
            SID sid = TRY(reader.try_read<BigEndian<SID>>());
            TRY(names.try_append(resolve(sid)));
        }
    } else if (format == 1) {
        while (names.size() < glyph_count - 1) {
            auto first_sid = TRY(reader.try_read<BigEndian<SID>>());
            int left = TRY(reader.try_read<Card8>());
            for (u8 sid = first_sid; left >= 0; left--, sid++)
                TRY(names.try_append(resolve(sid)));
        }
    }
    return names;
}

PDFErrorOr<Vector<CFF::Glyph>> CFF::parse_charstrings(Reader&& reader, Vector<ByteBuffer> const& subroutines)
{
    Vector<Glyph> glyphs;
    TRY(parse_index(reader, [&](ReadonlyBytes const& charstring_data) -> PDFErrorOr<void> {
        GlyphParserState state;
        auto glyph = TRY(parse_glyph(charstring_data, subroutines, state, true));
        return TRY(glyphs.try_append(glyph));
    }));
    return glyphs;
}

PDFErrorOr<Vector<u8>> CFF::parse_encoding(Reader&& reader)
{
    Vector<u8> encoding_codes;
    auto format_raw = TRY(reader.try_read<Card8>());
    // TODO: support encoding supplements when highest bit is set
    auto format = format_raw & 0x7f;
    if (format == 0) {
        auto n_codes = TRY(reader.try_read<Card8>());
        for (u8 i = 0; i < n_codes; i++) {
            TRY(encoding_codes.try_append(TRY(reader.try_read<Card8>())));
        }
    } else if (format == 1) {
        auto n_ranges = TRY(reader.try_read<Card8>());
        for (u8 i = 0; i < n_ranges; i++) {
            auto first_code = TRY(reader.try_read<Card8>());
            int left = TRY(reader.try_read<Card8>());
            for (u8 code = first_code; left >= 0; left--, code++)
                TRY(encoding_codes.try_append(code));
        }
    } else
        return error(DeprecatedString::formatted("Invalid encoding format: {}", format));
    return encoding_codes;
}

template<typename OperatorT>
PDFErrorOr<void> CFF::parse_dict(Reader& reader, DictEntryHandler<OperatorT>&& handler)
{
    Vector<DictOperand> operands;
    while (reader.remaining() > 0) {
        auto b0 = reader.read<u8>();
        // A command
        if (b0 <= 21) {
            auto op = TRY(parse_dict_operator<OperatorT>(b0, reader));
            TRY(handler(op, operands));
            operands.clear();
            continue;
        }
        // An operand
        TRY(operands.try_append(TRY(load_dict_operand(b0, reader))));
    }
    return {};
}

template PDFErrorOr<void> CFF::parse_dict<CFF::TopDictOperator>(Reader&, DictEntryHandler<TopDictOperator>&&);
template PDFErrorOr<void> CFF::parse_dict<CFF::PrivDictOperator>(Reader&, DictEntryHandler<PrivDictOperator>&&);

template<typename OperatorT>
PDFErrorOr<OperatorT> CFF::parse_dict_operator(u8 b0, Reader& reader)
{
    VERIFY(b0 <= 21);
    if (b0 != 12)
        return OperatorT { (int)b0 };
    auto b1 = TRY(reader.try_read<u8>());
    return OperatorT { b0 << 8 | b1 };
}

template PDFErrorOr<CFF::TopDictOperator> CFF::parse_dict_operator(u8, Reader&);

PDFErrorOr<void> CFF::parse_index(Reader& reader, IndexDataHandler&& data_handler)
{
    Card16 count = TRY(reader.try_read<BigEndian<Card16>>());
    if (count == 0)
        return {};
    auto offset_size = TRY(reader.try_read<OffSize>());
    if (offset_size == 1)
        return parse_index_data<u8>(count, reader, data_handler);
    if (offset_size == 2)
        return parse_index_data<u16>(count, reader, data_handler);
    if (offset_size == 4)
        return parse_index_data<u32>(count, reader, data_handler);
    VERIFY_NOT_REACHED();
}

template<typename OffsetType>
PDFErrorOr<void> CFF::parse_index_data(Card16 count, Reader& reader, IndexDataHandler& handler)
{
    OffsetType last_data_end = 1;
    auto offset_refpoint = reader.offset() + sizeof(OffsetType) * (count + 1) - 1;
    for (u16 i = 0; i < count; i++) {
        reader.save();
        reader.move_by(sizeof(OffsetType) * i);
        OffsetType data_start = reader.read<BigEndian<OffsetType>>();
        last_data_end = reader.read<BigEndian<OffsetType>>();
        auto data_size = last_data_end - data_start;
        reader.move_to(offset_refpoint + data_start);
        TRY(handler(reader.bytes().slice(reader.offset(), data_size)));
        reader.load();
    }
    reader.move_to(offset_refpoint + last_data_end);
    return {};
}

template PDFErrorOr<void> CFF::parse_index_data<u8>(Card16, Reader&, IndexDataHandler&);
template PDFErrorOr<void> CFF::parse_index_data<u16>(Card16, Reader&, IndexDataHandler&);
template PDFErrorOr<void> CFF::parse_index_data<u32>(Card16, Reader&, IndexDataHandler&);

// 4 DICT DATA, Table 3 Operand Encoding
int CFF::load_int_dict_operand(u8 b0, Reader& reader)
{
    if (b0 >= 32 && b0 <= 246) {
        return b0 - 139;
    }
    if (b0 >= 247 && b0 <= 250) {
        auto b1 = reader.read<u8>();
        return (b0 - 247) * 256 + b1 + 108;
    }
    if (b0 >= 251 && b0 <= 254) {
        auto b1 = reader.read<u8>();
        return -(b0 - 251) * 256 - b1 - 108;
    }
    if (b0 == 28) {
        auto b1 = reader.read<u8>();
        auto b2 = reader.read<u8>();
        return b1 << 8 | b2;
    }
    if (b0 == 29) {
        auto b1 = reader.read<u8>();
        auto b2 = reader.read<u8>();
        auto b3 = reader.read<u8>();
        auto b4 = reader.read<u8>();
        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }
    VERIFY_NOT_REACHED();
}

float CFF::load_float_dict_operand(Reader& reader)
{
    StringBuilder sb;
    auto add_nibble = [&](char nibble) {
        if (nibble < 0xa)
            sb.append('0' + nibble);
        else if (nibble == 0xa)
            sb.append('.');
        else if (nibble == 0xb)
            sb.append('E');
        else if (nibble == 0xc)
            sb.append("E-"sv);
        else if (nibble == 0xe)
            sb.append('-');
    };
    while (true) {
        auto byte = reader.read<u8>();
        char nibble1 = (byte & 0xf0) >> 4;
        char nibble2 = byte & 0x0f;
        if (nibble1 == 0xf)
            break;
        add_nibble(nibble1);
        if (nibble2 == 0xf)
            break;
        add_nibble(nibble2);
    }
    auto result = AK::StringUtils::convert_to_floating_point<float>(sb.string_view());
    return result.release_value();
}

PDFErrorOr<CFF::DictOperand> CFF::load_dict_operand(u8 b0, Reader& reader)
{
    if (b0 == 30)
        return load_float_dict_operand(reader);
    if (b0 >= 28)
        return load_int_dict_operand(b0, reader);
    return Error { Error::Type::MalformedPDF, DeprecatedString::formatted("Unknown CFF dict element prefix: {}", b0) };
}
}
