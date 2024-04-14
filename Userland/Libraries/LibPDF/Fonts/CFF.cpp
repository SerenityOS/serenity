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
#include <LibPDF/Fonts/CFF.h>

namespace PDF {

// The built-in encodings map codes to SIDs.

// CFF spec, "Appendix B Predefined Encodings, Standard Encoding"
// clang-format off
static constexpr Array s_predefined_encoding_standard = to_array<CFF::SID>({
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
});
static_assert(s_predefined_encoding_standard.size() == 256);
// clang-format on

// CFF spec, "Appendix B Predefined Encodings, Expert Encoding"
// clang-format off
static constexpr Array s_predefined_encoding_expert = to_array<CFF::SID>({
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
});
static_assert(s_predefined_encoding_expert.size() == 256);
// clang-format on

// Charsets map GIDs to SIDs.

// CFF spec, "Appendix C Predefined Charsets, Expert"
// clang-format off
static constexpr auto s_predefined_charset_expert = to_array<CFF::SID>({
      1, 229, 230, 231, 232,
    233, 234, 235, 236, 237,
    238,  13,  14,  15,  99,

    239, 240, 241, 242, 243, 244, 245, 246, 247, 248,  27,  28, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 109, 110,
    267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298,
    299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 158, 155, 163, 319, 320, 321, 322, 323, 324, 325, 326, 150,

    164, 169, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342,
    343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360,
    361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378,
});
// clang-format on

// CFF spec, "Appendix C Predefined Charsets, Expert Subset"
// clang-format off
static constexpr auto s_predefined_charset_expert_subset = to_array<CFF::SID>({
      1, 231, 232, 235, 236, 237, 238,  13,  14,  15,  99,
    239, 240, 241, 242, 243, 244, 245, 246, 247, 248,  27,
     28, 249, 250, 251, 253, 254, 255, 256, 257, 258, 259,

    260, 261, 262, 263, 264, 265, 266, 109, 110, 267, 268, 269, 270, 272, 300, 301, 302, 305,
    314, 315, 158, 155, 163, 320, 321, 322, 323, 324, 325, 326, 150, 164, 169, 327, 328, 329,
    330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346,
});
// clang-format on

ErrorOr<NonnullRefPtr<CFF>> CFF::create(ReadonlyBytes const& cff_bytes, RefPtr<Encoding> encoding)
{
    FixedMemoryStream reader(cff_bytes);

    // CFF spec, "6 Header"
    // skip major, minor version
    TRY(reader.discard(2));
    auto header_size = TRY(reader.read_value<Card8>());
    // skip offset size
    TRY(reader.discard(1));
    TRY(reader.seek(header_size));

    // CFF spec, "7 Name INDEX"
    Vector<String> font_names;
    TRY(parse_index(reader, [&](ReadonlyBytes const& data) -> ErrorOr<void> {
        auto font_name = TRY(String::from_utf8(data));
        dbgln_if(CFF_DEBUG, "CFF font name '{}'", font_name);
        return TRY(font_names.try_append(font_name));
    }));

    if (font_names.size() != 1)
        return AK::Error::from_string_literal("CFFs with more than one font not yet implemented");

    auto cff = adopt_ref(*new CFF());
    cff->set_font_matrix({ 0.001f, 0.0f, 0.0f, 0.001f, 0.0f, 0.0f });

    auto top_dicts = TRY(parse_top_dicts(reader, cff_bytes));
    if (top_dicts.size() != 1)
        return AK::Error::from_string_literal("CFFs with more than one font not yet implemented");
    auto const& top_dict = top_dicts[0];

    if (top_dict.is_cid_keyed) {
        // CFF spec, "18 CID-keyed Fonts"
        // "* The FDArray operator is expected to be present"
        if (top_dict.fdarray_offset == 0)
            return AK::Error::from_string_literal("CID-keyed CFFs must have an FDArray");

        // "* The charset data, although in the same format as non-CIDFonts, will represent CIDs rather than SIDs"
        // (Done below.)

        // "* The Top DICT will include an FDSelect operator"
        if (top_dict.fdselect_offset == 0)
            return AK::Error::from_string_literal("CID-keyed CFFs must have FDSelect");

        // "* no Encoding operator will be present and the default StandardEncoding should not be applied"
        if (top_dict.encoding_offset != 0)
            return AK::Error::from_string_literal("CID-keyed CFFs must not have Encoding");

        cff->set_kind(CFF::Kind::CIDKeyed);
    }

    auto strings = TRY(parse_strings(reader));

    // CFF spec "16 Local/Global Subrs INDEXes"
    // "Global subrs are stored in an INDEX structure which follows the String INDEX."
    Vector<ByteBuffer> global_subroutines;
    TRY(parse_index(reader, [&](ReadonlyBytes const& subroutine_bytes) -> ErrorOr<void> {
        return TRY(global_subroutines.try_append(TRY(ByteBuffer::copy(subroutine_bytes))));
    }));
    dbgln_if(CFF_DEBUG, "CFF has {} gsubr entries", global_subroutines.size());

    // Create glyphs (now that we have the subroutines) and associate missing information to store them and their encoding
    auto glyphs = TRY(parse_charstrings(FixedMemoryStream(cff_bytes.slice(top_dict.charstrings_offset)), top_dict.local_subroutines, global_subroutines));

    // CFF spec, "Table 16 Encoding ID"
    // FIXME: Only read this if the built-in encoding is actually needed? (ie. `if (!encoding)`)
    Vector<u8> encoding_codes;                 // Maps GID to its codepoint.
    HashMap<Card8, SID> encoding_supplemental; // Maps codepoint to SID.
    if (!top_dict.is_cid_keyed) {
        switch (top_dict.encoding_offset) {
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
            encoding_codes = TRY(parse_encoding(FixedMemoryStream(cff_bytes.slice(top_dict.encoding_offset)), encoding_supplemental));
            break;
        }
    }

    // CFF spec, "Table 22 Charset ID"
    Vector<SID> charset;                       // Maps GID to CIDs for CID-keyed, to SIDs otherwise.
    Vector<DeprecatedFlyString> charset_names; // Only valid for non-CID-keyed fonts.
    if (top_dict.is_cid_keyed) {
        charset = TRY(parse_charset(FixedMemoryStream { cff_bytes.slice(top_dict.charset_offset) }, glyphs.size()));
    } else {
        switch (top_dict.charset_offset) {
        case 0:
            dbgln_if(CFF_DEBUG, "CFF predefined charset ISOAdobe");
            // CFF spec, "Appendix C Predefined Charsets, ISOAdobe"
            for (SID sid = 1; sid <= 228; sid++)
                TRY(charset_names.try_append(resolve_sid(sid, strings)));
            break;
        case 1:
            dbgln_if(CFF_DEBUG, "CFF predefined charset Expert");
            for (SID sid : s_predefined_charset_expert)
                TRY(charset_names.try_append(resolve_sid(sid, strings)));
            break;
        case 2:
            dbgln_if(CFF_DEBUG, "CFF predefined charset Expert Subset");
            for (SID sid : s_predefined_charset_expert_subset)
                TRY(charset_names.try_append(resolve_sid(sid, strings)));
            break;
        default: {
            charset = TRY(parse_charset(FixedMemoryStream { cff_bytes.slice(top_dict.charset_offset) }, glyphs.size()));
            for (SID sid : charset)
                TRY(charset_names.try_append(resolve_sid(sid, strings)));
            break;
        }
        }
    }

    // CFF spec, "18 CID-keyed Fonts"
    Vector<TopDict> font_dicts;
    if (top_dict.fdarray_offset != 0) {
        FixedMemoryStream fdarray_reader { cff_bytes.slice(top_dict.fdarray_offset) };
        font_dicts = TRY(parse_top_dicts(fdarray_reader, cff_bytes));
        dbgln_if(CFF_DEBUG, "CFF has {} FDArray entries", font_dicts.size());
    }

    // CFF spec, "19 FDSelect"
    Vector<u8> fdselect;
    if (top_dict.fdselect_offset != 0) {
        fdselect = TRY(parse_fdselect(FixedMemoryStream { cff_bytes.slice(top_dict.fdselect_offset) }, glyphs.size()));
        dbgln_if(CFF_DEBUG, "CFF has {} FDSelect entries", fdselect.size());
    }

    // Adjust glyphs' widths as they are deltas from nominalWidthX
    for (size_t glyph_id = 0; glyph_id < glyphs.size(); glyph_id++) {
        auto& glyph = glyphs[glyph_id];
        if (!glyph.has_width()) {
            auto default_width = top_dict.defaultWidthX.value_or(0.0f);
            if (top_dict.is_cid_keyed)
                default_width = font_dicts[fdselect[glyph_id]].defaultWidthX.value_or(default_width);
            glyph.set_width(default_width);
        } else {
            auto nominal_width = top_dict.nominalWidthX.value_or(0.0f);
            if (top_dict.is_cid_keyed)
                nominal_width = font_dicts[fdselect[glyph_id]].nominalWidthX.value_or(nominal_width);
            glyph.set_width(glyph.width() + nominal_width);
        }
    }

    for (size_t i = 0; i < glyphs.size(); i++) {
        if (i == 0) {
            if (top_dict.is_cid_keyed) {
                // FIXME: Do better than printing the cid to a string.
                auto cid = 0;
                TRY(cff->add_glyph(ByteString::formatted("{}", cid), move(glyphs[0])));
            } else {
                TRY(cff->add_glyph(".notdef", move(glyphs[0])));
            }
            continue;
        }
        if (top_dict.is_cid_keyed) {
            // FIXME: Do better than printing the cid to a string.
            auto cid = charset[i - 1];
            TRY(cff->add_glyph(ByteString::formatted("{}", cid), move(glyphs[i])));
        } else {
            auto const& name = charset_names[i - 1];
            TRY(cff->add_glyph(name, move(glyphs[i])));
        }
    }
    cff->consolidate_glyphs();

    // Encoding given or read
    if (encoding) {
        dbgln_if(CFF_DEBUG, "CFF using external encoding");
        cff->set_encoding(move(encoding));
    } else if (!top_dict.is_cid_keyed) {
        dbgln_if(CFF_DEBUG, "CFF using embedded encoding");
        auto encoding = Encoding::create();
        for (size_t i = 0; i < glyphs.size(); i++) {
            if (i == 0) {
                encoding->set(0, ".notdef");
                continue;
            }
            if (i - 1 >= encoding_codes.size() || i - 1 >= charset_names.size()) {
                dbgln("CFF: No encoding for glyph {} onwards, encoding_codes size {} charset_names size {}", i, encoding_codes.size(), charset_names.size());
                break;
            }
            auto code = encoding_codes[i - 1];
            auto char_name = charset_names[i - 1];
            encoding->set(code, char_name);
        }
        for (auto const& entry : encoding_supplemental)
            encoding->set(entry.key, resolve_sid(entry.value, strings));
        cff->set_encoding(move(encoding));
    }

    return cff;
}

ErrorOr<Vector<CFF::TopDict>> CFF::parse_top_dicts(FixedMemoryStream& reader, ReadonlyBytes const& cff_bytes)
{
    Vector<TopDict> top_dicts;

    TRY(parse_index(reader, [&](ReadonlyBytes const& element_data) -> ErrorOr<void> {
        TopDict top_dict;

        FixedMemoryStream element_reader { element_data };
        TRY(parse_dict<TopDictOperator>(element_reader, [&](TopDictOperator op, Vector<DictOperand> const& operands) -> ErrorOr<void> {
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
                    top_dict.encoding_offset = operands[0].get<int>();
                break;
            }
            case TopDictOperator::Charset: {
                if (!operands.is_empty())
                    top_dict.charset_offset = operands[0].get<int>();
                break;
            }
            case TopDictOperator::CharStrings: {
                if (!operands.is_empty())
                    top_dict.charstrings_offset = operands[0].get<int>();
                break;
            }
            case TopDictOperator::Private: {
                auto private_dict_size = operands[0].get<int>();
                auto private_dict_offset = operands[1].get<int>();
                FixedMemoryStream priv_dict_reader { cff_bytes.slice(private_dict_offset, private_dict_size) };
                TRY(parse_dict<PrivDictOperator>(priv_dict_reader, [&](PrivDictOperator op, Vector<DictOperand> const& operands) -> ErrorOr<void> {
                    switch (op) {
                    case PrivDictOperator::BlueValues:
                    case PrivDictOperator::OtherBlues:
                    case PrivDictOperator::FamilyBlues:
                    case PrivDictOperator::FamilyOtherBlues:
                    case PrivDictOperator::BlueScale:
                    case PrivDictOperator::BlueShift:
                    case PrivDictOperator::BlueFuzz:
                    case PrivDictOperator::StemSnapH:
                    case PrivDictOperator::StemSnapV:
                    case PrivDictOperator::ForceBold:
                    case PrivDictOperator::LanguageGroup:
                    case PrivDictOperator::ExpansionFactor:
                    case PrivDictOperator::InitialRandomSeed:
                        // Ignore hinting-related operators for now.
                        break;
                    case PrivDictOperator::StdHW:
                    case PrivDictOperator::StdVW:
                        // FIXME: What do these do?
                        break;
                    case PrivDictOperator::Subrs: {
                        // CFF spec, "16 Local/Global Subrs INDEXes"
                        // "Local subrs are stored in an INDEX structure which is located via the offset operand of the Subrs operator in the Private DICT."
                        auto subrs_offset = operands[0].get<int>();
                        FixedMemoryStream subrs_reader { cff_bytes.slice(private_dict_offset + subrs_offset) };
                        TRY(parse_index(subrs_reader, [&](ReadonlyBytes const& subroutine_bytes) -> ErrorOr<void> {
                            return TRY(top_dict.local_subroutines.try_append(TRY(ByteBuffer::copy(subroutine_bytes))));
                        }));
                        dbgln_if(CFF_DEBUG, "CFF has {} subr entries", top_dict.local_subroutines.size());
                        break;
                    }
                    case PrivDictOperator::DefaultWidthX:
                        if (!operands.is_empty())
                            top_dict.defaultWidthX = to_number(operands[0]);
                        break;
                    case PrivDictOperator::NominalWidthX:
                        if (!operands.is_empty())
                            top_dict.nominalWidthX = to_number(operands[0]);
                        break;
                    default:
                        dbgln("CFF: Unhandled private dict entry {}", static_cast<int>(op));
                    }
                    return {};
                }));
                break;
            }
            case TopDictOperator::RegistryOrderingSupplement:
                // CFF Spec, "18 CID-keyed Fonts"
                // "The Top DICT begins with ROS operator which specifies the Registry-Ordering-Supplement for the font.
                //  This will indicate to a CFF parser that special CID processing should be applied to this font."
                top_dict.is_cid_keyed = true;
                break;
            case TopDictOperator::FDSelect:
                if (!operands.is_empty())
                    top_dict.fdselect_offset = operands[0].get<int>();
                break;
            case TopDictOperator::FDArray:
                if (!operands.is_empty())
                    top_dict.fdarray_offset = operands[0].get<int>();
                break;
            case TopDictOperator::CIDFontVersion:
            case TopDictOperator::CIDFontRevision:
            case TopDictOperator::CIDFontType:
            case TopDictOperator::CIDCount:
            case TopDictOperator::UIDBase:
            case TopDictOperator::FontName:
                // Keys for CID-keyed fonts that we don't need, at least at the moment.
                break;
            default:
                dbgln("CFF: Unhandled top dict entry {}", static_cast<int>(op));
            }
            return {};
        }));

        top_dicts.append((move(top_dict)));
        return {};
    }));

    return top_dicts;
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

