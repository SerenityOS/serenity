/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/QuickSort.h>
#include <AK/Try.h>
#include <LibEDID/EDID.h>

#ifdef KERNEL
#    include <Kernel/Library/StdLib.h>
#else
#    include <AK/ScopeGuard.h>
#    include <fcntl.h>
#    include <sys/devices/gpu.h>
#    include <unistd.h>

#    if ENABLE_PNP_IDS_DATA
#        include <LibEDID/PnpIDs.h>
#    endif
#endif

namespace EDID {

// clang doesn't like passing around pointers to members in packed structures,
// even though we're only using them for arithmetic purposes
#if defined(AK_COMPILER_CLANG)
#    pragma clang diagnostic ignored "-Waddress-of-packed-member"
#endif

static_assert(sizeof(Definitions::EDID) == Parser::BufferSize);

class CEA861ExtensionBlock final {
    friend class Parser;

public:
    enum class DataBlockTag : u8 {
        Reserved = 0,
        Audio,
        Video,
        VendorSpecific,
        SpeakerAllocation,
        VesaDTC,
        Reserved2,
        Extended
    };

    ErrorOr<IterationDecision> for_each_short_video_descriptor(Function<IterationDecision(bool, VIC::Details const&)> callback) const
    {
        return for_each_data_block([&](DataBlockTag tag, ReadonlyBytes bytes) -> ErrorOr<IterationDecision> {
            if (tag != DataBlockTag::Video)
                return IterationDecision::Continue;

            // Short video descriptors are one byte values
            for (size_t i = 0; i < bytes.size(); i++) {
                u8 byte = m_edid.read_host(&bytes[i]);
                bool is_native = (byte & 0x80) != 0;
                u8 vic_id = byte & 0x7f;

                auto* vic_details = VIC::find_details_by_vic_id(vic_id);
                if (!vic_details)
                    return Error::from_string_view_or_print_error_and_return_errno("CEA 861 extension block has invalid short video descriptor"sv, EINVAL);

                IterationDecision decision = callback(is_native, *vic_details);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
            return IterationDecision::Continue;
        });
    }

    ErrorOr<IterationDecision> for_each_dtd(Function<IterationDecision(Parser::DetailedTiming const&)> callback) const
    {
        u8 dtd_start = m_edid.read_host(&m_block->cea861extension.dtd_start_offset);
        if (dtd_start < 4) {
            // dtd_start == 4 means there are no data blocks, but there are still DTDs
            return IterationDecision::Continue;
        }

        if (dtd_start > offsetof(Definitions::ExtensionBlock, checksum) - sizeof(Definitions::DetailedTiming))
            return Error::from_string_view_or_print_error_and_return_errno("CEA 861 extension block has invalid DTD list"sv, EINVAL);

        for (size_t offset = dtd_start; offset <= offsetof(Definitions::ExtensionBlock, checksum) - sizeof(Definitions::DetailedTiming); offset += sizeof(Definitions::DetailedTiming)) {
            auto& dtd = *(Definitions::DetailedTiming const*)((u8 const*)m_block + offset);
            if (m_edid.read_host(&dtd.pixel_clock) == 0)
                break;

            IterationDecision decision = callback(Parser::DetailedTiming(m_edid, &dtd));
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

private:
    CEA861ExtensionBlock(Parser const& edid, Definitions::ExtensionBlock const* block)
        : m_edid(edid)
        , m_block(block)
    {
    }

    ErrorOr<IterationDecision> for_each_data_block(Function<ErrorOr<IterationDecision>(DataBlockTag, ReadonlyBytes)> callback) const
    {
        u8 dtd_start = m_edid.read_host(&m_block->cea861extension.dtd_start_offset);
        if (dtd_start <= 4)
            return IterationDecision::Continue;

        if (dtd_start > offsetof(Definitions::ExtensionBlock, checksum))
            return Error::from_string_view_or_print_error_and_return_errno("CEA 861 extension block has invalid DTD start offset"sv, EINVAL);

        auto* data_block_header = &m_block->cea861extension.bytes[0];
        auto* data_block_end = (u8 const*)m_block + dtd_start;
        while (data_block_header < data_block_end) {
            auto header_byte = m_edid.read_host(data_block_header);
            size_t payload_size = header_byte & 0x1f;
            auto tag = (DataBlockTag)((header_byte >> 5) & 0x7);
            if (tag == DataBlockTag::Extended && payload_size == 0)
                return Error::from_string_view_or_print_error_and_return_errno("CEA 861 extension block has invalid extended data block size"sv, EINVAL);

            auto decision = TRY(callback(tag, m_edid.m_bytes.slice(data_block_header - m_edid.m_bytes.data() + 1, payload_size)));
            if (decision != IterationDecision::Continue)
                return decision;

            data_block_header += 1 + payload_size;
        }
        return IterationDecision::Continue;
    }

    ErrorOr<IterationDecision> for_each_display_descriptor(Function<IterationDecision(u8, Definitions::DisplayDescriptor const&)> callback) const
    {
        u8 dtd_start = m_edid.read_host(&m_block->cea861extension.dtd_start_offset);
        if (dtd_start <= 4)
            return IterationDecision::Continue;

        if (dtd_start > offsetof(Definitions::ExtensionBlock, checksum) - sizeof(Definitions::DetailedTiming))
            return Error::from_string_view_or_print_error_and_return_errno("CEA 861 extension block has invalid DTD list"sv, EINVAL);

        for (size_t offset = dtd_start; offset <= offsetof(Definitions::ExtensionBlock, checksum) - sizeof(Definitions::DisplayDescriptor); offset += sizeof(Definitions::DisplayDescriptor)) {
            auto& dd = *(Definitions::DisplayDescriptor const*)((u8 const*)m_block + offset);
            if (m_edid.read_host(&dd.zero) != 0 || m_edid.read_host(&dd.reserved1) != 0)
                continue;

            u8 tag = m_edid.read_host(&dd.tag);
            IterationDecision decision = callback(tag, dd);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    Parser const& m_edid;
    Definitions::ExtensionBlock const* m_block;
};

template<typename T>
T Parser::read_host(T const* field) const
{
    VERIFY((u8 const*)field >= m_bytes.data() && (u8 const*)field + sizeof(T) <= m_bytes.data() + m_bytes.size());
    size_t offset = (u8 const*)field - m_bytes.data();
    T value;
    if constexpr (sizeof(T) > 1)
        ByteReader::load(m_bytes.offset(offset), value);
    else
        value = m_bytes.at(offset);

    return value;
}

template<Integral T>
requires(sizeof(T) > 1)
T Parser::read_le(T const* field) const
{
    static_assert(sizeof(T) > 1);
    return AK::convert_between_host_and_little_endian(read_host(field));
}

template<Integral T>
requires(sizeof(T) > 1)
T Parser::read_be(T const* field) const
{
    static_assert(sizeof(T) > 1);
    return AK::convert_between_host_and_big_endian(read_host(field));
}

ErrorOr<Parser> Parser::from_bytes(ReadonlyBytes bytes)
{
    Parser edid(bytes);
    TRY(edid.parse());
    return edid;
}

ErrorOr<Parser> Parser::from_bytes(ByteBuffer&& bytes)
{
    Parser edid(move(bytes));
    TRY(edid.parse());
    return edid;
}

#ifndef KERNEL
ErrorOr<Parser> Parser::from_display_connector_device(int display_connector_fd)
{
    RawBytes edid_bytes;
    GraphicsHeadEDID edid_info {};
    edid_info.bytes = &edid_bytes[0];
    edid_info.bytes_size = sizeof(edid_bytes);
    if (graphics_connector_get_head_edid(display_connector_fd, &edid_info) < 0) {
        int err = errno;
        if (err == EOVERFLOW) {
            // We need a bigger buffer with at least bytes_size bytes
            auto edid_byte_buffer = TRY(ByteBuffer::create_zeroed(edid_info.bytes_size));
            edid_info.bytes = edid_byte_buffer.data();
            if (graphics_connector_get_head_edid(display_connector_fd, &edid_info) < 0) {
                err = errno;
                return Error::from_errno(err);
            }

            return from_bytes(move(edid_byte_buffer));
        }

        return Error::from_errno(err);
    }

    auto edid_byte_buffer = TRY(ByteBuffer::copy((void const*)edid_bytes, sizeof(edid_bytes)));
    return from_bytes(move(edid_byte_buffer));
}

ErrorOr<Parser> Parser::from_display_connector_device(ByteString const& display_connector_device)
{
    int display_connector_fd = open(display_connector_device.characters(), O_RDWR | O_CLOEXEC);
    if (display_connector_fd < 0) {
        int err = errno;
        return Error::from_errno(err);
    }
    ScopeGuard fd_guard([&] {
        close(display_connector_fd);
    });
    return from_display_connector_device(display_connector_fd);
}
#endif

Parser::Parser(ReadonlyBytes bytes)
    : m_bytes(move(bytes))
{
}

Parser::Parser(ByteBuffer&& bytes)
    : m_bytes_buffer(move(bytes))
    , m_bytes(m_bytes_buffer)
{
}

Parser::Parser(Parser const& other)
    : m_bytes_buffer(other.m_bytes_buffer)
    , m_revision(other.m_revision)
{
    if (m_bytes_buffer.is_empty())
        m_bytes = other.m_bytes_buffer; // We don't own the buffer
    else
        m_bytes = m_bytes_buffer; // We own the buffer
}

Parser& Parser::operator=(Parser&& from)
{
    m_bytes_buffer = move(from.m_bytes_buffer);
    m_bytes = move(from.m_bytes);
    m_revision = from.m_revision;
    return *this;
}

Parser& Parser::operator=(Parser const& other)
{
    if (this == &other)
        return *this;

    m_bytes_buffer = other.m_bytes_buffer;
    if (m_bytes_buffer.is_empty())
        m_bytes = other.m_bytes_buffer; // We don't own the buffer
    else
        m_bytes = m_bytes_buffer; // We own the buffer
    m_revision = other.m_revision;
    return *this;
}

bool Parser::operator==(Parser const& other) const
{
    if (this == &other)
        return true;
    return m_bytes == other.m_bytes;
}

Definitions::EDID const& Parser::raw_edid() const
{
    return *(Definitions::EDID const*)m_bytes.data();
}

ErrorOr<void> Parser::parse()
{
    if (m_bytes.size() < sizeof(Definitions::EDID))
        return Error::from_string_view_or_print_error_and_return_errno("Incomplete Parser structure"sv, EINVAL);

    auto const& edid = raw_edid();
    u64 header = read_le(&edid.header);
    if (header != 0x00ffffffffffff00ull)
        return Error::from_string_view_or_print_error_and_return_errno("No Parser header"sv, EINVAL);

    u8 major_version = read_host(&edid.version.version);
    m_revision = read_host(&edid.version.revision);
    if (major_version != 1 || m_revision > 4)
        return Error::from_string_view_or_print_error_and_return_errno("Unsupported Parser version"sv, EINVAL);

#ifdef KERNEL
    m_version = TRY(Kernel::KString::formatted("1.{}", (int)m_revision));
#else
    m_version = ByteString::formatted("1.{}", (int)m_revision);
#endif

    u8 checksum = 0x0;
    for (size_t i = 0; i < sizeof(Definitions::EDID); i++)
        checksum += m_bytes[i];

    if (checksum != 0) {
        if (m_revision >= 4)
            return Error::from_string_view_or_print_error_and_return_errno("Parser checksum mismatch"sv, EINVAL);
        else
            dbgln("EDID checksum mismatch, data may be corrupted!");
    }

    u16 packed_id = read_be(&raw_edid().vendor.manufacturer_id);
    if (packed_id == 0x0)
        return {};
    m_legacy_manufacturer_id[0] = (char)((u16)'A' + ((packed_id >> 10) & 0x1f) - 1);
    m_legacy_manufacturer_id[1] = (char)((u16)'A' + ((packed_id >> 5) & 0x1f) - 1);
    m_legacy_manufacturer_id[2] = (char)((u16)'A' + (packed_id & 0x1f) - 1);
    m_legacy_manufacturer_id[3] = '\0';
    m_legacy_manufacturer_id_valid = true;

    return {};
}

ErrorOr<IterationDecision> Parser::for_each_extension_block(Function<IterationDecision(unsigned, u8, u8, ReadonlyBytes)> callback) const
{
    auto& edid = raw_edid();
    u8 raw_extension_block_count = read_host(&edid.extension_block_count);
    if (raw_extension_block_count == 0)
        return IterationDecision::Continue;
    if (sizeof(Definitions::EDID) + (size_t)raw_extension_block_count * sizeof(Definitions::ExtensionBlock) > m_bytes.size())
        return Error::from_string_view_or_print_error_and_return_errno("Truncated EDID"sv, EINVAL);

    auto validate_block_checksum = [&](Definitions::ExtensionBlock const& block) {
        u8 checksum = 0x0;
        auto* bytes = (u8 const*)&block;
        for (size_t i = 0; i < sizeof(block); i++)
            checksum += bytes[i];

        return checksum == 0;
    };

    auto* raw_extension_blocks = (Definitions::ExtensionBlock const*)(m_bytes.data() + sizeof(Definitions::EDID));
    Definitions::ExtensionBlock const* current_extension_map = nullptr;

    unsigned raw_index = 0;
    if (m_revision <= 3) {
        if (raw_extension_block_count > 1) {
            current_extension_map = &raw_extension_blocks[0];
            raw_index++;
            if (read_host(&current_extension_map->tag) != (u8)Definitions::ExtensionBlockTag::ExtensionBlockMap)
                return Error::from_string_view_or_print_error_and_return_errno("Did not find extension map at block 1"sv, EINVAL);

            if (!validate_block_checksum(*current_extension_map))
                return Error::from_string_view_or_print_error_and_return_errno("Extension block map checksum mismatch"sv, EINVAL);
        }
    } else if (read_host(&raw_extension_blocks[0].tag) == (u8)Definitions::ExtensionBlockTag::ExtensionBlockMap) {
        current_extension_map = &raw_extension_blocks[0];
        raw_index++;
    }

    for (; raw_index < raw_extension_block_count; raw_index++) {
        auto& raw_block = raw_extension_blocks[raw_index];
        u8 tag = read_host(&raw_block.tag);

        if (current_extension_map && raw_index == 127) {
            if (tag != (u8)Definitions::ExtensionBlockTag::ExtensionBlockMap)
                return Error::from_string_view_or_print_error_and_return_errno("Did not find extension map at block 128"sv, EINVAL);

            current_extension_map = &raw_extension_blocks[127];
            if (!validate_block_checksum(*current_extension_map))
                return Error::from_string_view_or_print_error_and_return_errno("Extension block map checksum mismatch"sv, EINVAL);
            continue;
        }

        if (tag == (u8)Definitions::ExtensionBlockTag::ExtensionBlockMap)
            return Error::from_string_view_or_print_error_and_return_errno("Unexpected extension map encountered"sv, EINVAL);

        if (!validate_block_checksum(raw_block))
            return Error::from_string_view_or_print_error_and_return_errno("Extension block checksum mismatch"sv, EINVAL);

        size_t offset = (u8 const*)&raw_block - m_bytes.data();
        IterationDecision decision = callback(raw_index + 1, tag, raw_block.block.revision, m_bytes.slice(offset, sizeof(Definitions::ExtensionBlock)));
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}

StringView Parser::version() const
{
#ifdef KERNEL
    return m_version->view();
#else
    return m_version;
#endif
}

StringView Parser::legacy_manufacturer_id() const
{
    return { m_legacy_manufacturer_id, strlen(m_legacy_manufacturer_id) };
}

#ifndef KERNEL
ByteString Parser::manufacturer_name() const
{
    if (!m_legacy_manufacturer_id_valid)
        return "Unknown";
    auto manufacturer_id = legacy_manufacturer_id();
#    if ENABLE_PNP_IDS_DATA
    if (auto pnp_id_data = PnpIDs::find_by_manufacturer_id(manufacturer_id); pnp_id_data.has_value())
        return pnp_id_data.value().manufacturer_name;
#    endif
    return manufacturer_id;
}
#endif

u16 Parser::product_code() const
{
    return read_le(&raw_edid().vendor.product_code);
}

u32 Parser::serial_number() const
{
    return read_le(&raw_edid().vendor.serial_number);
}

auto Parser::digital_display() const -> Optional<DigitalDisplay>
{
    auto& edid = raw_edid();
    u8 video_input_definition = read_host(&edid.basic_display_parameters.video_input_definition);
    if (!(video_input_definition & 0x80))
        return {}; // This is an analog display

    u8 feature_support = read_host(&edid.basic_display_parameters.feature_support);
    return DigitalDisplay(video_input_definition, feature_support, m_revision);
}

auto Parser::analog_display() const -> Optional<AnalogDisplay>
{
    auto& edid = raw_edid();
    u8 video_input_definition = read_host(&edid.basic_display_parameters.video_input_definition);
    if ((video_input_definition & 0x80) != 0)
        return {}; // This is a digital display

    u8 feature_support = read_host(&edid.basic_display_parameters.feature_support);
    return AnalogDisplay(video_input_definition, feature_support, m_revision);
}

auto Parser::screen_size() const -> Optional<ScreenSize>
{
    auto& edid = raw_edid();
    u8 horizontal_size_or_aspect_ratio = read_host(&edid.basic_display_parameters.horizontal_size_or_aspect_ratio);
    u8 vertical_size_or_aspect_ratio = read_host(&edid.basic_display_parameters.vertical_size_or_aspect_ratio);

    if (horizontal_size_or_aspect_ratio == 0 || vertical_size_or_aspect_ratio == 0) {
        // EDID < 1.4: Unknown or undefined
        // EDID >= 1.4: If both are 0 it is unknown or undefined
        //              If one of them is 0 then we're dealing with aspect ratios
        return {};
    }

    return ScreenSize(horizontal_size_or_aspect_ratio, vertical_size_or_aspect_ratio);
}

auto Parser::aspect_ratio() const -> Optional<ScreenAspectRatio>
{
    if (m_revision < 4)
        return {};

    auto& edid = raw_edid();
    u8 value_1 = read_host(&edid.basic_display_parameters.horizontal_size_or_aspect_ratio);
    u8 value_2 = read_host(&edid.basic_display_parameters.vertical_size_or_aspect_ratio);

    if (value_1 == 0 && value_2 == 0)
        return {}; // Unknown or undefined
    if (value_1 != 0 && value_2 != 0)
        return {}; // Dimensions are in cm

    if (value_1 == 0)
        return ScreenAspectRatio(ScreenAspectRatio::Orientation::Portrait, FixedPoint<16>(100) / FixedPoint<16>((i32)value_2 + 99));

    VERIFY(value_2 == 0);
    return ScreenAspectRatio(ScreenAspectRatio::Orientation::Landscape, FixedPoint<16>((i32)value_1 + 99) / 100);
}

Optional<FixedPoint<16>> Parser::gamma() const
{
    u8 display_transfer_characteristics = read_host(&raw_edid().basic_display_parameters.display_transfer_characteristics);
    if (display_transfer_characteristics == 0xff) {
        if (m_revision < 4)
            return {};

        // TODO: EDID >= 1.4 stores more gamma details in an extension block (e.g. DI-EXT)
        return {};
    }

    FixedPoint<16> gamma { (i32)display_transfer_characteristics + 100 };
    gamma /= 100;
    return gamma;
}

u32 Parser::DetailedTiming::pixel_clock_khz() const
{
    // Note: The stored value is in units of 10 kHz, which means that to get the
    // value in kHz, we need to multiply it by 10.
    return (u32)m_edid.read_le(&m_detailed_timings.pixel_clock) * 10;
}

u16 Parser::DetailedTiming::horizontal_addressable_pixels() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.horizontal_addressable_pixels_low);
    u8 high = m_edid.read_host(&m_detailed_timings.horizontal_addressable_and_blanking_pixels_high) >> 4;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::horizontal_blanking_pixels() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.horizontal_blanking_pixels_low);
    u8 high = m_edid.read_host(&m_detailed_timings.horizontal_addressable_and_blanking_pixels_high) & 0xf;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::vertical_addressable_lines_raw() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.vertical_addressable_lines_low);
    u8 high = m_edid.read_host(&m_detailed_timings.vertical_addressable_and_blanking_lines_high) >> 4;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::vertical_addressable_lines() const
{
    auto lines = vertical_addressable_lines_raw();
    return is_interlaced() ? lines * 2 : lines;
}

u16 Parser::DetailedTiming::vertical_blanking_lines() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.vertical_blanking_lines_low);
    u8 high = m_edid.read_host(&m_detailed_timings.vertical_addressable_and_blanking_lines_high) & 0xf;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::horizontal_front_porch_pixels() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.horizontal_front_porch_pixels_low);
    u8 high = m_edid.read_host(&m_detailed_timings.horizontal_and_vertical_front_porch_sync_pulse_width_high) >> 6;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::horizontal_sync_pulse_width_pixels() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.horizontal_sync_pulse_width_pixels_low);
    u8 high = (m_edid.read_host(&m_detailed_timings.horizontal_and_vertical_front_porch_sync_pulse_width_high) >> 4) & 3;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::vertical_front_porch_lines() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.vertical_front_porch_and_sync_pulse_width_lines_low) >> 4;
    u8 high = (m_edid.read_host(&m_detailed_timings.horizontal_and_vertical_front_porch_sync_pulse_width_high) >> 2) & 3;
    return ((u16)high << 4) | (u16)low;
}

