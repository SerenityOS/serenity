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

// The built-in encodings map codes to SIDs.

// CFF spec, "Appendix B Predefined Encodings, Standard Encoding"
// clang-format off
static constexpr Array s_predefined_encoding_standard {
      0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
      0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   1,   2,   3,   4,   5,   6,   7,   8,   9,  10,
     11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,

     32,  33,  34,  35,  36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,  60,
     61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  81,  82,  83,  84,  85,  86,  87,  88,  89,
     90,  91,  92,  93,  94,  95,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,

      0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  96,  97,  98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,   0, 111, 112,
    113, 114,   0, 115, 116, 117, 118, 119, 120, 121, 122,   0, 123,   0, 124, 125, 126, 127, 128, 129, 130, 131,   0, 132, 133,   0, 134, 135, 136,
    137,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0, 138,   0, 139,   0,   0,   0,   0, 140, 141, 142, 143,   0,

      0,   0,   0,   0, 144,   0,   0,
      0, 145,   0,   0, 146, 147, 148,
    149,   0,   0,   0,   0,
};
static_assert(s_predefined_encoding_standard.size() == 256);
// clang-format on

// CFF spec, "Appendix B Predefined Encodings, Expert Encoding"
// clang-format off
static constexpr Array s_predefined_encoding_expert {
      0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
      0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   1, 229, 230,   0,
    231, 232, 233, 234, 235, 236, 237, 238,  13,  14,  15,  99, 239, 240, 241, 242, 243, 244,

    245, 246, 247, 248,  27,  28, 249, 250, 251, 252,   0, 253, 254, 255, 256, 257,   0,   0,   0, 258,   0,   0, 259, 260, 261, 262,   0,   0, 263,
    264, 265,   0, 266, 109, 110, 267, 268, 269,   0, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288,
    289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,

      0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0, 304, 305, 306,   0,   0, 307, 308, 309, 310,
    311,   0, 312,   0,   0, 313,   0,   0, 314, 315,   0,   0, 316, 317, 318,   0,   0,   0, 158, 155, 163, 319, 320, 321, 322, 323, 324, 325,   0,
      0, 326, 150, 164, 169, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350,

    351, 352, 353, 354, 355, 356, 357, 358, 359, 360,
    361, 362, 363, 364, 365, 366, 367, 368, 369, 370,
    371, 372, 373, 374, 375, 376, 377, 378,
};
static_assert(s_predefined_encoding_expert.size() == 256);
// clang-format on

// Charsets map GIDs to SIDs.

