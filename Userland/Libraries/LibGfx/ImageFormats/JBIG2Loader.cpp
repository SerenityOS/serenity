/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Utf16View.h>
#include <LibGfx/ImageFormats/CCITTDecoder.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>
#include <LibTextCodec/Decoder.h>

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// Annex H has a datastream example.

namespace Gfx {

// JBIG2 spec, Annex D, D.4.1 ID string
static constexpr u8 id_string[] = { 0x97, 0x4A, 0x42, 0x32, 0x0D, 0x0A, 0x1A, 0x0A };

// 7.3 Segment types
enum SegmentType {
    SymbolDictionary = 0,
    IntermediateTextRegion = 4,
    ImmediateTextRegion = 6,
    ImmediateLosslessTextRegion = 7,
    PatternDictionary = 16,
    IntermediateHalftoneRegion = 20,
    ImmediateHalftoneRegion = 22,
    ImmediateLosslessHalftoneRegion = 23,
    IntermediateGenericRegion = 36,
    ImmediateGenericRegion = 38,
    ImmediateLosslessGenericRegion = 39,
    IntermediateGenericRefinementRegion = 40,
    ImmediateGenericRefinementRegion = 42,
    ImmediateLosslessGenericRefinementRegion = 43,
    PageInformation = 48,
    EndOfPage = 49,
    EndOfStripe = 50,
    EndOfFile = 51,
    Profiles = 52,
    Tables = 53,
    ColorPalette = 54,
    Extension = 62,
};

// Annex D
enum class Organization {
    // D.1 Sequential organization
    Sequential,

    // D.2 Random-access organization
    RandomAccess,

    // D.3 Embedded organization
    Embedded,
};

struct SegmentHeader {
    u32 segment_number;
    SegmentType type;
    Vector<u32> referred_to_segment_numbers;

    // 7.2.6 Segment page association
    // "The first page must be numbered "1". This field may contain a value of zero; this value indicates that this segment is not associated with any page."
    u32 page_association;

    Optional<u32> data_length;
};

struct SegmentData {
    SegmentHeader header;
    ReadonlyBytes data;
};

class BitBuffer {
public:
    static ErrorOr<NonnullOwnPtr<BitBuffer>> create(size_t width, size_t height);
    bool get_bit(size_t x, size_t y) const;
    void set_bit(size_t x, size_t y, bool b);
    void fill(bool b);

    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> to_gfx_bitmap() const;
    ErrorOr<ByteBuffer> to_byte_buffer() const;

private:
    BitBuffer(ByteBuffer, size_t width, size_t height, size_t pitch);

    ByteBuffer m_bits;
    size_t m_width;
    size_t m_height;
    size_t m_pitch;
};

ErrorOr<NonnullOwnPtr<BitBuffer>> BitBuffer::create(size_t width, size_t height)
{
    size_t pitch = ceil_div(width, 8ull);
    auto bits = TRY(ByteBuffer::create_uninitialized(pitch * height));
    return adopt_nonnull_own_or_enomem(new (nothrow) BitBuffer(move(bits), width, height, pitch));
}

bool BitBuffer::get_bit(size_t x, size_t y) const
{
    VERIFY(x < m_width);
    VERIFY(y < m_height);
    size_t byte_offset = x / 8;
    size_t bit_offset = x % 8;
    u8 byte = m_bits[y * m_pitch + byte_offset];
    byte = (byte >> (8 - 1 - bit_offset)) & 1;
    return byte != 0;
}

void BitBuffer::set_bit(size_t x, size_t y, bool b)
{
    VERIFY(x < m_width);
    VERIFY(y < m_height);
    size_t byte_offset = x / 8;
    size_t bit_offset = x % 8;
    u8 byte = m_bits[y * m_pitch + byte_offset];
    u8 mask = 1u << (8 - 1 - bit_offset);
    if (b)
        byte |= mask;
    else
        byte &= ~mask;
    m_bits[y * m_pitch + byte_offset] = byte;
}

void BitBuffer::fill(bool b)
{
    u8 fill_byte = b ? 0xff : 0;
    for (auto& byte : m_bits.bytes())
        byte = fill_byte;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> BitBuffer::to_gfx_bitmap() const
{
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { m_width, m_height }));
    for (size_t y = 0; y < m_height; ++y) {
        for (size_t x = 0; x < m_width; ++x) {
            auto color = get_bit(x, y) ? Color::Black : Color::White;
            bitmap->set_pixel(x, y, color);
        }
    }
    return bitmap;
}

