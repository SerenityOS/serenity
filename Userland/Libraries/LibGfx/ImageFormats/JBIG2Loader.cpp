/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/ImageFormats/JBIG2Loader.h>

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I

namespace Gfx {

// JBIG2 spec, Annex D, D.4.1 ID string
static constexpr u8 id_string[] = { 0x97, 0x4A, 0x42, 0x32, 0x0D, 0x0A, 0x1A, 0x0A };

// Annex D
enum class Organization {
    // D.1 Sequential organization
    Sequential,

    // D.2 Random-access organization
    RandomAccess,

    // D.3 Embedded organization
    Embedded,
};

struct JBIG2LoadingContext {
    enum class State {
        NotDecoded = 0,
        Error,
    };
    State state { State::NotDecoded };
    ReadonlyBytes data;

    Organization organization { Organization::Sequential };
    IntSize size;

    Optional<u32> number_of_pages;
};

static ErrorOr<void> decode_jbig2_header(JBIG2LoadingContext& context)
{
    if (!JBIG2ImageDecoderPlugin::sniff(context.data))
        return Error::from_string_literal("JBIG2LoadingContext: Invalid JBIG2 header");

    FixedMemoryStream stream(context.data.slice(sizeof(id_string)));

    // D.4.2 File header flags
    u8 header_flags = TRY(stream.read_value<u8>());
    if (header_flags & 0b11110000)
        return Error::from_string_literal("JBIG2LoadingContext: Invalid header flags");
    context.organization = (header_flags & 1) ? Organization::Sequential : Organization::RandomAccess;
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

JBIG2ImageDecoderPlugin::JBIG2ImageDecoderPlugin(ReadonlyBytes data)
{
    m_context = make<JBIG2LoadingContext>();
    m_context->data = data;
}

IntSize JBIG2ImageDecoderPlugin::size()
{
    return m_context->size;
}

bool JBIG2ImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.starts_with(id_string);
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JBIG2ImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JBIG2ImageDecoderPlugin(data)));
    TRY(decode_jbig2_header(*plugin->m_context));
    return plugin;
}

ErrorOr<ImageFrameDescriptor> JBIG2ImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    // FIXME: Use this for multi-page JBIG2 files?
    if (index != 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Invalid frame index");

    if (m_context->state == JBIG2LoadingContext::State::Error)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Decoding failed");

    return Error::from_string_literal("JBIG2ImageDecoderPlugin: Draw the rest of the owl");
}

}