// CFF spec, "Appendix C Predefined Charsets, Expert"
// clang-format off
static constexpr Array s_predefined_charset_expert {
      1, 229, 230, 231, 232,
    233, 234, 235, 236, 237,
    238,  13,  14,  15,  99,

    239, 240, 241, 242, 243, 244, 245, 246, 247, 248,  27,  28, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 109, 110,
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
      1, 231, 232, 235, 236, 237, 238,  13,  14,  15,  99,
    239, 240, 241, 242, 243, 244, 245, 246, 247, 248,  27,
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

    if (font_names.size() != 1)
        return error("CFFs with more than one font not yet implemented");

    auto cff = adopt_ref(*new CFF());
    cff->set_font_matrix({ 0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f });

    // CFF spec, "8 Top DICT INDEX"
    int charset_offset = 0;
    int encoding_offset = 0;
    auto charstrings_offset = 0;
    Vector<ByteBuffer> local_subroutines;
    float defaultWidthX = 0;
    float nominalWidthX = 0;
    TRY(parse_index(reader, [&](ReadonlyBytes const& element_data) {
        Reader element_reader { element_data };
        return parse_dict<TopDictOperator>(element_reader, [&](TopDictOperator op, Vector<DictOperand> const& operands) -> PDFErrorOr<void> {
            switch (op) {
            case TopDictOperator::Version:
            case TopDictOperator::Notice:
            case TopDictOperator::FullName:
            case TopDictOperator::FamilyName:
            case TopDictOperator::Weight:
            case TopDictOperator::FontBBox:
            case TopDictOperator::UniqueID:
            case TopDictOperator::XUID:
            case TopDictOperator::Copyright:
            case TopDictOperator::IsFixedPitch:
            case TopDictOperator::ItalicAngle:
            case TopDictOperator::UnderlinePosition:
            case TopDictOperator::UnderlineThickness:
            case TopDictOperator::PaintType:
            case TopDictOperator::FontMatrix:
            case TopDictOperator::StrokeWidth:
            case TopDictOperator::PostScript:
            case TopDictOperator::BaseFontName:
            case TopDictOperator::BaseFontBlend:
                break;
            case TopDictOperator::CharstringType: {
                int charstring_type = 2;
                if (!operands.is_empty())
                    charstring_type = operands[0].get<int>();
                if (charstring_type != 2)
                    dbgln("CFF: has unimplemented CharstringType, might not look right");
                break;
            }
            case TopDictOperator::SyntheticBase:
                dbgln("CFF: has unimplemented SyntheticBase, might not look right");
                break;
            case TopDictOperator::Encoding: {
                if (!operands.is_empty())
                    encoding_offset = operands[0].get<int>();
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
                        TRY(parse_index(subrs_reader, [&](ReadonlyBytes const& subroutine_bytes) -> PDFErrorOr<void> {
                            return TRY(local_subroutines.try_append(TRY(ByteBuffer::copy(subroutine_bytes))));
                        }));
                        dbgln_if(CFF_DEBUG, "CFF has {} subr entries", local_subroutines.size());
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
            default:
                dbgln("CFF: Unhandled top dict entry {}", static_cast<int>(op));
            }
            return {};
        });
    }));

    auto strings = TRY(parse_strings(reader));

    // CFF spec "16 Local/Global Subrs INDEXes"
    // "Global subrs are stored in an INDEX structure which follows the String INDEX."
    Vector<ByteBuffer> global_subroutines;
    TRY(parse_index(reader, [&](ReadonlyBytes const& subroutine_bytes) -> PDFErrorOr<void> {
        return TRY(global_subroutines.try_append(TRY(ByteBuffer::copy(subroutine_bytes))));
    }));
    dbgln_if(CFF_DEBUG, "CFF has {} gsubr entries", global_subroutines.size());

    // Create glyphs (now that we have the subroutines) and associate missing information to store them and their encoding
    auto glyphs = TRY(parse_charstrings(Reader(cff_bytes.slice(charstrings_offset)), local_subroutines, global_subroutines));

    // CFF spec, "Table 16 Encoding ID"
    // FIXME: Only read this if the built-in encoding is actually needed? (ie. `if (!encoding)`)
    Vector<u8> encoding_codes;                 // Maps GID to its codepoint.
    HashMap<Card8, SID> encoding_supplemental; // Maps codepoint to SID.
    switch (encoding_offset) {
    case 0:
        dbgln_if(CFF_DEBUG, "CFF predefined encoding Standard");
        for (size_t i = 1; i < s_predefined_encoding_standard.size(); ++i)
            TRY(encoding_supplemental.try_set(i, s_predefined_encoding_standard[i]));
        break;
    case 1:
        dbgln_if(CFF_DEBUG, "CFF predefined encoding Expert");
        for (size_t i = 1; i < s_predefined_encoding_expert.size(); ++i)
            TRY(encoding_supplemental.try_set(i, s_predefined_encoding_expert[i]));
        break;
    default:
        encoding_codes = TRY(parse_encoding(Reader(cff_bytes.slice(encoding_offset)), encoding_supplemental));
        break;
    }

    // CFF spec, "Table 22 Charset ID"
    Vector<ByteString> charset;
    switch (charset_offset) {
    case 0:
        dbgln_if(CFF_DEBUG, "CFF predefined charset ISOAdobe");
        // CFF spec, "Appendix C Predefined Charsets, ISOAdobe"
        for (SID sid = 1; sid <= 228; sid++)
            TRY(charset.try_append(resolve_sid(sid, strings)));
        break;
    case 1:
        dbgln_if(CFF_DEBUG, "CFF predefined charset Expert");
        for (SID sid : s_predefined_charset_expert)
            TRY(charset.try_append(resolve_sid(sid, strings)));
        break;
    case 2:
        dbgln_if(CFF_DEBUG, "CFF predefined charset Expert Subset");
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
            if (i >= encoding_codes.size() || i >= charset.size())
                break;
            auto code = encoding_codes[i - 1];
            auto char_name = charset[i - 1];
            encoding->set(code, char_name);
        }
        for (auto const& entry : encoding_supplemental)
            encoding->set(entry.key, resolve_sid(entry.value, strings));
        cff->set_encoding(move(encoding));
    }

    return cff;
}

