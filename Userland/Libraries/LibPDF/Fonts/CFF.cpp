/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// CFF spec: https://adobe-type-tools.github.io/font-tech-notes/pdfs/5176.CFF.pdf

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Encoding.h>
#include <LibPDF/Error.h>
#include <LibPDF/Fonts/CFF.h>
#include <LibPDF/Reader.h>

namespace PDF {

// CFF spec, "Appendix C Predefined Charsets, Expert"
// clang-format off
static constexpr Array s_predefined_charset_expert {
    1, 229, 230, 231, 232,
    233, 234, 235, 236, 237,
    238, 13, 14, 15, 99,
    239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 27, 28, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 109, 110,
    267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298,
    299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 158, 155, 163, 319, 320, 321, 322, 323, 324, 325, 326, 150,
    164, 169, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342,
    343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360,
    361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378,
};
// clang-format on

// CFF spec, "Appendix C Predefined Charsets, Expert Subset"
// clang-format off
static constexpr Array s_predefined_charset_expert_subset {
    1, 231, 232, 235, 236, 237, 238, 13, 14, 15, 99,
    239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 27,
    28, 249, 250, 251, 253, 254, 255, 256, 257, 258, 259,
    260, 261, 262, 263, 264, 265, 266, 109, 110, 267, 268, 269, 270, 272, 300, 301, 302, 305,
    314, 315, 158, 155, 163, 320, 321, 322, 323, 324, 325, 326, 150, 164, 169, 327, 328, 329,
    330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346,
};
// clang-format on

PDFErrorOr<NonnullRefPtr<CFF>> CFF::create(ReadonlyBytes const& cff_bytes, RefPtr<Encoding> encoding)
{
    Reader reader(cff_bytes);

    // CFF spec, "6 Header"
    // skip major, minor version
    reader.consume(2);
    auto header_size = TRY(reader.try_read<Card8>());
    // skip offset size
    reader.consume(1);
    reader.move_to(header_size);

    // CFF spec, "7 Name INDEX"
    Vector<String> font_names;
    TRY(parse_index(reader, [&](ReadonlyBytes const& data) -> PDFErrorOr<void> {
        auto font_name = TRY(String::from_utf8(data));
        dbgln_if(CFF_DEBUG, "CFF font name '{}'", font_name);
        return TRY(font_names.try_append(font_name));
    }));

    auto cff = adopt_ref(*new CFF());
    cff->set_font_matrix({ 0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f });

    // CFF spec, "8 Top DICT INDEX"
    int charset_offset = 0;
    Vector<u8> encoding_codes;
    auto charstrings_offset = 0;
    Vector<ByteBuffer> subroutines;
    float defaultWidthX = 0;
    float nominalWidthX = 0;
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
                        // CFF spec, "16 Local/Global Subrs INDEXes"
                        // "Local subrs are stored in an INDEX structure which is located via the offset operand of the Subrs operator in the Private DICT."
                        auto subrs_offset = operands[0].get<int>();
                        Reader subrs_reader { cff_bytes.slice(private_dict_offset + subrs_offset) };
                        dbgln("Parsing Subrs INDEX");
                        TRY(parse_index(subrs_reader, [&](ReadonlyBytes const& subroutine_bytes) -> PDFErrorOr<void> {
                            return TRY(subroutines.try_append(TRY(ByteBuffer::copy(subroutine_bytes))));
                        }));
                        break;
                    }
                    case PrivDictOperator::DefaultWidthX:
                        if (!operands.is_empty())
                            defaultWidthX = to_number(operands[0]);
                        break;
                    case PrivDictOperator::NominalWidthX:
                        if (!operands.is_empty())
                            nominalWidthX = to_number(operands[0]);
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

    auto strings = TRY(parse_strings(reader));

    // FIXME: CFF spec "16 Local/Global Subrs INDEXes"
    //        "Global subrs are stored in an INDEX structure which follows the String INDEX."

    // Create glyphs (now that we have the subroutines) and associate missing information to store them and their encoding
    auto glyphs = TRY(parse_charstrings(Reader(cff_bytes.slice(charstrings_offset)), subroutines));

    // CFF spec, "Table 22 Charset ID"
    Vector<DeprecatedFlyString> charset;
    switch (charset_offset) {
    case 0:
        // CFF spec, "Appendix C Predefined Charsets, ISOAdobe"
        for (SID sid = 1; sid <= 228; sid++)
            TRY(charset.try_append(resolve_sid(sid, strings)));
        break;
    case 1:
        for (SID sid : s_predefined_charset_expert)
            TRY(charset.try_append(resolve_sid(sid, strings)));
        break;
    case 2:
        for (SID sid : s_predefined_charset_expert_subset)
            TRY(charset.try_append(resolve_sid(sid, strings)));
        break;
    default:
        charset = TRY(parse_charset(Reader { cff_bytes.slice(charset_offset) }, glyphs.size(), strings));
        break;
    }

