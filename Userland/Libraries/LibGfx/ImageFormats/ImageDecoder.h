/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>
#include <LibGfx/VectorGraphic.h>

namespace Gfx {

class Bitmap;

static constexpr size_t maximum_width_for_decoded_images = 16384;
static constexpr size_t maximum_height_for_decoded_images = 16384;

struct ImageFrameDescriptor {
    RefPtr<Bitmap> image;
    int duration { 0 };
};

struct VectorImageFrameDescriptor {
    RefPtr<VectorGraphic> image;
    int duration { 0 };
};

class ImageDecoderPlugin {
public:
    virtual ~ImageDecoderPlugin() = default;

    // Each plugin should implement these static functions and register them in ImageDecoder.cpp
    // Implement sniff() if the file includes a magic number
    // static bool sniff(ReadonlyBytes);
    // Implement validate_before_create() otherwise
    // static ErrorOr<bool> validate_before_create(ReadonlyBytes);

    // This function should be used to both create the context and parse the image header.
    // static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    // This should always be available as gathered in create()
    virtual IntSize size() = 0;

    // Override this if the format supports animated images
    virtual bool is_animated() { return false; }
    virtual size_t loop_count() { return 0; }
    virtual size_t frame_count() { return 1; }
    virtual size_t first_animated_frame_index() { return 0; }

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) = 0;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() = 0;

    virtual bool is_vector() { return false; }
    virtual ErrorOr<VectorImageFrameDescriptor> vector_frame(size_t) { VERIFY_NOT_REACHED(); }

protected:
    ImageDecoderPlugin() = default;
};

class ImageDecoder : public RefCounted<ImageDecoder> {
public:
    static RefPtr<ImageDecoder> try_create_for_raw_bytes(ReadonlyBytes, Optional<DeprecatedString> mime_type = {});
    ~ImageDecoder() = default;

    IntSize size() const { return m_plugin->size(); }
    int width() const { return size().width(); }
    int height() const { return size().height(); }
    bool is_animated() const { return m_plugin->is_animated(); }
    size_t loop_count() const { return m_plugin->loop_count(); }
    size_t frame_count() const { return m_plugin->frame_count(); }
    size_t first_animated_frame_index() const { return m_plugin->first_animated_frame_index(); }
    ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) const { return m_plugin->frame(index, ideal_size); }
    ErrorOr<Optional<ReadonlyBytes>> icc_data() const { return m_plugin->icc_data(); }

    bool is_vector() { return m_plugin->is_vector(); }
    ErrorOr<VectorImageFrameDescriptor> vector_frame(size_t index) { return m_plugin->vector_frame(index); }

private:
    explicit ImageDecoder(NonnullOwnPtr<ImageDecoderPlugin>);

    NonnullOwnPtr<ImageDecoderPlugin> mutable m_plugin;
};

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

}