/// Appendix A: Standard Strings
static constexpr Array s_cff_builtin_names {
    ".notdef",
    "space",
    "exclam",
    "quotedbl",
    "numbersign",
    "dollar",
    "percent",
    "ampersand",
    "quoteright",
    "parenleft",
    "parenright",
    "asterisk",
    "plus",
    "comma",
    "hyphen",
    "period",
    "slash",
    "zero",
    "one",
    "two",
    "three",
    "four",
    "five",
    "six",
    "seven",
    "eight",
    "nine",
    "colon",
    "semicolon",
    "less",
    "equal",
    "greater",
    "question",
    "at",
    "A",
    "B",
    "C",
    "D",
    "E",
    "F",
    "G",
    "H",
    "I",
    "J",
    "K",
    "L",
    "M",
    "N",
    "O",
    "P",
    "Q",
    "R",
    "S",
    "T",
    "U",
    "V",
    "W",
    "X",
    "Y",
    "Z",
    "bracketleft",
    "backslash",
    "bracketright",
    "asciicircum",
    "underscore",
    "quoteleft",
    "a",
    "b",
    "c",
    "d",
    "e",
    "f",
    "g",
    "h",
    "i",
    "j",
    "k",
    "l",
    "m",
    "n",
    "o",
    "p",
    "q",
    "r",
    "s",
    "t",
    "u",
    "v",
    "w",
    "x",
    "y",
    "z",
    "braceleft",
    "bar",
    "braceright",
    "asciitilde",
    "exclamdown",
    "cent",
    "sterling",
    "fraction",
    "yen",
    "florin",
    "section",
    "currency",
    "quotesingle",
    "quotedblleft",
    "guillemotleft",
    "guilsinglleft",
    "guilsinglright",
    "fi",
    "fl",
    "endash",
    "dagger",
    "daggerdbl",
    "periodcentered",
    "paragraph",
    "bullet",
    "quotesinglbase",
    "quotedblbase",
    "quotedblright",
    "guillemotright",
    "ellipsis",
    "perthousand",
    "questiondown",
    "grave",
    "acute",
    "circumflex",
    "tilde",
    "macron",
    "breve",
    "dotaccent",
    "dieresis",
    "ring",
    "cedilla",
    "hungarumlaut",
    "ogonek",
    "caron",
    "emdash",
    "AE",
    "ordfeminine",
    "Lslash",
    "Oslash",
    "OE",
    "ordmasculine",
    "ae",
    "dotlessi",
    "lslash",
    "oslash",
    "oe",
    "germandbls",
    "onesuperior",
    "logicalnot",
    "mu",
    "trademark",
    "Eth",
    "onehalf",
    "plusminus",
    "Thorn",
    "onequarter",
    "divide",
    "brokenbar",
    "degree",
    "thorn",
    "threequarters",
    "twosuperior",
    "registered",
    "minus",
    "eth",
    "multiply",
    "threesuperior",
    "copyright",
    "Aacute",
    "Acircumflex",
    "Adieresis",
    "Agrave",
    "Aring",
    "Atilde",
    "Ccedilla",
    "Eacute",
    "Ecircumflex",
    "Edieresis",
    "Egrave",
    "Iacute",
    "Icircumflex",
    "Idieresis",
    "Igrave",
    "Ntilde",
    "Oacute",
    "Ocircumflex",
    "Odieresis",
    "Ograve",
    "Otilde",
    "Scaron",
    "Uacute",
    "Ucircumflex",
    "Udieresis",
    "Ugrave",
    "Yacute",
    "Ydieresis",
    "Zcaron",
    "aacute",
    "acircumflex",
    "adieresis",
    "agrave",
    "aring",
    "atilde",
    "ccedilla",
    "eacute",
    "ecircumflex",
    "edieresis",
    "egrave",
    "iacute",
    "icircumflex",
    "idieresis",
    "igrave",
    "ntilde",
    "oacute",
    "ocircumflex",
    "odieresis",
    "ograve",
    "otilde",
    "scaron",
    "uacute",
    "ucircumflex",
    "udieresis",
    "ugrave",
    "yacute",
    "ydieresis",
    "zcaron",
    "exclamsmall",
    "Hungarumlautsmall",
    "dollaroldstyle",
    "dollarsuperior",
    "ampersandsmall",
    "Acutesmall",
    "parenleftsuperior",
    "parenrightsuperior",
    "twodotenleader",
    "onedotenleader",
    "zerooldstyle",
    "oneoldstyle",
    "twooldstyle",
    "threeoldstyle",
    "fouroldstyle",
    "fiveoldstyle",
    "sixoldstyle",
    "sevenoldstyle",
    "eightoldstyle",
    "nineoldstyle",
    "commasuperior",
    "threequartersemdash",
    "periodsuperior",
    "questionsmall",
    "asuperior",
    "bsuperior",
    "centsuperior",
    "dsuperior",
    "esuperior",
    "isuperior",
    "lsuperior",
    "msuperior",
    "nsuperior",
    "osuperior",
    "rsuperior",
    "ssuperior",
    "tsuperior",
    "ff",
    "ffi",
    "ffl",
    "parenleftinferior",
    "parenrightinferior",
    "Circumflexsmall",
    "hyphensuperior",
    "Gravesmall",
    "Asmall",
    "Bsmall",
    "Csmall",
    "Dsmall",
    "Esmall",
    "Fsmall",
    "Gsmall",
    "Hsmall",
    "Ismall",
    "Jsmall",
    "Ksmall",
    "Lsmall",
    "Msmall",
    "Nsmall",
    "Osmall",
    "Psmall",
    "Qsmall",
    "Rsmall",
    "Ssmall",
    "Tsmall",
    "Usmall",
    "Vsmall",
    "Wsmall",
    "Xsmall",
    "Ysmall",
    "Zsmall",
    "colonmonetary",
    "onefitted",
    "rupiah",
    "Tildesmall",
    "exclamdownsmall",
    "centoldstyle",
    "Lslashsmall",
    "Scaronsmall",
    "Zcaronsmall",
    "Dieresissmall",
    "Brevesmall",
    "Caronsmall",
    "Dotaccentsmall",
    "Macronsmall",
    "figuredash",
    "hypheninferior",
    "Ogoneksmall",
    "Ringsmall",
    "Cedillasmall",
    "questiondownsmall",
    "oneeighth",
    "threeeighths",
    "fiveeighths",
    "seveneighths",
    "onethird",
    "twothirds",
    "zerosuperior",
    "foursuperior",
    "fivesuperior",
    "sixsuperior",
    "sevensuperior",
    "eightsuperior",
    "ninesuperior",
    "zeroinferior",
    "oneinferior",
    "twoinferior",
    "threeinferior",
    "fourinferior",
    "fiveinferior",
    "sixinferior",
    "seveninferior",
    "eightinferior",
    "nineinferior",
    "centinferior",
    "dollarinferior",
    "periodinferior",
    "commainferior",
    "Agravesmall",
    "Aacutesmall",
    "Acircumflexsmall",
    "Atildesmall",
    "Adieresissmall",
    "Aringsmall",
    "AEsmall",
    "Ccedillasmall",
    "Egravesmall",
    "Eacutesmall",
    "Ecircumflexsmall",
    "Edieresissmall",
    "Igravesmall",
    "Iacutesmall",
    "Icircumflexsmall",
    "Idieresissmall",
    "Ethsmall",
    "Ntildesmall",
    "Ogravesmall",
    "Oacutesmall",
    "Ocircumflexsmall",
    "Otildesmall",
    "Odieresissmall",
    "OEsmall",
    "Oslashsmall",
    "Ugravesmall",
    "Uacutesmall",
    "Ucircumflexsmall",
    "Udieresissmall",
    "Yacutesmall",
    "Thornsmall",
    "Ydieresissmall",
    "001.000",
    "001.001",
    "001.002",
    "001.003",
    "Black",
    "Bold",
    "Book",
    "Light",
    "Medium",
    "Regular",
    "Roman",
    "Semibold",
};