u16 Parser::DetailedTiming::vertical_sync_pulse_width_lines() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.vertical_front_porch_and_sync_pulse_width_lines_low) & 0xf;
    u8 high = m_edid.read_host(&m_detailed_timings.horizontal_and_vertical_front_porch_sync_pulse_width_high) & 3;
    return ((u16)high << 4) | (u16)low;
}

u16 Parser::DetailedTiming::horizontal_image_size_mm() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.horizontal_addressable_image_size_mm_low);
    u8 high = m_edid.read_host(&m_detailed_timings.horizontal_vertical_addressable_image_size_mm_high) >> 4;
    return ((u16)high << 8) | (u16)low;
}

u16 Parser::DetailedTiming::vertical_image_size_mm() const
{
    u8 low = m_edid.read_host(&m_detailed_timings.vertical_addressable_image_size_mm_low);
    u8 high = m_edid.read_host(&m_detailed_timings.horizontal_vertical_addressable_image_size_mm_high) & 0xf;
    return ((u16)high << 8) | (u16)low;
}

u8 Parser::DetailedTiming::horizontal_right_or_left_border_pixels() const
{
    return m_edid.read_host(&m_detailed_timings.right_or_left_horizontal_border_pixels);
}

u8 Parser::DetailedTiming::vertical_top_or_bottom_border_lines() const
{
    return m_edid.read_host(&m_detailed_timings.top_or_bottom_vertical_border_lines);
}