    // Adjust glyphs' widths as they are deltas from nominalWidthX
    for (auto& glyph : glyphs) {
        if (!glyph.has_width())
            glyph.set_width(defaultWidthX);
        else
            glyph.set_width(glyph.width() + nominalWidthX);
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
        dbgln_if(CFF_DEBUG, "CFF using external encoding");
        cff->set_encoding(move(encoding));
    } else {
        dbgln_if(CFF_DEBUG, "CFF using embedded encoding");
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

/// Appendix A: Standard Strings
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
    "exclamsmall"sv,
    "Hungarumlautsmall"sv,
    "dollaroldstyle"sv,
    "dollarsuperior"sv,
    "ampersandsmall"sv,
    "Acutesmall"sv,
    "parenleftsuperior"sv,
    "parenrightsuperior"sv,
    "twodotenleader"sv,
    "onedotenleader"sv,
    "zerooldstyle"sv,
    "oneoldstyle"sv,
    "twooldstyle"sv,
    "threeoldstyle"sv,
    "fouroldstyle"sv,
    "fiveoldstyle"sv,
    "sixoldstyle"sv,
    "sevenoldstyle"sv,
    "eightoldstyle"sv,
    "nineoldstyle"sv,
    "commasuperior"sv,
    "threequartersemdash"sv,
    "periodsuperior"sv,
    "questionsmall"sv,
    "asuperior"sv,
    "bsuperior"sv,
    "centsuperior"sv,
    "dsuperior"sv,
    "esuperior"sv,
    "isuperior"sv,
    "lsuperior"sv,
    "msuperior"sv,
    "nsuperior"sv,
    "osuperior"sv,
    "rsuperior"sv,
    "ssuperior"sv,
    "tsuperior"sv,
    "ff"sv,
    "ffi"sv,
    "ffl"sv,
    "parenleftinferior"sv,
    "parenrightinferior"sv,
    "Circumflexsmall"sv,
    "hyphensuperior"sv,
    "Gravesmall"sv,
    "Asmall"sv,
    "Bsmall"sv,
    "Csmall"sv,
    "Dsmall"sv,
    "Esmall"sv,
    "Fsmall"sv,
    "Gsmall"sv,
    "Hsmall"sv,
    "Ismall"sv,
    "Jsmall"sv,
    "Ksmall"sv,
    "Lsmall"sv,
    "Msmall"sv,
    "Nsmall"sv,
    "Osmall"sv,
    "Psmall"sv,
    "Qsmall"sv,
    "Rsmall"sv,
    "Ssmall"sv,
    "Tsmall"sv,
    "Usmall"sv,
    "Vsmall"sv,
    "Wsmall"sv,
    "Xsmall"sv,
    "Ysmall"sv,
    "Zsmall"sv,
    "colonmonetary"sv,
    "onefitted"sv,
    "rupiah"sv,
    "Tildesmall"sv,
    "exclamdownsmall"sv,
    "centoldstyle"sv,
    "Lslashsmall"sv,
    "Scaronsmall"sv,
    "Zcaronsmall"sv,
    "Dieresissmall"sv,
    "Brevesmall"sv,
    "Caronsmall"sv,
    "Dotaccentsmall"sv,
    "Macronsmall"sv,
    "figuredash"sv,
    "hypheninferior"sv,
    "Ogoneksmall"sv,
    "Ringsmall"sv,
    "Cedillasmall"sv,
    "questiondownsmall"sv,
    "oneeighth"sv,
    "threeeighths"sv,
    "fiveeighths"sv,
    "seveneighths"sv,
    "onethird"sv,
    "twothirds"sv,
    "zerosuperior"sv,
    "foursuperior"sv,
    "fivesuperior"sv,
    "sixsuperior"sv,
    "sevensuperior"sv,
    "eightsuperior"sv,
    "ninesuperior"sv,
    "zeroinferior"sv,
    "oneinferior"sv,
    "twoinferior"sv,
    "threeinferior"sv,
    "fourinferior"sv,
    "fiveinferior"sv,
    "sixinferior"sv,
    "seveninferior"sv,
    "eightinferior"sv,
    "nineinferior"sv,
    "centinferior"sv,
    "dollarinferior"sv,
    "periodinferior"sv,
    "commainferior"sv,
    "Agravesmall"sv,
    "Aacutesmall"sv,
    "Acircumflexsmall"sv,
    "Atildesmall"sv,
    "Adieresissmall"sv,
    "Aringsmall"sv,
    "AEsmall"sv,
    "Ccedillasmall"sv,
    "Egravesmall"sv,
    "Eacutesmall"sv,
    "Ecircumflexsmall"sv,
    "Edieresissmall"sv,
    "Igravesmall"sv,
    "Iacutesmall"sv,
    "Icircumflexsmall"sv,
    "Idieresissmall"sv,
    "Ethsmall"sv,
    "Ntildesmall"sv,
    "Ogravesmall"sv,
    "Oacutesmall"sv,
    "Ocircumflexsmall"sv,
    "Otildesmall"sv,
    "Odieresissmall"sv,
    "OEsmall"sv,
    "Oslashsmall"sv,
    "Ugravesmall"sv,
    "Uacutesmall"sv,
    "Ucircumflexsmall"sv,
    "Udieresissmall"sv,
    "Yacutesmall"sv,
    "Thornsmall"sv,
    "Ydieresissmall"sv,
    "001.000"sv,
    "001.001"sv,
    "001.002"sv,
    "001.003"sv,
    "Black"sv,
    "Bold"sv,
    "Book"sv,
    "Light"sv,
    "Medium"sv,
    "Regular"sv,
    "Roman"sv,
    "Semibold"sv,
};

PDFErrorOr<Vector<StringView>> CFF::parse_strings(Reader& reader)
{
    // CFF spec "10 String Index"
    Vector<StringView> strings;
    TRY(parse_index(reader, [&](ReadonlyBytes const& string) -> PDFErrorOr<void> {
        return TRY(strings.try_append(string));
    }));
    return strings;
}

DeprecatedFlyString CFF::resolve_sid(SID sid, Vector<StringView> const& strings)
{
    if (sid < s_cff_builtin_names.size())
        return DeprecatedFlyString(s_cff_builtin_names[sid]);

    if (sid - s_cff_builtin_names.size() < strings.size())
        return DeprecatedFlyString(strings[sid - s_cff_builtin_names.size()]);

    dbgln("Couldn't find string for SID {}, going with space", sid);
    return DeprecatedFlyString("space");
}

PDFErrorOr<Vector<DeprecatedFlyString>> CFF::parse_charset(Reader&& reader, size_t glyph_count, Vector<StringView> const& strings)
{
    // CFF spec, "13 Charsets"
    Vector<DeprecatedFlyString> names;
    auto format = TRY(reader.try_read<Card8>());
    if (format == 0) {
        // CFF spec, "Table 17 Format 0"
        for (u8 i = 0; i < glyph_count - 1; i++) {
            SID sid = TRY(reader.try_read<BigEndian<SID>>());
            TRY(names.try_append(resolve_sid(sid, strings)));
        }
    } else if (format == 1) {
        // CFF spec, "Table 18 Format 1"
        while (names.size() < glyph_count - 1) {
            // CFF spec, "Table 19 Range1 Format (Charset)"
            auto first_sid = TRY(reader.try_read<BigEndian<SID>>());
            int left = TRY(reader.try_read<Card8>());
            for (SID sid = first_sid; left >= 0; left--, sid++)
                TRY(names.try_append(resolve_sid(sid, strings)));
        }
    }
    return names;
}

PDFErrorOr<Vector<CFF::Glyph>> CFF::parse_charstrings(Reader&& reader, Vector<ByteBuffer> const& subroutines)
{
    // CFF spec, "14 CharStrings INDEX"
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
    // CFF spec, "12 Encodings"
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
            // CFF spec, "Table 13 Range1 Format (Encoding)"
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
    // CFF spec, "4 DICT data"
    Vector<DictOperand> operands;
    while (reader.remaining() > 0) {
        auto b0 = reader.read<u8>();
        // "Operators and operands may be distinguished by inspection of their first byte: 0-21 specify operators"
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
    // CFF spec, "4 DICT data"
    VERIFY(b0 <= 21);

    // "Two-byte operators have an initial escape byte of 12."
    if (b0 != 12)
        return OperatorT { (int)b0 };
    auto b1 = TRY(reader.try_read<u8>());
    return OperatorT { b0 << 8 | b1 };
}

template PDFErrorOr<CFF::TopDictOperator> CFF::parse_dict_operator(u8, Reader&);

PDFErrorOr<void> CFF::parse_index(Reader& reader, IndexDataHandler&& data_handler)
{
    // CFF spec, "5 INDEX Data"
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
    // CFF spec, "5 INDEX Data"
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

int CFF::load_int_dict_operand(u8 b0, Reader& reader)
{
    // CFF spec, "Table 3 Operand Encoding"
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
    // CFF spec, "Table 5 Nibble Definitions"
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
    // CFF spec, "4 DICT data"
    if (b0 == 30)
        return load_float_dict_operand(reader);
    if (b0 >= 28)
        return load_int_dict_operand(b0, reader);
    return Error { Error::Type::MalformedPDF, DeprecatedString::formatted("Unknown CFF dict element prefix: {}", b0) };
}
}