PDFErrorOr<Vector<ByteString>> CFF::parse_strings(Reader& reader)
{
    // CFF spec "10 String Index"
    Vector<ByteString> strings;
    TRY(parse_index(reader, [&](ReadonlyBytes const& string) -> PDFErrorOr<void> {
        return TRY(strings.try_append(ByteString(string)));
    }));
    dbgln_if(CFF_DEBUG, "CFF has {} additional strings in string table", strings.size());
    return strings;
}

ByteString CFF::resolve_sid(SID sid, Vector<ByteString> const& strings)
{
    if (sid < s_cff_builtin_names.size())
        return s_cff_builtin_names[sid];

    if (sid - s_cff_builtin_names.size() < strings.size())
        return strings[sid - s_cff_builtin_names.size()];

    dbgln("Couldn't find string for SID {}, going with space", sid);
    return "space";
}

PDFErrorOr<Vector<ByteString>> CFF::parse_charset(Reader&& reader, size_t glyph_count, Vector<ByteString> const& strings)
{
    // CFF spec, "13 Charsets"
    Vector<ByteString> names;
    auto format = TRY(reader.try_read<Card8>());
    if (format == 0) {
        // CFF spec, "Table 17 Format 0"
        dbgln_if(CFF_DEBUG, "CFF charset format 0");
        for (size_t i = 0; i < glyph_count - 1; i++) {
            SID sid = TRY(reader.try_read<BigEndian<SID>>());
            TRY(names.try_append(resolve_sid(sid, strings)));
        }
    } else if (format == 1) {
        // CFF spec, "Table 18 Format 1"
        dbgln_if(CFF_DEBUG, "CFF charset format 1");
        while (names.size() < glyph_count - 1) {
            // CFF spec, "Table 19 Range1 Format (Charset)"
            auto first_sid = TRY(reader.try_read<BigEndian<SID>>());
            int left = TRY(reader.try_read<Card8>());
            for (SID sid = first_sid; left >= 0 && names.size() < glyph_count - 1; left--, sid++)
                TRY(names.try_append(resolve_sid(sid, strings)));
        }
    } else if (format == 2) {
        // CFF spec, "Table 20 Format 2"
        // "Format 2 differs from format 1 only in the size of the Left field in each range."
        dbgln_if(CFF_DEBUG, "CFF charset format 2");
        while (names.size() < glyph_count - 1) {
            // CFF spec, "Table 21 Range2 Format"
            auto first_sid = TRY(reader.try_read<BigEndian<SID>>());
            int left = TRY(reader.try_read<Card16>());
            for (SID sid = first_sid; left >= 0 && names.size() < glyph_count - 1; left--, sid++)
                TRY(names.try_append(resolve_sid(sid, strings)));
        }
    } else {
        dbgln("CFF: Unknown charset format {}", format);
    }
    return names;
}