ErrorOr<ByteBuffer> BitBuffer::to_byte_buffer() const
{
    return ByteBuffer::copy(m_bits);
}

BitBuffer::BitBuffer(ByteBuffer bits, size_t width, size_t height, size_t pitch)
    : m_bits(move(bits))
    , m_width(width)
    , m_height(height)
    , m_pitch(pitch)
{
}

// 7.4.8.5 Page segment flags
enum class CombinationOperator {
    Or = 0,
    And = 1,
    Xor = 2,
    XNor = 3,
};

struct Page {
    IntSize size;
    CombinationOperator default_combination_operator;
    OwnPtr<BitBuffer> bits;
};

struct JBIG2LoadingContext {
    enum class State {
        NotDecoded = 0,
        Error,
        Decoded,
    };
    State state { State::NotDecoded };

    Organization organization { Organization::Sequential };
    Page page;

    Optional<u32> number_of_pages;

    Vector<SegmentData> segments;
};

static ErrorOr<void> decode_jbig2_header(JBIG2LoadingContext& context, ReadonlyBytes data)
{
    if (!JBIG2ImageDecoderPlugin::sniff(data))
        return Error::from_string_literal("JBIG2LoadingContext: Invalid JBIG2 header");

    FixedMemoryStream stream(data.slice(sizeof(id_string)));

    // D.4.2 File header flags
    u8 header_flags = TRY(stream.read_value<u8>());
    if (header_flags & 0b11110000)
        return Error::from_string_literal("JBIG2LoadingContext: Invalid header flags");
    context.organization = (header_flags & 1) ? Organization::Sequential : Organization::RandomAccess;
    dbgln_if(JBIG2_DEBUG, "JBIG2LoadingContext: Organization: {} ({})", (int)context.organization, context.organization == Organization::Sequential ? "Sequential" : "Random-access");
    bool has_known_number_of_pages = (header_flags & 2) ? false : true;
    bool uses_templates_with_12_AT_pixels = (header_flags & 4) ? true : false;
    bool contains_colored_region_segments = (header_flags & 8) ? true : false;

    // FIXME: Do something with these?
    (void)uses_templates_with_12_AT_pixels;
    (void)contains_colored_region_segments;

    // D.4.3 Number of pages
    if (has_known_number_of_pages) {
        context.number_of_pages = TRY(stream.read_value<BigEndian<u32>>());
        dbgln_if(JBIG2_DEBUG, "JBIG2LoadingContext: Number of pages: {}", context.number_of_pages.value());
    }

    return {};
}

