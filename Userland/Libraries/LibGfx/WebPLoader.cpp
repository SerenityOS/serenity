/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Format.h>
#include <LibGfx/WebPLoader.h>

// Container: https://developers.google.com/speed/webp/docs/riff_container
// Lossless format: https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification
// Lossy format: https://datatracker.ietf.org/doc/html/rfc6386

namespace Gfx {

namespace {

struct FourCC {
    constexpr FourCC(char const* name)
    {
        cc[0] = name[0];
        cc[1] = name[1];
        cc[2] = name[2];
        cc[3] = name[3];
    }

    bool operator==(FourCC const&) const = default;
    bool operator!=(FourCC const&) const = default;

    char cc[4];
};

// https://developers.google.com/speed/webp/docs/riff_container#webp_file_header
struct WebPFileHeader {
    FourCC riff;
    LittleEndian<u32> file_size;
    FourCC webp;
};
static_assert(AssertSize<WebPFileHeader, 12>());

struct ChunkHeader {
    FourCC chunk_type;
    LittleEndian<u32> chunk_size;
};
static_assert(AssertSize<ChunkHeader, 8>());

struct Chunk {
    FourCC type;
    ReadonlyBytes data;
};

}

struct WebPLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        SizeDecoded,
        ChunksDecoded,
        BitmapDecoded,
    };
    State state { State::NotDecoded };
    ReadonlyBytes data;

    RefPtr<Gfx::Bitmap> bitmap;

    Optional<ReadonlyBytes> icc_data;
};

// https://developers.google.com/speed/webp/docs/riff_container#webp_file_header
static ErrorOr<void> decode_webp_header(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::HeaderDecoded)
        return {};

    if (context.data.size() < sizeof(WebPFileHeader)) {
        context.state = WebPLoadingContext::State::Error;
        return Error::from_string_literal("Missing WebP header");
    }

    auto& header = *bit_cast<WebPFileHeader const*>(context.data.data());
    if (header.riff != FourCC("RIFF") || header.webp != FourCC("WEBP")) {
        context.state = WebPLoadingContext::State::Error;
        return Error::from_string_literal("Invalid WebP header");
    }

    // "File Size: [...] The size of the file in bytes starting at offset 8. The maximum value of this field is 2^32 minus 10 bytes."
    u32 const maximum_webp_file_size = 0xffff'ffff - 9;
    if (header.file_size > maximum_webp_file_size) {
        context.state = WebPLoadingContext::State::Error;
        return Error::from_string_literal("WebP header file size over maximum");
    }

    // "The file size in the header is the total size of the chunks that follow plus 4 bytes for the 'WEBP' FourCC.
    //  The file SHOULD NOT contain any data after the data specified by File Size.
    //  Readers MAY parse such files, ignoring the trailing data."
    if (context.data.size() - 8 < header.file_size) {
        context.state = WebPLoadingContext::State::Error;
        return Error::from_string_literal("WebP data too small for size in header");
    }
    if (context.data.size() - 8 > header.file_size) {
        dbgln_if(WEBP_DEBUG, "WebP has {} bytes of data, but header needs only {}. Trimming.", context.data.size(), header.file_size + 8);
        context.data = context.data.trim(header.file_size + 8);
    }

    context.state = WebPLoadingContext::HeaderDecoded;
    return {};
}

// https://developers.google.com/speed/webp/docs/riff_container#riff_file_format
static ErrorOr<Chunk> decode_webp_chunk_header(WebPLoadingContext& context, ReadonlyBytes chunks)
{
    if (chunks.size() < sizeof(ChunkHeader)) {
        context.state = WebPLoadingContext::State::Error;
        return Error::from_string_literal("Not enough data for WebP chunk header");
    }

    auto const& header = *bit_cast<ChunkHeader const*>(chunks.data());
    dbgln_if(WEBP_DEBUG, "chunk {} size {}", header.chunk_type, header.chunk_size);

    if (chunks.size() < sizeof(ChunkHeader) + header.chunk_size) {
        context.state = WebPLoadingContext::State::Error;
        return Error::from_string_literal("Not enough data for WebP chunk");
    }

    return Chunk { header.chunk_type, { chunks.data() + sizeof(ChunkHeader), header.chunk_size } };
}

// https://developers.google.com/speed/webp/docs/riff_container#riff_file_format
static ErrorOr<Chunk> decode_webp_advance_chunk(WebPLoadingContext& context, ReadonlyBytes& chunks)
{
    auto chunk = TRY(decode_webp_chunk_header(context, chunks));

    // "Chunk Size: 32 bits (uint32)
    //      The size of the chunk in bytes, not including this field, the chunk identifier or padding.
    //  Chunk Payload: Chunk Size bytes
    //      The data payload. If Chunk Size is odd, a single padding byte -- that MUST be 0 to conform with RIFF -- is added."
    chunks = chunks.slice(sizeof(ChunkHeader) + chunk.data.size());

    if (chunk.data.size() % 2 != 0) {
        if (chunks.is_empty()) {
            context.state = WebPLoadingContext::State::Error;
            return Error::from_string_literal("Missing data for padding byte");
        }
        if (*chunks.data() != 0) {
            context.state = WebPLoadingContext::State::Error;
            return Error::from_string_literal("Padding byte is not 0");
        }
        chunks = chunks.slice(1);
    }

    return chunk;
}