PDFErrorOr<Vector<CFF::Glyph>> CFF::parse_charstrings(Reader&& reader, Vector<ByteBuffer> const& local_subroutines, Vector<ByteBuffer> const& global_subroutines)
{
    // CFF spec, "14 CharStrings INDEX"
    Vector<Glyph> glyphs;
    TRY(parse_index(reader, [&](ReadonlyBytes const& charstring_data) -> PDFErrorOr<void> {
        GlyphParserState state;
        auto glyph = TRY(parse_glyph(charstring_data, local_subroutines, global_subroutines, state, true));
        return TRY(glyphs.try_append(glyph));
    }));
    dbgln_if(CFF_DEBUG, "CFF has {} glyphs", glyphs.size());
    return glyphs;
}

PDFErrorOr<Vector<u8>> CFF::parse_encoding(Reader&& reader, HashMap<Card8, SID>& supplemental)
{
    // CFF spec, "12 Encodings"
    Vector<u8> encoding_codes;
    auto format_raw = TRY(reader.try_read<Card8>());

    auto format = format_raw & 0x7f;
    if (format == 0) {
        // CFF spec, "Table 11 Format 0"
        auto n_codes = TRY(reader.try_read<Card8>());
        dbgln_if(CFF_DEBUG, "CFF encoding format 0, {} codes", n_codes);
        for (u8 i = 0; i < n_codes; i++) {
            TRY(encoding_codes.try_append(TRY(reader.try_read<Card8>())));
        }
    } else if (format == 1) {
        // CFF spec, "Table 12 Format 1"
        auto n_ranges = TRY(reader.try_read<Card8>());
        dbgln_if(CFF_DEBUG, "CFF encoding format 1, {} ranges", n_ranges);
        for (u8 i = 0; i < n_ranges; i++) {
            // CFF spec, "Table 13 Range1 Format (Encoding)"
            auto first_code = TRY(reader.try_read<Card8>());
            int left = TRY(reader.try_read<Card8>());
            for (u8 code = first_code; left >= 0; left--, code++)
                TRY(encoding_codes.try_append(code));
        }
    } else
        return error(ByteString::formatted("Invalid encoding format: {}", format));

    if (format_raw & 0x80) {
        // CFF spec, "Table 14 Supplemental Encoding Data"
        auto n_sups = TRY(reader.try_read<Card8>());
        dbgln_if(CFF_DEBUG, "CFF encoding, {} supplemental entries", n_sups);
        for (u8 i = 0; i < n_sups; i++) {
            // CFF spec, "Table 15 Supplement Format"
            auto code = TRY(reader.try_read<Card8>());
            SID name = TRY(reader.try_read<SID>());
            TRY(supplemental.try_set(code, name));
        }
    }

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
    if (offset_size > 4)
        return error("CFF INDEX Data offset_size > 4 not supported");
    return parse_index_data(offset_size, count, reader, data_handler);
}

PDFErrorOr<void> CFF::parse_index_data(OffSize offset_size, Card16 count, Reader& reader, IndexDataHandler& handler)
{
    // CFF spec, "5 INDEX Data"
    u32 last_data_end = 1;

    auto read_offset = [&]() -> PDFErrorOr<u32> {
        u32 offset = 0;
        for (OffSize i = 0; i < offset_size; ++i)
            offset = (offset << 8) | TRY(reader.try_read<u8>());
        return offset;
    };

    auto offset_refpoint = reader.offset() + offset_size * (count + 1) - 1;
    for (u16 i = 0; i < count; i++) {
        reader.save();

        reader.move_by(offset_size * i);
        u32 data_start = TRY(read_offset());
        last_data_end = TRY(read_offset());

        auto data_size = last_data_end - data_start;
        reader.move_to(offset_refpoint + data_start);
        TRY(handler(reader.bytes().slice(reader.offset(), data_size)));
        reader.load();
    }
    reader.move_to(offset_refpoint + last_data_end);
    return {};
}

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
    return Error { Error::Type::MalformedPDF, ByteString::formatted("Unknown CFF dict element prefix: {}", b0) };
}
}
