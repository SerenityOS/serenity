/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MemoryStream.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

// The DICOM spec is available here https://dicom.nema.org/medical/dicom/.
// Note that links are always targeting the "current" spec, which is 2025c at the time of writing.

// Here is the link to the Registry of DICOM Elements:
// https://dicom.nema.org/medical/dicom/current/output/chtml/part06/chapter_6.html

namespace Gfx {

class DICOMLoadingContext;

class DICOMImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~DICOMImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    virtual Optional<Metadata const&> metadata() override;

private:
    DICOMImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream>);

    OwnPtr<DICOMLoadingContext> m_context;
};

}