bool Parser::DetailedTiming::is_interlaced() const
{
    return (m_edid.read_host(&m_detailed_timings.features) & (1 << 7)) != 0;
}

FixedPoint<16, u32> Parser::DetailedTiming::refresh_rate() const
{
    // Blanking = front porch + sync pulse width + back porch
    u32 total_horizontal_pixels = (u32)horizontal_addressable_pixels() + (u32)horizontal_blanking_pixels();
    u32 total_vertical_lines = (u32)vertical_addressable_lines_raw() + (u32)vertical_blanking_lines();
    u32 total_pixels = total_horizontal_pixels * total_vertical_lines;
    if (total_pixels == 0)
        return {};
    // Use a bigger fixed point representation due to the large numbers involved and then downcast
    // Note: We need to convert the pixel clock from kHz to Hertz to actually calculate this correctly.
    return FixedPoint<32, u64>(pixel_clock_khz() * 1000) / total_pixels;
}

ErrorOr<IterationDecision> Parser::for_each_established_timing(Function<IterationDecision(EstablishedTiming const&)> callback) const
{
    static constexpr EstablishedTiming established_timing_byte1[8] = {
        { EstablishedTiming::Source::VESA, 800, 600, 60, 0x9 },
        { EstablishedTiming::Source::VESA, 800, 600, 56, 0x8 },
        { EstablishedTiming::Source::VESA, 640, 480, 75, 0x6 },
        { EstablishedTiming::Source::VESA, 640, 480, 73, 0x5 },
        { EstablishedTiming::Source::Apple, 640, 480, 67 },
        { EstablishedTiming::Source::IBM, 640, 480, 60, 0x4 },
        { EstablishedTiming::Source::IBM, 720, 400, 88 },
        { EstablishedTiming::Source::IBM, 720, 400, 70 }
    };
    static constexpr EstablishedTiming established_timing_byte2[8] = {
        { EstablishedTiming::Source::VESA, 1280, 1024, 75, 0x24 },
        { EstablishedTiming::Source::VESA, 1024, 768, 75, 0x12 },
        { EstablishedTiming::Source::VESA, 1024, 768, 70, 0x11 },
        { EstablishedTiming::Source::VESA, 1024, 768, 60, 0x10 },
        { EstablishedTiming::Source::IBM, 1024, 768, 87, 0xf },
        { EstablishedTiming::Source::Apple, 832, 624, 75 },
        { EstablishedTiming::Source::VESA, 800, 600, 75, 0xb },
        { EstablishedTiming::Source::VESA, 800, 600, 72, 0xa }
    };
    static constexpr EstablishedTiming established_timing_byte3[1] = {
        { EstablishedTiming::Source::Apple, 1152, 870, 75 }
    };

    auto& established_timings = raw_edid().established_timings;
    for (int i = 7; i >= 0; i--) {
        if (!(established_timings.timings_1 & (1 << i)))
            continue;
        IterationDecision decision = callback(established_timing_byte1[i]);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    for (int i = 7; i >= 0; i--) {
        if (!(established_timings.timings_2 & (1 << i)))
            continue;
        IterationDecision decision = callback(established_timing_byte2[i]);
        if (decision != IterationDecision::Continue)
            return decision;
    }

    if ((established_timings.manufacturer_reserved & (1 << 7)) != 0) {
        IterationDecision decision = callback(established_timing_byte3[0]);
        if (decision != IterationDecision::Continue)
            return decision;
    }

    u8 manufacturer_specific = established_timings.manufacturer_reserved & 0x7f;
    if (manufacturer_specific != 0) {
        IterationDecision decision = callback(EstablishedTiming(EstablishedTiming::Source::Manufacturer, 0, 0, manufacturer_specific));
        if (decision != IterationDecision::Continue)
            return decision;
    }

    auto callback_decision = IterationDecision::Continue;
    TRY(for_each_display_descriptor([&](u8 descriptor_tag, auto& display_descriptor) {
        if (descriptor_tag != (u8)Definitions::DisplayDescriptorTag::EstablishedTimings3)
            return IterationDecision::Continue;

        static constexpr EstablishedTiming established_timings3_bytes[] = {
            // Byte 1
            { EstablishedTiming::Source::VESA, 640, 350, 85, 0x1 },
            { EstablishedTiming::Source::VESA, 640, 400, 85, 0x2 },
            { EstablishedTiming::Source::VESA, 720, 400, 85, 0x3 },
            { EstablishedTiming::Source::VESA, 640, 480, 85, 0x7 },
            { EstablishedTiming::Source::VESA, 848, 480, 60, 0xe },
            { EstablishedTiming::Source::VESA, 800, 600, 85, 0xc },
            { EstablishedTiming::Source::VESA, 1024, 768, 85, 0x13 },
            { EstablishedTiming::Source::VESA, 1152, 864, 75, 0x15 },
            // Byte 2
            { EstablishedTiming::Source::VESA, 1280, 768, 60, 0x16 },
            { EstablishedTiming::Source::VESA, 1280, 768, 60, 0x17 },
            { EstablishedTiming::Source::VESA, 1280, 768, 75, 0x18 },
            { EstablishedTiming::Source::VESA, 1280, 768, 85, 0x19 },
            { EstablishedTiming::Source::VESA, 1280, 960, 60, 0x20 },
            { EstablishedTiming::Source::VESA, 1280, 960, 85, 0x21 },
            { EstablishedTiming::Source::VESA, 1280, 1024, 60, 0x23 },
            { EstablishedTiming::Source::VESA, 1280, 1024, 85, 0x25 },
            // Byte 3
            { EstablishedTiming::Source::VESA, 1360, 768, 60, 0x27 },
            { EstablishedTiming::Source::VESA, 1440, 900, 60, 0x2e },
            { EstablishedTiming::Source::VESA, 1440, 900, 60, 0x2f },
            { EstablishedTiming::Source::VESA, 1440, 900, 75, 0x30 },
            { EstablishedTiming::Source::VESA, 1440, 900, 85, 0x31 },
            { EstablishedTiming::Source::VESA, 1400, 1050, 60, 0x29 },
            { EstablishedTiming::Source::VESA, 1400, 1050, 60, 0x2a },
            { EstablishedTiming::Source::VESA, 1400, 1050, 75, 0x2b },
            // Byte 4
            { EstablishedTiming::Source::VESA, 1400, 1050, 85, 0x2c },
            { EstablishedTiming::Source::VESA, 1680, 1050, 60, 0x39 },
            { EstablishedTiming::Source::VESA, 1680, 1050, 60, 0x3a },
            { EstablishedTiming::Source::VESA, 1680, 1050, 75, 0x3b },
            { EstablishedTiming::Source::VESA, 1680, 1050, 85, 0x3c },
            { EstablishedTiming::Source::VESA, 1600, 1200, 60, 0x33 },
            { EstablishedTiming::Source::VESA, 1600, 1200, 65, 0x34 },
            { EstablishedTiming::Source::VESA, 1600, 1200, 70, 0x35 },
            // Byte 5
            { EstablishedTiming::Source::VESA, 1600, 1200, 75, 0x36 },
            { EstablishedTiming::Source::VESA, 1600, 1200, 85, 0x37 },
            { EstablishedTiming::Source::VESA, 1792, 1344, 60, 0x3e },
            { EstablishedTiming::Source::VESA, 1792, 1344, 75, 0x3f },
            { EstablishedTiming::Source::VESA, 1856, 1392, 60, 0x41 },
            { EstablishedTiming::Source::VESA, 1856, 1392, 75, 0x42 },
            { EstablishedTiming::Source::VESA, 1920, 1200, 60, 0x44 },
            { EstablishedTiming::Source::VESA, 1920, 1200, 60, 0x45 },
            // Byte 6
            { EstablishedTiming::Source::VESA, 1920, 1200, 75, 0x46 },
            { EstablishedTiming::Source::VESA, 1920, 1200, 85, 0x47 },
            { EstablishedTiming::Source::VESA, 1920, 1440, 60, 0x49 },
            { EstablishedTiming::Source::VESA, 1920, 1440, 75, 0x4a }
            // Reserved
        };

        size_t byte_index = 0;
        for (u8 dmt_bits : display_descriptor.established_timings3.dmt_bits) {
            for (int i = 7; i >= 0; i--) {
                if ((dmt_bits & (1 << i)) == 0)
                    continue;

                size_t table_index = byte_index * 8 + (size_t)(7 - i);
                if (table_index >= (sizeof(established_timings3_bytes) + 7) / sizeof(established_timings3_bytes[0]))
                    break; // Sometimes reserved bits are set

                callback_decision = callback(established_timings3_bytes[table_index]);
                if (callback_decision != IterationDecision::Continue)
                    return IterationDecision::Break;
            }
            byte_index++;
        }
        return IterationDecision::Break; // Only process one descriptor
    }));
    return callback_decision;
}

ErrorOr<IterationDecision> Parser::for_each_standard_timing(Function<IterationDecision(StandardTiming const&)> callback) const
{
    for (size_t index = 0; index < 8; index++) {
        auto& standard_timings = raw_edid().standard_timings[index];
        if (standard_timings.horizontal_8_pixels == 0x1 && standard_timings.ratio_and_refresh_rate == 0x1)
            continue; // Skip unused records
        u16 width = 8 * ((u16)read_host(&standard_timings.horizontal_8_pixels) + 31);
        u8 aspect_ratio_and_refresh_rate = read_host(&standard_timings.ratio_and_refresh_rate);
        u8 refresh_rate = (aspect_ratio_and_refresh_rate & 0x3f) + 60;
        u16 height;
        StandardTiming::AspectRatio aspect_ratio;
        switch ((aspect_ratio_and_refresh_rate >> 6) & 3) {
        case 0:
            height = (width * 10) / 16;
            aspect_ratio = StandardTiming::AspectRatio::AR_16_10;
            break;
        case 1:
            height = (width * 3) / 4;
            aspect_ratio = StandardTiming::AspectRatio::AR_4_3;
            break;
        case 2:
            height = (width * 4) / 5;
            aspect_ratio = StandardTiming::AspectRatio::AR_5_4;
            break;
        case 3:
            height = (width * 9) / 16;
            aspect_ratio = StandardTiming::AspectRatio::AR_16_9;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        auto* dmt = DMT::find_timing_by_std_id(standard_timings.horizontal_8_pixels, standard_timings.ratio_and_refresh_rate);
        IterationDecision decision = callback(StandardTiming(width, height, refresh_rate, aspect_ratio, dmt ? dmt->dmt_id : 0));
        if (decision != IterationDecision::Continue)
            return decision;
    }

    return IterationDecision::Continue;
}

u16 Parser::CoordinatedVideoTiming::horizontal_addressable_pixels() const
{
    u32 aspect_h, aspect_v;
    switch (aspect_ratio()) {
    case AspectRatio::AR_4_3:
        aspect_h = 4;
        aspect_v = 3;
        break;
    case AspectRatio::AR_16_9:
        aspect_h = 16;
        aspect_v = 9;
        break;
    case AspectRatio::AR_16_10:
        aspect_h = 16;
        aspect_v = 10;
        break;
    case AspectRatio::AR_15_9:
        aspect_h = 15;
        aspect_v = 9;
        break;
    }
    // Round down to nearest cell as per 3.10.3.8
    return (u16)(8 * ((((u32)vertical_addressable_lines() * aspect_h) / aspect_v) / 8));
}

u16 Parser::CoordinatedVideoTiming::vertical_addressable_lines() const
{
    return ((u16)(m_cvt.bytes[1] >> 4) << 8) | (u16)m_cvt.bytes[0];
}

auto Parser::CoordinatedVideoTiming::aspect_ratio() const -> AspectRatio
{
    return (AspectRatio)((m_cvt.bytes[2] >> 2) & 0x3);
}

u16 Parser::CoordinatedVideoTiming::preferred_refresh_rate()
{
    switch ((m_cvt.bytes[2] >> 5) & 3) {
    case 0:
        return 50;
    case 1:
        return 60;
    case 2:
        return 75;
    case 3:
        return 85;
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<IterationDecision> Parser::for_each_coordinated_video_timing(Function<IterationDecision(CoordinatedVideoTiming const&)> callback) const
{
    return for_each_display_descriptor([&](u8 descriptor_tag, Definitions::DisplayDescriptor const& display_descriptor) {
        if (descriptor_tag != (u8)Definitions::DisplayDescriptorTag::CVTTimingCodes)
            return IterationDecision::Continue;
        u8 version = read_host(&display_descriptor.coordinated_video_timings.version);
        if (version != 1) {
            dbgln("Unsupported CVT display descriptor version: {}", version);
            return IterationDecision::Continue;
        }

        for (size_t i = 0; i < 4; i++) {
            const DMT::CVT cvt {
                {
                    read_host(&display_descriptor.coordinated_video_timings.cvt[i][0]),
                    read_host(&display_descriptor.coordinated_video_timings.cvt[i][1]),
                    read_host(&display_descriptor.coordinated_video_timings.cvt[i][2]),
                }
            };
            if (cvt.bytes[0] == 0 && cvt.bytes[1] == 0 && cvt.bytes[2] == 0)
                continue;

            IterationDecision decision = callback(CoordinatedVideoTiming(cvt));
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    });
}

ErrorOr<IterationDecision> Parser::for_each_detailed_timing(Function<IterationDecision(DetailedTiming const&, unsigned)> callback) const
{
    auto& edid = raw_edid();
    for (size_t raw_index = 0; raw_index < 4; raw_index++) {
        if (raw_index == 0 || read_le(&edid.detailed_timing_or_display_descriptors[raw_index].detailed_timing.pixel_clock) != 0) {
            IterationDecision decision = callback(DetailedTiming(*this, &edid.detailed_timing_or_display_descriptors[raw_index].detailed_timing), 0);
            if (decision != IterationDecision::Continue)
                return decision;
        }
    }

    Optional<Error> extension_error;
    auto result = TRY(for_each_extension_block([&](u8 block_id, u8 tag, u8, ReadonlyBytes bytes) {
        if (tag != (u8)Definitions::ExtensionBlockTag::CEA_861)
            return IterationDecision::Continue;

        CEA861ExtensionBlock cea861(*this, (Definitions::ExtensionBlock const*)bytes.data());
        auto result = cea861.for_each_dtd([&](auto& dtd) {
            return callback(dtd, block_id);
        });
        if (result.is_error()) {
            dbgln("Failed to iterate DTDs in CEA861 extension block: {}", result.error());
            extension_error = result.release_error();
            return IterationDecision::Break;
        }

        return result.value();
    }));
    if (extension_error.has_value())
        return extension_error.release_value();
    return result;
}

auto Parser::detailed_timing(size_t index) const -> Optional<DetailedTiming>
{
    Optional<DetailedTiming> found_dtd;
    auto result = for_each_detailed_timing([&](DetailedTiming const& dtd, unsigned) {
        if (index == 0) {
            found_dtd = dtd;
            return IterationDecision::Break;
        }
        index--;
        return IterationDecision::Continue;
    });
    if (result.is_error()) {
        dbgln("Error getting Parser detailed timing #{}: {}", index, result.error());
        return {};
    }
    return found_dtd;
}

ErrorOr<IterationDecision> Parser::for_each_short_video_descriptor(Function<IterationDecision(unsigned, bool, VIC::Details const&)> callback) const
{
    Optional<Error> extension_error;
    auto result = for_each_extension_block([&](u8 block_id, u8 tag, u8, ReadonlyBytes bytes) {
        if (tag != (u8)Definitions::ExtensionBlockTag::CEA_861)
            return IterationDecision::Continue;

        CEA861ExtensionBlock cea861(*this, (Definitions::ExtensionBlock const*)bytes.data());
        auto result = cea861.for_each_short_video_descriptor([&](bool is_native, VIC::Details const& vic) {
            return callback(block_id, is_native, vic);
        });
        if (result.is_error()) {
            extension_error = result.release_error();
            return IterationDecision::Break;
        }
        return result.value();
    });
    if (result.is_error()) {
        dbgln("Failed to iterate Parser extension blocks: {}", result.error());
        return IterationDecision::Break;
    }
    return result.value();
}

ErrorOr<IterationDecision> Parser::for_each_display_descriptor(Function<IterationDecision(u8, Definitions::DisplayDescriptor const&)> callback) const
{
    auto& edid = raw_edid();
    for (size_t raw_index = 1; raw_index < 4; raw_index++) {
        auto& display_descriptor = edid.detailed_timing_or_display_descriptors[raw_index].display_descriptor;
        if (read_le(&display_descriptor.zero) != 0 || read_host(&display_descriptor.reserved1) != 0)
            continue;

        u8 tag = read_host(&display_descriptor.tag);
        IterationDecision decision = callback(tag, display_descriptor);
        if (decision != IterationDecision::Continue)
            return decision;
    }

    Optional<Error> extension_error;
    auto result = TRY(for_each_extension_block([&](u8, u8 tag, u8, ReadonlyBytes bytes) {
        if (tag != (u8)Definitions::ExtensionBlockTag::CEA_861)
            return IterationDecision::Continue;

        CEA861ExtensionBlock cea861(*this, (Definitions::ExtensionBlock const*)bytes.data());
        auto result = cea861.for_each_display_descriptor([&](u8 tag, auto& display_descriptor) {
            return callback(tag, display_descriptor);
        });
        if (result.is_error()) {
            dbgln("Failed to iterate display descriptors in CEA861 extension block: {}", result.error());
            extension_error = result.release_error();
            return IterationDecision::Break;
        }

        return result.value();
    }));
    if (extension_error.has_value())
        return extension_error.release_value();
    return result;
}

#ifndef KERNEL
ByteString Parser::display_product_name() const
{
    ByteString product_name;
    auto result = for_each_display_descriptor([&](u8 descriptor_tag, Definitions::DisplayDescriptor const& display_descriptor) {
        if (descriptor_tag != (u8)Definitions::DisplayDescriptorTag::DisplayProductName)
            return IterationDecision::Continue;

        StringBuilder str;
        for (u8 byte : display_descriptor.display_product_name.ascii_name) {
            if (byte == 0xa)
                break;
            str.append((char)byte);
        }
        product_name = str.to_byte_string();
        return IterationDecision::Break;
    });
    if (result.is_error()) {
        dbgln("Failed to locate product name display descriptor: {}", result.error());
        return {};
    }
    return product_name;
}

ByteString Parser::display_product_serial_number() const
{
    ByteString product_name;
    auto result = for_each_display_descriptor([&](u8 descriptor_tag, Definitions::DisplayDescriptor const& display_descriptor) {
        if (descriptor_tag != (u8)Definitions::DisplayDescriptorTag::DisplayProductSerialNumber)
            return IterationDecision::Continue;

        StringBuilder str;
        for (u8 byte : display_descriptor.display_product_serial_number.ascii_str) {
            if (byte == 0xa)
                break;
            str.append((char)byte);
        }
        product_name = str.to_byte_string();
        return IterationDecision::Break;
    });
    if (result.is_error()) {
        dbgln("Failed to locate product name display descriptor: {}", result.error());
        return {};
    }
    return product_name;
}
#endif

auto Parser::supported_resolutions() const -> ErrorOr<Vector<SupportedResolution>>
{
    Vector<SupportedResolution> resolutions;

    auto add_resolution = [&](unsigned width, unsigned height, FixedPoint<16, u32> refresh_rate, bool preferred = false) {
        auto it = resolutions.find_if([&](auto& info) {
            return info.width == width && info.height == height;
        });
        if (it == resolutions.end()) {
            resolutions.append({ width, height, { { refresh_rate, preferred } } });
        } else {
            auto& info = *it;
            SupportedResolution::RefreshRate* found_refresh_rate = nullptr;
            for (auto& supported_refresh_rate : info.refresh_rates) {
                if (supported_refresh_rate.rate == refresh_rate) {
                    found_refresh_rate = &supported_refresh_rate;
                    break;
                }
            }
            if (found_refresh_rate)
                found_refresh_rate->preferred |= preferred;
            else
                info.refresh_rates.append({ refresh_rate, preferred });
        }
    };

    TRY(for_each_established_timing([&](auto& established_timing) {
        if (established_timing.source() != EstablishedTiming::Source::Manufacturer)
            add_resolution(established_timing.width(), established_timing.height(), established_timing.refresh_rate());
        return IterationDecision::Continue;
    }));

    TRY(for_each_standard_timing([&](auto& standard_timing) {
        add_resolution(standard_timing.width(), standard_timing.height(), standard_timing.refresh_rate());
        return IterationDecision::Continue;
    }));

    size_t detailed_timing_index = 0;
    auto detailed_timing_result = for_each_detailed_timing([&](auto& detailed_timing, auto) {
        bool is_preferred = detailed_timing_index++ == 0;
        add_resolution(detailed_timing.horizontal_addressable_pixels(), detailed_timing.vertical_addressable_lines(), detailed_timing.refresh_rate(), is_preferred);
        return IterationDecision::Continue;
    });

    if (detailed_timing_result.is_error())
        dbgln("Failed to process detailed timing data: {}", detailed_timing_result.error());

    auto short_video_descriptor_result = for_each_short_video_descriptor([&](unsigned, bool, VIC::Details const& vic_details) {
        add_resolution(vic_details.horizontal_pixels, vic_details.vertical_lines, vic_details.refresh_rate_hz());
        return IterationDecision::Continue;
    });

    if (short_video_descriptor_result.is_error())
        dbgln("Failed to process short video descriptors: {}", short_video_descriptor_result.error());

    auto coordinated_video_timings_result = for_each_coordinated_video_timing([&](auto& coordinated_video_timing) {
        if (auto* dmt = DMT::find_timing_by_cvt(coordinated_video_timing.cvt_code())) {
            add_resolution(dmt->horizontal_pixels, dmt->vertical_lines, dmt->vertical_frequency_hz());
        } else {
            // TODO: We couldn't find this cvt code, try to decode it
            auto cvt = coordinated_video_timing.cvt_code();
            dbgln("TODO: Decode CVT code: {:02x},{:02x},{:02x}", cvt.bytes[0], cvt.bytes[1], cvt.bytes[2]);
        }
        return IterationDecision::Continue;
    });

    if (coordinated_video_timings_result.is_error())
        dbgln("Failed to process coordinated video timing results: {}", coordinated_video_timings_result.error());

    quick_sort(resolutions, [&](auto& info1, auto& info2) {
        if (info1.width < info2.width)
            return true;
        if (info1.width == info2.width && info1.height < info2.height)
            return true;
        return false;
    });

    for (auto& res : resolutions) {
        if (res.refresh_rates.size() > 1)
            quick_sort(res.refresh_rates);
    }
    return resolutions;
}

}