// https://developers.google.com/speed/webp/docs/riff_container#simple_file_format_lossy
static ErrorOr<void> decode_webp_simple_lossy(WebPLoadingContext& context, Chunk const& vp8_chunk)
{
    // FIXME
    (void)context;
    (void)vp8_chunk;
    return {};
}

// https://developers.google.com/speed/webp/docs/riff_container#simple_file_format_lossless
static ErrorOr<void> decode_webp_simple_lossless(WebPLoadingContext& context, Chunk const& vp8l_chunk)
{
    // FIXME
    (void)context;
    (void)vp8l_chunk;
    return {};
}

// https://developers.google.com/speed/webp/docs/riff_container#extended_file_format
static ErrorOr<void> decode_webp_extended(WebPLoadingContext& context, Chunk const& vp8x_chunk, ReadonlyBytes chunks)
{

    // FIXME: Do something with this.
    (void)vp8x_chunk;

    // FIXME: This isn't quite to spec, which says
    // "All chunks SHOULD be placed in the same order as listed above.
    //  If a chunk appears in the wrong place, the file is invalid, but readers MAY parse the file, ignoring the chunks that are out of order."
    while (!chunks.is_empty()) {
        auto chunk = TRY(decode_webp_advance_chunk(context, chunks));

        if (chunk.type == FourCC("ICCP"))
            context.icc_data = chunk.data;

        // FIXME: Probably want to make this and decode_webp_simple_lossy/lossless call the same function
        //        instead of calling the _simple functions from the _extended function.
        if (chunk.type == FourCC("VP8 "))
            TRY(decode_webp_simple_lossy(context, chunk));
        if (chunk.type == FourCC("VP8X"))
            TRY(decode_webp_simple_lossless(context, chunk));
    }

    context.state = WebPLoadingContext::State::ChunksDecoded;
    return {};
}

static ErrorOr<void> decode_webp_chunks(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::State::ChunksDecoded)
        return {};

    if (context.state < WebPLoadingContext::HeaderDecoded)
        TRY(decode_webp_header(context));

    ReadonlyBytes chunks = context.data.slice(sizeof(WebPFileHeader));
    auto first_chunk = TRY(decode_webp_advance_chunk(context, chunks));

    if (first_chunk.type == FourCC("VP8 ")) {
        context.state = WebPLoadingContext::State::ChunksDecoded;
        return decode_webp_simple_lossy(context, first_chunk);
    }

    if (first_chunk.type == FourCC("VP8L")) {
        context.state = WebPLoadingContext::State::ChunksDecoded;
        return decode_webp_simple_lossless(context, first_chunk);
    }

    if (first_chunk.type == FourCC("VP8X"))
        return decode_webp_extended(context, first_chunk, chunks);

    return Error::from_string_literal("WebPImageDecoderPlugin: Invalid first chunk type");
}

WebPImageDecoderPlugin::WebPImageDecoderPlugin(ReadonlyBytes data, OwnPtr<WebPLoadingContext> context)
    : m_context(move(context))
{
    m_context->data = data;
}

WebPImageDecoderPlugin::~WebPImageDecoderPlugin() = default;

IntSize WebPImageDecoderPlugin::size()
{
    if (m_context->state == WebPLoadingContext::State::Error)
        return {};

    if (m_context->state < WebPLoadingContext::State::SizeDecoded) {
        // FIXME
    }

    // FIXME
    return { 0, 0 };
}

void WebPImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool WebPImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile(was_purged);
}

bool WebPImageDecoderPlugin::initialize()
{
    return !decode_webp_header(*m_context).is_error();
}

ErrorOr<bool> WebPImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    WebPLoadingContext context;
    context.data = data;
    TRY(decode_webp_header(context));
    return true;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> WebPImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto context = TRY(try_make<WebPLoadingContext>());
    return adopt_nonnull_own_or_enomem(new (nothrow) WebPImageDecoderPlugin(data, move(context)));
}

bool WebPImageDecoderPlugin::is_animated()
{
    // FIXME
    return false;
}

size_t WebPImageDecoderPlugin::loop_count()
{
    // FIXME
    return 0;
}

size_t WebPImageDecoderPlugin::frame_count()
{
    // FIXME
    return 1;
}

ErrorOr<ImageFrameDescriptor> WebPImageDecoderPlugin::frame(size_t index)
{
    if (index >= frame_count())
        return Error::from_string_literal("WebPImageDecoderPlugin: Invalid frame index");

    return Error::from_string_literal("WebPImageDecoderPlugin: decoding not yet implemented");
}

ErrorOr<Optional<ReadonlyBytes>> WebPImageDecoderPlugin::icc_data()
{
    TRY(decode_webp_chunks(*m_context));
    return m_context->icc_data;
}

}

template<>
struct AK::Formatter<Gfx::FourCC> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::FourCC const& four_cc)
    {
        TRY(builder.put_padding('\'', 1));
        TRY(builder.put_padding(four_cc.cc[0], 1));
        TRY(builder.put_padding(four_cc.cc[1], 1));
        TRY(builder.put_padding(four_cc.cc[2], 1));
        TRY(builder.put_padding(four_cc.cc[3], 1));
        TRY(builder.put_padding('\'', 1));
        return {};
    }
};
