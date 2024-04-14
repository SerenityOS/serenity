/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibPDF/Error.h>
#include <LibPDF/Fonts/Type1FontProgram.h>

namespace PDF {

// CFF spec: https://adobe-type-tools.github.io/font-tech-notes/pdfs/5176.CFF.pdf

class CFF : public Type1FontProgram {

private:
    // CFF spec, "Table 9 Top DICT Operator Entries"
    enum class TopDictOperator {
        Version = 0,
        Notice,
        FullName,
        FamilyName,
        Weight,
        FontBBox,
        UniqueID = 13,
        XUID,
        Charset = 15,
        Encoding,
        CharStrings,
        Private,
        Copyright = (12 << 8),
        IsFixedPitch,
        ItalicAngle,
        UnderlinePosition,
        UnderlineThickness,
        PaintType,
        CharstringType,
        FontMatrix,
        StrokeWidth,
        SyntheticBase = (12 << 8 | 20),
        PostScript,
        BaseFontName,
        BaseFontBlend,

        // CFF spec, "Table 10 CIDFont Operator Extensions"
        RegistryOrderingSupplement = (12 << 8 | 30),
        CIDFontVersion,
        CIDFontRevision,
        CIDFontType,
        CIDCount,
        UIDBase,
        FDArray,
        FDSelect,
        FontName,
    };

    // CFF spec, "Table 23 Private DICT Operators"
    enum class PrivDictOperator {
        BlueValues = 6,
        OtherBlues,
        FamilyBlues,
        FamilyOtherBlues,
        BlueScale = (12 << 8 | 9),
        BlueShift,
        BlueFuzz,
        StdHW = 10,
        StdVW,
        StemSnapH = (12 << 8 | 12),
        StemSnapV,
        ForceBold,
        LanguageGroup = (12 << 8 | 17),
        ExpansionFactor,
        InitialRandomSeed,
        Subrs = 19,
        DefaultWidthX,
        NominalWidthX,
    };

public:
    static ErrorOr<NonnullRefPtr<CFF>> create(ReadonlyBytes const&, RefPtr<Encoding> encoding);

    // to private
    using Card8 = u8;
    using Card16 = u16;
    using Offset = i32;
    using OffSize = u8;
    using SID = u16;
    using DictOperand = Variant<int, float>;

    static float to_number(DictOperand operand)
    {
        if (operand.has<int>())
            return operand.get<int>();
        return operand.get<float>();
    }

    static ErrorOr<int> load_int_dict_operand(u8 b0, Stream&);
    static ErrorOr<float> load_float_dict_operand(Stream&);
    static ErrorOr<DictOperand> load_dict_operand(u8, Stream&);

    using IndexDataHandler = Function<ErrorOr<void>(ReadonlyBytes const&)>;
    static ErrorOr<void> parse_index(FixedMemoryStream&, IndexDataHandler&&);

    static ErrorOr<void> parse_index_data(OffSize offset_size, Card16 count, FixedMemoryStream&, IndexDataHandler&);

    template<typename OperatorT>
    using DictEntryHandler = Function<ErrorOr<void>(OperatorT, Vector<DictOperand> const&)>;

    template<typename OperatorT>
    static ErrorOr<void> parse_dict(Stream&, DictEntryHandler<OperatorT>&& handler);

    template<typename OperatorT>
    static ErrorOr<OperatorT> parse_dict_operator(u8, Stream&);

    // CFF spec, "8 Top DICT INDEX"
    struct TopDict {
        int charset_offset = 0;
        int encoding_offset = 0;
        int charstrings_offset = 0;
        Vector<ByteBuffer> local_subroutines;
        Optional<float> defaultWidthX;
        Optional<float> nominalWidthX;
        bool is_cid_keyed = false;
        int fdselect_offset = 0;
        int fdarray_offset = 0;
    };
    static ErrorOr<Vector<TopDict>> parse_top_dicts(FixedMemoryStream&, ReadonlyBytes const& cff_bytes);

    static ErrorOr<Vector<StringView>> parse_strings(FixedMemoryStream&);

    static ErrorOr<Vector<CFF::Glyph>> parse_charstrings(FixedMemoryStream&&, Vector<ByteBuffer> const& local_subroutines, Vector<ByteBuffer> const& global_subroutines);

    static DeprecatedFlyString resolve_sid(SID, Vector<StringView> const&);
    static ErrorOr<Vector<SID>> parse_charset(Stream&&, size_t);
    static ErrorOr<Vector<u8>> parse_fdselect(Stream&&, size_t);
    static ErrorOr<Vector<u8>> parse_encoding(Stream&&, HashMap<Card8, SID>& supplemental);
};

}