ErrorOr<Vector<StringView>> CFF::parse_strings(FixedMemoryStream& reader)
{
    // CFF spec "10 String Index"
    Vector<StringView> strings;
    TRY(parse_index(reader, [&](ReadonlyBytes const& string) -> ErrorOr<void> {
        return TRY(strings.try_append(string));
    }));
    dbgln_if(CFF_DEBUG, "CFF has {} additional strings in string table", strings.size());
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

ErrorOr<Vector<CFF::SID>> CFF::parse_charset(Stream&& reader, size_t glyph_count)
{
    // CFF spec, "13 Charsets"

    // Maps `GID - 1` to a SID (or CID, for CID-keyed fonts). The name of GID 0 is implicitly ".notdef".
    Vector<SID> names;
    auto format = TRY(reader.read_value<Card8>());
    if (format == 0) {
        // CFF spec, "Table 17 Format 0"
        dbgln_if(CFF_DEBUG, "CFF charset format 0");
        for (size_t i = 0; i < glyph_count - 1; i++) {
            SID sid = TRY(reader.read_value<BigEndian<SID>>());
            TRY(names.try_append(sid));
        }
    } else if (format == 1) {
        // CFF spec, "Table 18 Format 1"
        dbgln_if(CFF_DEBUG, "CFF charset format 1");
        while (names.size() < glyph_count - 1) {
            // CFF spec, "Table 19 Range1 Format (Charset)"
            auto first_sid = TRY(reader.read_value<BigEndian<SID>>());
            int left = TRY(reader.read_value<Card8>());
            for (SID sid = first_sid; left >= 0; left--, sid++)
                TRY(names.try_append(sid));
        }
    } else if (format == 2) {
        // CFF spec, "Table 20 Format 2"
        // "Format 2 differs from format 1 only in the size of the Left field in each range."
        dbgln_if(CFF_DEBUG, "CFF charset format 2");
        while (names.size() < glyph_count - 1) {
            // CFF spec, "Table 21 Range2 Format"
            auto first_sid = TRY(reader.read_value<BigEndian<SID>>());
            int left = TRY(reader.read_value<BigEndian<Card16>>());
            for (SID sid = first_sid; left >= 0; left--, sid++)
                TRY(names.try_append(sid));
        }
    } else {
        dbgln("CFF: Unknown charset format {}", format);
    }
    return names;
}

ErrorOr<Vector<u8>> CFF::parse_fdselect(Stream&& reader, size_t glyph_count)
{
    Vector<u8> fd_selector_array; // Maps GIDs to their FD index.

    // CFF spec, "19 FDSelect"
    auto format = TRY(reader.read_value<Card8>());

    if (format == 0) {
        // CFF spec, "Table 27 Format 0"
        // "(This format is identical to charset format 0 except that the notdef glyph is included in this case.)"
        dbgln_if(CFF_DEBUG, "CFF fdselect format 0");
        fd_selector_array.ensure_capacity(glyph_count);
        for (size_t i = 0; i < glyph_count; i++)
            fd_selector_array.append(TRY(reader.read_value<Card8>()));
    } else if (format == 3) {
        // CFF spec, "Table 28 Format 3"
        dbgln_if(CFF_DEBUG, "CFF fdselect format 3");

        // The spec presents this as n "Card16 first; Card8 fd;" struct entries followed by a Char16 sentinel value.
        // But the code is shorter if we treat it as a Char16 start value followed by n "Card8 fd; Card16 end;" struct entries.
        Card16 n_ranges = TRY(reader.read_value<BigEndian<Card16>>());
        Card16 begin = TRY(reader.read_value<BigEndian<Card16>>());

        // "The first range must have a 'first' GID of 0."
        if (begin != 0)
            return AK::Error::from_string_literal("CFF fdselect format 3 first range must have a 'first' GID of 0");

        for (Card16 i = 0; i < n_ranges; i++) {
            auto fd = TRY(reader.read_value<Card8>());
            auto end = TRY(reader.read_value<BigEndian<Card16>>());
            for (Card16 j = begin; j < end; j++)
                fd_selector_array.append(fd);
            begin = end;
        }

        // "The sentinel GID is set equal to the number of glyphs in the font."
        if (begin != glyph_count)
            return AK::Error::from_string_literal("CFF fdselect format 3 last range must end at the number of glyphs in the font");
    } else {
        dbgln("CFF: Unknown fdselect format {}", format);
    }

    return fd_selector_array;
}

ErrorOr<Vector<CFF::Glyph>> CFF::parse_charstrings(FixedMemoryStream&& reader, Vector<ByteBuffer> const& local_subroutines, Vector<ByteBuffer> const& global_subroutines)
{
    // CFF spec, "14 CharStrings INDEX"
    Vector<Glyph> glyphs;
    TRY(parse_index(reader, [&](ReadonlyBytes const& charstring_data) -> ErrorOr<void> {
        GlyphParserState state;
        auto glyph = TRY(parse_glyph(charstring_data, local_subroutines, global_subroutines, state, true));
        return TRY(glyphs.try_append(glyph));
    }));
    dbgln_if(CFF_DEBUG, "CFF has {} glyphs", glyphs.size());
    return glyphs;
}

ErrorOr<Vector<u8>> CFF::parse_encoding(Stream&& reader, HashMap<Card8, SID>& supplemental)
{
    // CFF spec, "12 Encodings"
    Vector<u8> encoding_codes;
    auto format_raw = TRY(reader.read_value<Card8>());

    auto format = format_raw & 0x7f;
    if (format == 0) {
        // CFF spec, "Table 11 Format 0"
        auto n_codes = TRY(reader.read_value<Card8>());
        dbgln_if(CFF_DEBUG, "CFF encoding format 0, {} codes", n_codes);
        for (u8 i = 0; i < n_codes; i++) {
            TRY(encoding_codes.try_append(TRY(reader.read_value<Card8>())));
        }
    } else if (format == 1) {
        // CFF spec, "Table 12 Format 1"
        auto n_ranges = TRY(reader.read_value<Card8>());
        dbgln_if(CFF_DEBUG, "CFF encoding format 1, {} ranges", n_ranges);
        for (u8 i = 0; i < n_ranges; i++) {
            // CFF spec, "Table 13 Range1 Format (Encoding)"
            auto first_code = TRY(reader.read_value<Card8>());
            int left = TRY(reader.read_value<Card8>());
            for (u8 code = first_code; left >= 0; left--, code++)
                TRY(encoding_codes.try_append(code));
        }
    } else {
        dbgln("Invalid encoding format: {}", format);
        return AK::Error::from_string_literal("Invalid encoding format");
    }

    if (format_raw & 0x80) {
        // CFF spec, "Table 14 Supplemental Encoding Data"
        auto n_sups = TRY(reader.read_value<Card8>());
        dbgln_if(CFF_DEBUG, "CFF encoding, {} supplemental entries", n_sups);
        for (u8 i = 0; i < n_sups; i++) {
            // CFF spec, "Table 15 Supplement Format"
            auto code = TRY(reader.read_value<Card8>());
            SID name = TRY(reader.read_value<SID>());
            TRY(supplemental.try_set(code, name));
        }
    }

    return encoding_codes;
}

template<typename OperatorT>
ErrorOr<void> CFF::parse_dict(Stream& reader, DictEntryHandler<OperatorT>&& handler)
{
    // CFF spec, "4 DICT data"
    Vector<DictOperand> operands;
    while (!reader.is_eof()) {
        auto b0 = TRY(reader.read_value<u8>());
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

template ErrorOr<void> CFF::parse_dict<CFF::TopDictOperator>(Stream&, DictEntryHandler<TopDictOperator>&&);
template ErrorOr<void> CFF::parse_dict<CFF::PrivDictOperator>(Stream&, DictEntryHandler<PrivDictOperator>&&);

template<typename OperatorT>
ErrorOr<OperatorT> CFF::parse_dict_operator(u8 b0, Stream& reader)
{
    // CFF spec, "4 DICT data"
    VERIFY(b0 <= 21);

    // "Two-byte operators have an initial escape byte of 12."
    if (b0 != 12)
        return OperatorT { (int)b0 };
    auto b1 = TRY(reader.read_value<u8>());
    return OperatorT { b0 << 8 | b1 };
}

template ErrorOr<CFF::TopDictOperator> CFF::parse_dict_operator(u8, Stream&);

ErrorOr<void> CFF::parse_index(FixedMemoryStream& reader, IndexDataHandler&& data_handler)
{
    // CFF spec, "5 INDEX Data"
    Card16 count = TRY(reader.read_value<BigEndian<Card16>>());
    if (count == 0)
        return {};
    auto offset_size = TRY(reader.read_value<OffSize>());
    if (offset_size > 4)
        return AK::Error::from_string_literal("CFF INDEX Data offset_size > 4 not supported");
    return parse_index_data(offset_size, count, reader, data_handler);
}

ErrorOr<void> CFF::parse_index_data(OffSize offset_size, Card16 count, FixedMemoryStream& reader, IndexDataHandler& handler)
{
    // CFF spec, "5 INDEX Data"
    u32 last_data_end = 1;

    auto read_offset = [&]() -> ErrorOr<u32> {
        u32 offset = 0;
        for (OffSize i = 0; i < offset_size; ++i)
            offset = (offset << 8) | TRY(reader.read_value<u8>());
        return offset;
    };

    auto offset_refpoint = reader.offset() + offset_size * (count + 1) - 1;
    for (u16 i = 0; i < count; i++) {
        auto saved_offset = TRY(reader.tell());

        TRY(reader.seek(offset_size * i, SeekMode::FromCurrentPosition));
        u32 data_start = TRY(read_offset());
        last_data_end = TRY(read_offset());

        auto data_size = last_data_end - data_start;
        TRY(reader.seek(offset_refpoint + data_start));
        TRY(handler(TRY(reader.read_in_place<u8 const>(data_size))));
        TRY(reader.seek(saved_offset));
    }
    TRY(reader.seek(offset_refpoint + last_data_end));
    return {};
}

ErrorOr<int> CFF::load_int_dict_operand(u8 b0, Stream& reader)
{
    // CFF spec, "Table 3 Operand Encoding"
    if (b0 >= 32 && b0 <= 246) {
        return b0 - 139;
    }
    if (b0 >= 247 && b0 <= 250) {
        auto b1 = TRY(reader.read_value<u8>());
        return (b0 - 247) * 256 + b1 + 108;
    }
    if (b0 >= 251 && b0 <= 254) {
        auto b1 = TRY(reader.read_value<u8>());
        return -(b0 - 251) * 256 - b1 - 108;
    }
    if (b0 == 28) {
        auto b1 = TRY(reader.read_value<u8>());
        auto b2 = TRY(reader.read_value<u8>());
        return b1 << 8 | b2;
    }
    if (b0 == 29) {
        auto b1 = TRY(reader.read_value<u8>());
        auto b2 = TRY(reader.read_value<u8>());
        auto b3 = TRY(reader.read_value<u8>());
        auto b4 = TRY(reader.read_value<u8>());
        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<float> CFF::load_float_dict_operand(Stream& reader)
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
        auto byte = TRY(reader.read_value<u8>());
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

ErrorOr<CFF::DictOperand> CFF::load_dict_operand(u8 b0, Stream& reader)
{
    // CFF spec, "4 DICT data"
    if (b0 == 30)
        return DictOperand { TRY(load_float_dict_operand(reader)) };
    if (b0 >= 28)
        return DictOperand { TRY(load_int_dict_operand(b0, reader)) };
    dbgln("Unknown CFF dict element prefix: {}", b0);
    return AK::Error::from_string_literal("Unknown CFF dict element prefix");
}

}