static ErrorOr<SegmentHeader> decode_segment_header(SeekableStream& stream)
{
    // 7.2.2 Segment number
    u32 segment_number = TRY(stream.read_value<BigEndian<u32>>());
    dbgln_if(JBIG2_DEBUG, "Segment number: {}", segment_number);

    // 7.2.3 Segment header flags
    u8 flags = TRY(stream.read_value<u8>());
    SegmentType type = static_cast<SegmentType>(flags & 0b11'1111);
    dbgln_if(JBIG2_DEBUG, "Segment type: {}", (int)type);
    bool segment_page_association_size_is_32_bits = (flags & 0b100'0000) != 0;
    bool segment_retained_only_by_itself_and_extension_segments = (flags & 0b1000'00000) != 0;

    // FIXME: Do something with these.
    (void)segment_page_association_size_is_32_bits;
    (void)segment_retained_only_by_itself_and_extension_segments;

    // 7.2.4 Referred-to segment count and retention flags
    u8 referred_to_segment_count_and_retention_flags = TRY(stream.read_value<u8>());
    u32 count_of_referred_to_segments = referred_to_segment_count_and_retention_flags >> 5;
    if (count_of_referred_to_segments == 5 || count_of_referred_to_segments == 6)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid count_of_referred_to_segments");
    u32 extra_count = 0;
    if (count_of_referred_to_segments == 7) {
        TRY(stream.seek(-1, SeekMode::FromCurrentPosition));
        count_of_referred_to_segments = TRY(stream.read_value<BigEndian<u32>>()) & 0x1FFF'FFFF;
        extra_count = ceil_div(count_of_referred_to_segments + 1, 8);
        TRY(stream.seek(extra_count, SeekMode::FromCurrentPosition));
    }
    dbgln_if(JBIG2_DEBUG, "Referred-to segment count: {}", count_of_referred_to_segments);

    // 7.2.5 Referred-to segment numbers
    Vector<u32> referred_to_segment_numbers;
    for (u32 i = 0; i < count_of_referred_to_segments; ++i) {
        u32 referred_to_segment_number;
        if (segment_number <= 256)
            referred_to_segment_number = TRY(stream.read_value<u8>());
        else if (segment_number <= 65536)
            referred_to_segment_number = TRY(stream.read_value<BigEndian<u16>>());
        else
            referred_to_segment_number = TRY(stream.read_value<BigEndian<u32>>());
        referred_to_segment_numbers.append(referred_to_segment_number);
        dbgln_if(JBIG2_DEBUG, "Referred-to segment number: {}", referred_to_segment_number);
    }

    // 7.2.6 Segment page association
    u32 segment_page_association;
    if (segment_page_association_size_is_32_bits) {
        segment_page_association = TRY(stream.read_value<BigEndian<u32>>());
    } else {
        segment_page_association = TRY(stream.read_value<u8>());
    }
    dbgln_if(JBIG2_DEBUG, "Segment page association: {}", segment_page_association);

    // 7.2.7 Segment data length
    u32 data_length = TRY(stream.read_value<BigEndian<u32>>());
    dbgln_if(JBIG2_DEBUG, "Segment data length: {}", data_length);

    // FIXME: Add some validity checks:
    // - check type is valid
    // - check referred_to_segment_numbers are smaller than segment_number
    // - 7.3.1 Rules for segment references
    // - 7.3.2 Rules for page associations

    Optional<u32> opt_data_length;
    if (data_length != 0xffff'ffff)
        opt_data_length = data_length;
    else if (type != ImmediateGenericRegion)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unknown data length only allowed for ImmediateGenericRegion");

    return SegmentHeader { segment_number, type, move(referred_to_segment_numbers), segment_page_association, opt_data_length };
}

static ErrorOr<size_t> scan_for_immediate_generic_region_size(ReadonlyBytes data)
{
    // 7.2.7 Segment data length
    // "If the segment's type is "Immediate generic region", then the length field may contain the value 0xFFFFFFFF.
    //  This value is intended to mean that the length of the segment's data part is unknown at the time that the segment header is written (...).
    //  In this case, the true length of the segment's data part shall be determined through examination of the data:
    //  if the segment uses template-based arithmetic coding, then the segment's data part ends with the two-byte sequence 0xFF 0xAC followed by a four-byte row count.
    //  If the segment uses MMR coding, then the segment's data part ends with the two-byte sequence 0x00 0x00 followed by a four-byte row count.
    //  The form of encoding used by the segment may be determined by examining the eighteenth byte of its segment data part,
    //  and the end sequences can occur anywhere after that eighteenth byte."
    // 7.4.6.4 Decoding a generic region segment
    // "NOTE – The sequence 0x00 0x00 cannot occur within MMR-encoded data; the sequence 0xFF 0xAC can occur only at the end of arithmetically-coded data.
    //  Thus, those sequences cannot occur by chance in the data that is decoded to generate the contents of the generic region."
    dbgln_if(JBIG2_DEBUG, "(Unknown data length, computing it)");

    if (data.size() < 19 + sizeof(u32))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Data too short to contain segment data header and end sequence");

    // Per 7.4.6.1 Generic region segment data header, this starts with the 17 bytes described in
    // 7.4.1 Region segment information field, followed the byte described in 7.4.6.2 Generic region segment flags.
    // That byte's lowest bit stores if the segment uses MMR.
    u8 flags = data[17];
    bool uses_mmr = (flags & 1) != 0;
    auto end_sequence = uses_mmr ? to_array<u8>({ 0x00, 0x00 }) : to_array<u8>({ 0xFF, 0xAC });
    u8 const* end = static_cast<u8 const*>(memmem(data.data() + 19, data.size() - 19 - sizeof(u32), end_sequence.data(), end_sequence.size()));
    if (!end)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Could not find end sequence in segment data");

    size_t size = end - data.data() + end_sequence.size() + sizeof(u32);
    dbgln_if(JBIG2_DEBUG, "(Computed size is {})", size);
    return size;
}

static ErrorOr<void> decode_segment_headers(JBIG2LoadingContext& context, ReadonlyBytes data)
{
    FixedMemoryStream stream(data);

    Vector<ReadonlyBytes> segment_datas;
    auto store_and_skip_segment_data = [&](SegmentHeader const& segment_header) -> ErrorOr<void> {
        size_t start_offset = TRY(stream.tell());
        u32 data_length = TRY(segment_header.data_length.try_value_or_lazy_evaluated([&]() {
            return scan_for_immediate_generic_region_size(data.slice(start_offset));
        }));

        if (start_offset + data_length > data.size()) {
            dbgln_if(JBIG2_DEBUG, "JBIG2ImageDecoderPlugin: start_offset={}, data_length={}, data.size()={}", start_offset, data_length, data.size());
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment data length exceeds file size");
        }
        ReadonlyBytes segment_data = data.slice(start_offset, data_length);
        segment_datas.append(segment_data);

        TRY(stream.seek(data_length, SeekMode::FromCurrentPosition));
        return {};
    };

    Vector<SegmentHeader> segment_headers;
    while (!stream.is_eof()) {
        auto segment_header = TRY(decode_segment_header(stream));
        segment_headers.append(segment_header);

        if (context.organization != Organization::RandomAccess)
            TRY(store_and_skip_segment_data(segment_header));

        // Required per spec for files with RandomAccess organization.
        if (segment_header.type == SegmentType::EndOfFile)
            break;
    }

    if (context.organization == Organization::RandomAccess) {
        for (auto const& segment_header : segment_headers)
            TRY(store_and_skip_segment_data(segment_header));
    }

    if (segment_headers.size() != segment_datas.size())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Segment headers and segment datas have different sizes");
    for (size_t i = 0; i < segment_headers.size(); ++i)
        context.segments.append({ segment_headers[i], segment_datas[i] });

    return {};
}

// 7.4.1 Region segment information field
struct [[gnu::packed]] RegionSegmentInformationField {
    BigEndian<u32> width;
    BigEndian<u32> height;
    BigEndian<u32> x_location;
    BigEndian<u32> y_location;
    u8 flags;

    // FIXME: Or have just ::CombinationOperator represent both page and segment operators?
    enum class CombinationOperator {
        Or = 0,
        And = 1,
        Xor = 2,
        XNor = 3,
        Replace = 4,
    };

    CombinationOperator external_combination_operator() const
    {
        VERIFY((flags & 0x7) <= 4);
        return static_cast<CombinationOperator>(flags & 0x7);
    }

    bool is_color_bitmap() const
    {
        return (flags & 0x8) != 0;
    }
};
static_assert(AssertSize<RegionSegmentInformationField, 17>());

static ErrorOr<RegionSegmentInformationField> decode_region_segment_information_field(ReadonlyBytes data)
{
    // 7.4.8 Page information segment syntax
    if (data.size() < sizeof(RegionSegmentInformationField))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid region segment information field size");
    auto result = *(RegionSegmentInformationField const*)data.data();
    if ((result.flags & 0b1111'0000) != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid region segment information field flags");
    if ((result.flags & 0x7) > 4)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid region segment information field operator");

    // NOTE 3 – If the colour extension flag (COLEXTFLAG) is equal to 1, the external combination operator must be REPLACE.
    if (result.is_color_bitmap() && result.external_combination_operator() != RegionSegmentInformationField::CombinationOperator::Replace)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid colored region segment information field operator");

    return result;
}

// 7.4.8 Page information segment syntax
struct [[gnu::packed]] PageInformationSegment {
    BigEndian<u32> bitmap_width;
    BigEndian<u32> bitmap_height;
    BigEndian<u32> page_x_resolution; // In pixels/meter.
    BigEndian<u32> page_y_resolution; // In pixels/meter.
    u8 flags;
    BigEndian<u16> striping_information;
};
static_assert(AssertSize<PageInformationSegment, 19>());

static ErrorOr<PageInformationSegment> decode_page_information_segment(ReadonlyBytes data)
{
    // 7.4.8 Page information segment syntax
    if (data.size() != sizeof(PageInformationSegment))
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid page information segment size");
    return *(PageInformationSegment const*)data.data();
}

static ErrorOr<void> scan_for_page_size(JBIG2LoadingContext& context)
{
    // We only decode the first page at the moment.
    bool found_size = false;
    for (auto const& segment : context.segments) {
        if (segment.header.type != SegmentType::PageInformation || segment.header.page_association != 1)
            continue;
        auto page_information = TRY(decode_page_information_segment(segment.data));

        // FIXME: We're supposed to compute this from the striping information if it's not set.
        if (page_information.bitmap_height == 0xffff'ffff)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot handle unknown page height yet");

        context.page.size = { page_information.bitmap_width, page_information.bitmap_height };
        found_size = true;
    }
    if (!found_size)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: No page information segment found for page 1");
    return {};
}

static ErrorOr<void> warn_about_multiple_pages(JBIG2LoadingContext& context)
{
    HashTable<u32> seen_pages;
    Vector<u32> pages;

    for (auto const& segment : context.segments) {
        if (segment.header.page_association == 0)
            continue;
        if (seen_pages.contains(segment.header.page_association))
            continue;
        seen_pages.set(segment.header.page_association);
        pages.append(segment.header.page_association);
    }

    // scan_for_page_size() already checked that there's a page 1.
    VERIFY(seen_pages.contains(1));
    if (pages.size() == 1)
        return {};

    StringBuilder builder;
    builder.appendff("JBIG2 file contains {} pages ({}", pages.size(), pages[0]);
    size_t i;
    for (i = 1; i < min(pages.size(), 10); ++i)
        builder.appendff(" {}", pages[i]);
    if (i != pages.size())
        builder.append(" ..."sv);
    builder.append("). We will only render page 1."sv);
    dbgln("JBIG2ImageDecoderPlugin: {}", TRY(builder.to_string()));

    return {};
}

// 6.2.2 Input parameters
struct GenericRegionDecodingInputParameters {
    bool is_modified_modified_read; // "MMR" in spec.
    u32 region_width;               // "GBW" in spec.
    u32 region_height;              // "GBH" in spec.
    u8 gb_template;
    bool is_typical_prediction_used;                 // "TPGDON" in spec.
    bool is_extended_reference_template_used;        // "EXTTEMPLATE" in spec.
    Optional<NonnullOwnPtr<BitBuffer>> skip_pattern; // "USESKIP", "SKIP" in spec.

    struct AdaptiveTemplatePixel {
        i8 x, y;
    };
    AdaptiveTemplatePixel adaptive_template_pixels[12]; // "GBATX" / "GBATY" in spec.
    // FIXME: GBCOLS, GBCOMBOP, COLEXTFLAG
};

// 6.2 Generic region decoding procedure
static ErrorOr<NonnullOwnPtr<BitBuffer>> generic_region_decoding_procedure(GenericRegionDecodingInputParameters const& inputs, ReadonlyBytes data)
{
    if (inputs.is_modified_modified_read) {
        // 6.2.6 Decoding using MMR coding
        auto buffer = TRY(CCITT::decode_ccitt_group4(data, inputs.region_width, inputs.region_height));
        auto result = TRY(BitBuffer::create(inputs.region_width, inputs.region_height));
        size_t bytes_per_row = ceil_div(inputs.region_width, 8);
        if (buffer.size() != bytes_per_row * inputs.region_height)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Decoded MMR data has wrong size");

        // FIXME: Could probably just copy the ByteBuffer directly into the BitBuffer's internal ByteBuffer instead.
        for (size_t y = 0; y < inputs.region_height; ++y) {
            for (size_t x = 0; x < inputs.region_width; ++x) {
                bool bit = buffer[y * bytes_per_row + x / 8] & (1 << (7 - x % 8));
                result->set_bit(x, y, bit);
            }
        }
        return result;
    }
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Non-MMR generic region decoding not implemented yet");
}

static ErrorOr<void> decode_symbol_dictionary(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode symbol dictionary yet");
}

static ErrorOr<void> decode_intermediate_text_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode intermediate text region yet");
}

static ErrorOr<void> decode_immediate_text_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate text region yet");
}

static ErrorOr<void> decode_immediate_lossless_text_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate lossless text region yet");
}

static ErrorOr<void> decode_pattern_dictionary(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode pattern dictionary yet");
}

static ErrorOr<void> decode_intermediate_halftone_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode intermediate halftone region yet");
}

static ErrorOr<void> decode_immediate_halftone_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate halftone region yet");
}

static ErrorOr<void> decode_immediate_lossless_halftone_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate lossless halftone region yet");
}

static ErrorOr<void> decode_intermediate_generic_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode intermediate generic region yet");
}

