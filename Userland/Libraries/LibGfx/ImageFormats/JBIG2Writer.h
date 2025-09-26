/*
 * Copyright (c) 2024-2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Variant.h>
#include <LibGfx/Forward.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>

namespace Gfx {

struct JBIG2EncoderOptions {
};

// Structs in this namespace can be used to explicitly construct JBIG2 files with a certain structure.
// This is useful for making test inputs, and not useful else. Usually you want to use the usual encode() API instead.
namespace JBIG2 {

struct SegmentHeaderData {
    u32 segment_number { 0 };
    bool retention_flag { false };

    struct Reference {
        u32 segment_number { 0 };
        bool retention_flag { false };
    };
    Vector<Reference> referred_to_segments;

    // 7.2.6 Segment page association
    // "The first page must be numbered "1". This field may contain a value of zero; this value indicates that this segment is not associated with any page."
    u32 page_association { 0 };
};

struct GenericRegionSegmentData {
    RegionSegmentInformationField region_segment_information {};
    u8 flags { 0 };
    Array<AdaptiveTemplatePixel, 12> adaptive_template_pixels {};
    NonnullOwnPtr<BilevelImage> image;
};

struct ImmediateGenericRegionSegmentData {
    GenericRegionSegmentData generic_region;
};

struct ImmediateLosslessGenericRegionSegmentData {
    GenericRegionSegmentData generic_region;
};

struct EndOfPageSegmentData { };

struct SegmentData {
    SegmentHeaderData header;
    Variant<JBIG2::PageInformationSegment, EndOfPageSegmentData, ImmediateGenericRegionSegmentData, ImmediateLosslessGenericRegionSegmentData> data;
};

struct FileHeaderData {
    Organization organization { Organization::Sequential };
    Optional<u32> number_of_pages { 0 };
    bool force_32bit_page_numbers { false };
};

struct FileData {
    FileHeaderData header;
    Vector<SegmentData> segments;
};

};

class JBIG2Writer {
public:
    using Options = JBIG2EncoderOptions;

    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});

    static ErrorOr<void> encode_with_explicit_data(Stream&, JBIG2::FileData const&);

private:
    JBIG2Writer() = delete;
};

}
