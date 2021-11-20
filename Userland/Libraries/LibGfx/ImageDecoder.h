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

namespace Gfx {

class Bitmap;

static constexpr size_t maximum_width_for_decoded_images = 16384;
static constexpr size_t maximum_height_for_decoded_images = 16384;

struct ImageFrameDescriptor {
    RefPtr<Bitmap> image;
    int duration { 0 };
};

class ImageDecoderPlugin {
public:
    virtual ~ImageDecoderPlugin() { }

    virtual IntSize size() = 0;

    virtual void set_volatile() = 0;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) = 0;

    virtual bool sniff() = 0;

    virtual bool is_animated() = 0;
    virtual size_t loop_count() = 0;
    virtual size_t frame_count() = 0;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) = 0;

protected:
    ImageDecoderPlugin() { }
};

class ImageDecoder : public RefCounted<ImageDecoder> {
public:
    static RefPtr<ImageDecoder> try_create(ReadonlyBytes);
    ~ImageDecoder();

    IntSize size() const { return m_plugin->size(); }
    int width() const { return size().width(); }
    int height() const { return size().height(); }
    void set_volatile() { m_plugin->set_volatile(); }
    [[nodiscard]] bool set_nonvolatile(bool& was_purged) { return m_plugin->set_nonvolatile(was_purged); }
    bool sniff() const { return m_plugin->sniff(); }
    bool is_animated() const { return m_plugin->is_animated(); }
    size_t loop_count() const { return m_plugin->loop_count(); }
    size_t frame_count() const { return m_plugin->frame_count(); }
    ErrorOr<ImageFrameDescriptor> frame(size_t index) const { return m_plugin->frame(index); }

private:
    explicit ImageDecoder(NonnullOwnPtr<ImageDecoderPlugin>);

    NonnullOwnPtr<ImageDecoderPlugin> mutable m_plugin;
};

}