static ErrorOr<void> decode_immediate_generic_region(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.6 Generic region segment syntax
    auto data = segment.data;
    auto information_field = TRY(decode_region_segment_information_field(data));
    data = data.slice(sizeof(information_field));

    // 7.4.6.2 Generic region segment flags
    if (data.is_empty())
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: No segment data");
    u8 flags = data[0];
    bool uses_mmr = (flags & 1) != 0;
    u8 arithmetic_coding_template = (flags >> 1) & 3;               // "GBTEMPLATE"
    bool typical_prediction_generic_decoding_on = (flags >> 3) & 1; // "TPGDON"
    bool uses_extended_reference_template = (flags >> 4) & 1;       // "EXTTEMPLATE"
    if (flags & 0b1110'0000)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid flags");
    data = data.slice(sizeof(flags));

    // 7.4.6.3 Generic region segment AT flags
    GenericRegionDecodingInputParameters::AdaptiveTemplatePixel adaptive_template_pixels[12] = {};
    if (!uses_mmr) {
        dbgln_if(JBIG2_DEBUG, "Non-MMR generic region, GBTEMPLATE={} TPGDON={} EXTTEMPLATE={}", arithmetic_coding_template, typical_prediction_generic_decoding_on, uses_extended_reference_template);

        if (arithmetic_coding_template == 0 && uses_extended_reference_template) {
            // This was added in T.88 Amendment 2 (https://www.itu.int/rec/T-REC-T.88-200306-S!Amd2/en) mid-2003.
            // I haven't seen it being used in the wild, and the spec says "32-byte field as shown below" and then shows 24 bytes,
            // so it's not clear how much data to read.
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: GBTEMPLATE=0 EXTTEMPLATE=1 not yet implemented");
        }

        size_t number_of_adaptive_template_pixels = arithmetic_coding_template == 0 ? 4 : 1;
        if (data.size() < 2 * number_of_adaptive_template_pixels)
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: No adaptive template data");
        for (size_t i = 0; i < number_of_adaptive_template_pixels; ++i) {
            adaptive_template_pixels[i].x = static_cast<i8>(data[2 * i]);
            adaptive_template_pixels[i].y = static_cast<i8>(data[2 * i + 1]);
        }
        data = data.slice(2 * number_of_adaptive_template_pixels);
    }

    // 7.4.6.4 Decoding a generic region segment
    // "1) Interpret its header, as described in 7.4.6.1"
    // Done above.
    // "2) As described in E.3.7, reset all the arithmetic coding statistics to zero."
    // FIXME: Implement this once we support arithmetic coding.
    // "3) Invoke the generic region decoding procedure described in 6.2, with the parameters to the generic region decoding procedure set as shown in Table 37."
    GenericRegionDecodingInputParameters inputs;
    inputs.is_modified_modified_read = uses_mmr;
    inputs.region_width = information_field.width;
    inputs.region_height = information_field.height;
    inputs.gb_template = arithmetic_coding_template;
    inputs.is_typical_prediction_used = typical_prediction_generic_decoding_on;
    inputs.is_extended_reference_template_used = uses_extended_reference_template;
    inputs.skip_pattern = OptionalNone {};
    static_assert(sizeof(inputs.adaptive_template_pixels) == sizeof(adaptive_template_pixels));
    memcpy(inputs.adaptive_template_pixels, adaptive_template_pixels, sizeof(adaptive_template_pixels));
    auto result = TRY(generic_region_decoding_procedure(inputs, data));

    // 8.2 Page image composition step 5)
    if (information_field.x_location + information_field.width > (u32)context.page.size.width()
        || information_field.y_location + information_field.height > (u32)context.page.size.height()) {
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Region bounds outsize of page bounds");
    }
    for (size_t y = 0; y < information_field.height; ++y) {
        for (size_t x = 0; x < information_field.width; ++x) {
            // FIXME: Honor segment's combination operator instead of just copying.
            context.page.bits->set_bit(information_field.x_location + x, information_field.y_location + y, result->get_bit(x, y));
        }
    }

    return {};
}

static ErrorOr<void> decode_immediate_lossless_generic_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate lossless generic region yet");
}

static ErrorOr<void> decode_intermediate_generic_refinement_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode intermediate generic refinement region yet");
}

static ErrorOr<void> decode_immediate_generic_refinement_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate generic refinement region yet");
}

static ErrorOr<void> decode_immediate_lossless_generic_refinement_region(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode immediate lossless generic refinement region yet");
}

static ErrorOr<void> decode_page_information(JBIG2LoadingContext& context, SegmentData const& segment)
{
    // 7.4.8 Page information segment syntax and 8.1 Decoder model steps 1) - 3).

    // "1) Decode the page information segment.""
    auto page_information = TRY(decode_page_information_segment(segment.data));

    bool page_is_striped = (page_information.striping_information & 0x80) != 0;
    if (page_information.bitmap_height == 0xffff'ffff && !page_is_striped)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Non-striped bitmaps of indeterminate height not allowed");

    u8 default_color = (page_information.flags >> 2) & 1;
    u8 default_combination_operator = (page_information.flags >> 3) & 3;
    context.page.default_combination_operator = static_cast<CombinationOperator>(default_combination_operator);

    // FIXME: Do something with the other fields in page_information.

    // "2) Create the page buffer, of the size given in the page information segment.
    //
    //     If the page height is unknown, then this is not possible. However, in this case the page must be striped,
    //     and the maximum stripe height specified, and the initial page buffer can be created with height initially
    //     equal to this maximum stripe height."
    size_t height = page_information.bitmap_height;
    if (height == 0xffff'ffff)
        height = page_information.striping_information & 0x7F;
    context.page.bits = TRY(BitBuffer::create(page_information.bitmap_width, height));

    // "3) Fill the page buffer with the page's default pixel value."
    context.page.bits->fill(default_color != 0);

    return {};
}

static ErrorOr<void> decode_end_of_page(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.9 End of page segment syntax
    if (segment.data.size() != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of page segment has non-zero size");
    // FIXME: If the page had unknown height, check that previous segment was end-of-stripe.
    // FIXME: Maybe mark page as completed and error if we see more segments for it?
    return {};
}

static ErrorOr<void> decode_end_of_stripe(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode end of stripe yet");
}

static ErrorOr<void> decode_end_of_file(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.11 End of file segment syntax
    if (segment.data.size() != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of file segment has non-zero size");
    return {};
}

static ErrorOr<void> decode_profiles(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode profiles yet");
}

static ErrorOr<void> decode_tables(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode tables yet");
}

static ErrorOr<void> decode_color_palette(JBIG2LoadingContext&, SegmentData const&)
{
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Cannot decode color palette yet");
}

static ErrorOr<void> decode_extension(JBIG2LoadingContext&, SegmentData const& segment)
{
    // 7.4.14 Extension segment syntax
    FixedMemoryStream stream { segment.data };

    enum ExtensionType {
        SingleByteCodedComment = 0x20000000,
        MultiByteCodedComment = 0x20000002,
    };
    u32 type = TRY(stream.read_value<BigEndian<u32>>());

    auto read_string = [&]<class T>() -> ErrorOr<Vector<T>> {
        Vector<T> result;
        do {
            result.append(TRY(stream.read_value<BigEndian<T>>()));
        } while (result.last());
        result.take_last();
        return result;
    };

    switch (type) {
    case SingleByteCodedComment: {
        // 7.4.15.1 Single-byte coded comment
        // Pairs of zero-terminated ISO/IEC 8859-1 (latin1) pairs, terminated by another \0.
        while (true) {
            auto first_bytes = TRY(read_string.template operator()<u8>());
            if (first_bytes.is_empty())
                break;

            auto second_bytes = TRY(read_string.template operator()<u8>());

            auto first = TRY(TextCodec::decoder_for("ISO-8859-1"sv)->to_utf8(StringView { first_bytes }));
            auto second = TRY(TextCodec::decoder_for("ISO-8859-1"sv)->to_utf8(StringView { second_bytes }));
            dbgln("JBIG2ImageDecoderPlugin: key '{}', value '{}'", first, second);
        }
        if (!stream.is_eof())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Trailing data after SingleByteCodedComment");
        return {};
    }
    case MultiByteCodedComment: {
        // 7.4.15.2 Multi-byte coded comment
        // Pairs of (two-byte-)zero-terminated UCS-2 pairs, terminated by another \0\0.
        while (true) {
            auto first_ucs2 = TRY(read_string.template operator()<u16>());
            if (first_ucs2.is_empty())
                break;

            auto second_ucs2 = TRY(read_string.template operator()<u16>());

            auto first = TRY(Utf16View(first_ucs2).to_utf8());
            auto second = TRY(Utf16View(second_ucs2).to_utf8());
            dbgln("JBIG2ImageDecoderPlugin: key '{}', value '{}'", first, second);
        }
        if (!stream.is_eof())
            return Error::from_string_literal("JBIG2ImageDecoderPlugin: Trailing data after MultiByteCodedComment");
        return {};
    }
    }

    // FIXME: If bit 31 in `type` is not set, the extension isn't necessary, and we could ignore it.
    dbgln("JBIG2ImageDecoderPlugin: Unknown extension type {:#x}", type);
    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Unknown extension type");
}

static ErrorOr<void> decode_data(JBIG2LoadingContext& context)
{
    TRY(warn_about_multiple_pages(context));

    for (size_t i = 0; i < context.segments.size(); ++i) {
        auto const& segment = context.segments[i];

        if (segment.header.page_association != 0 && segment.header.page_association != 1)
            continue;

        switch (segment.header.type) {
        case SegmentType::SymbolDictionary:
            TRY(decode_symbol_dictionary(context, segment));
            break;
        case SegmentType::IntermediateTextRegion:
            TRY(decode_intermediate_text_region(context, segment));
            break;
        case SegmentType::ImmediateTextRegion:
            TRY(decode_immediate_text_region(context, segment));
            break;
        case SegmentType::ImmediateLosslessTextRegion:
            TRY(decode_immediate_lossless_text_region(context, segment));
            break;
        case SegmentType::PatternDictionary:
            TRY(decode_pattern_dictionary(context, segment));
            break;
        case SegmentType::IntermediateHalftoneRegion:
            TRY(decode_intermediate_halftone_region(context, segment));
            break;
        case SegmentType::ImmediateHalftoneRegion:
            TRY(decode_immediate_halftone_region(context, segment));
            break;
        case SegmentType::ImmediateLosslessHalftoneRegion:
            TRY(decode_immediate_lossless_halftone_region(context, segment));
            break;
        case SegmentType::IntermediateGenericRegion:
            TRY(decode_intermediate_generic_region(context, segment));
            break;
        case SegmentType::ImmediateGenericRegion:
            TRY(decode_immediate_generic_region(context, segment));
            break;
        case SegmentType::ImmediateLosslessGenericRegion:
            TRY(decode_immediate_lossless_generic_region(context, segment));
            break;
        case SegmentType::IntermediateGenericRefinementRegion:
            TRY(decode_intermediate_generic_refinement_region(context, segment));
            break;
        case SegmentType::ImmediateGenericRefinementRegion:
            TRY(decode_immediate_generic_refinement_region(context, segment));
            break;
        case SegmentType::ImmediateLosslessGenericRefinementRegion:
            TRY(decode_immediate_lossless_generic_refinement_region(context, segment));
            break;
        case SegmentType::PageInformation:
            TRY(decode_page_information(context, segment));
            break;
        case SegmentType::EndOfPage:
            TRY(decode_end_of_page(context, segment));
            break;
        case SegmentType::EndOfStripe:
            TRY(decode_end_of_stripe(context, segment));
            break;
        case SegmentType::EndOfFile:
            TRY(decode_end_of_file(context, segment));
            // "If a file contains an end of file segment, it must be the last segment."
            if (i != context.segments.size() - 1)
                return Error::from_string_literal("JBIG2ImageDecoderPlugin: End of file segment not last segment");
            break;
        case SegmentType::Profiles:
            TRY(decode_profiles(context, segment));
            break;
        case SegmentType::Tables:
            TRY(decode_tables(context, segment));
            break;
        case SegmentType::ColorPalette:
            TRY(decode_color_palette(context, segment));
            break;
        case SegmentType::Extension:
            TRY(decode_extension(context, segment));
            break;
        }
    }

    return {};
}

JBIG2ImageDecoderPlugin::JBIG2ImageDecoderPlugin()
{
    m_context = make<JBIG2LoadingContext>();
}

IntSize JBIG2ImageDecoderPlugin::size()
{
    return m_context->page.size;
}

bool JBIG2ImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.starts_with(id_string);
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JBIG2ImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JBIG2ImageDecoderPlugin()));
    TRY(decode_jbig2_header(*plugin->m_context, data));

    data = data.slice(sizeof(id_string) + sizeof(u8) + (plugin->m_context->number_of_pages.has_value() ? sizeof(u32) : 0));
    TRY(decode_segment_headers(*plugin->m_context, data));

    TRY(scan_for_page_size(*plugin->m_context));

    return plugin;
}

ErrorOr<ImageFrameDescriptor> JBIG2ImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    // FIXME: Use this for multi-page JBIG2 files?
    if (index != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid frame index");

    if (m_context->state == JBIG2LoadingContext::State::Error)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Decoding failed");

    if (m_context->state < JBIG2LoadingContext::State::Decoded) {
        auto result = decode_data(*m_context);
        if (result.is_error()) {
            m_context->state = JBIG2LoadingContext::State::Error;
            return result.release_error();
        }
        m_context->state = JBIG2LoadingContext::State::Decoded;
    }

    auto bitmap = TRY(m_context->page.bits->to_gfx_bitmap());
    return ImageFrameDescriptor { move(bitmap), 0 };
}

ErrorOr<ByteBuffer> JBIG2ImageDecoderPlugin::decode_embedded(Vector<ReadonlyBytes> data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JBIG2ImageDecoderPlugin()));
    plugin->m_context->organization = Organization::Embedded;

    for (auto const& segment_data : data)
        TRY(decode_segment_headers(*plugin->m_context, segment_data));

    TRY(scan_for_page_size(*plugin->m_context));
    TRY(decode_data(*plugin->m_context));

    return plugin->m_context->page.bits->to_byte_buffer();
}

}
