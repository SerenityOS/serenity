/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGfx/Size.h>

namespace Gfx {

class Bitmap;

struct ImageFrameDescriptor {
    RefPtr<Bitmap> image;
    int duration { 0 };
};

class ImageDecoderPlugin {
public:
    virtual ~ImageDecoderPlugin() { }

    virtual IntSize size() = 0;
    virtual RefPtr<Gfx::Bitmap> bitmap() = 0;

    virtual void set_volatile() = 0;
    [[nodiscard]] virtual bool set_nonvolatile() = 0;

    virtual bool sniff() = 0;

    virtual bool is_animated() = 0;
    virtual size_t loop_count() = 0;
    virtual size_t frame_count() = 0;
    virtual ImageFrameDescriptor frame(size_t i) = 0;

protected:
    ImageDecoderPlugin() { }
};

class ImageDecoder : public RefCounted<ImageDecoder> {
public:
    static NonnullRefPtr<ImageDecoder> create(const u8* data, size_t size) { return adopt(*new ImageDecoder(data, size)); }
    static NonnullRefPtr<ImageDecoder> create(const ByteBuffer& data) { return adopt(*new ImageDecoder(data.data(), data.size())); }
    ~ImageDecoder();

    bool is_valid() const { return m_plugin; }

    IntSize size() const { return m_plugin ? m_plugin->size() : IntSize(); }
    int width() const { return size().width(); }
    int height() const { return size().height(); }
    RefPtr<Gfx::Bitmap> bitmap() const;
    void set_volatile()
    {
        if (m_plugin)
            m_plugin->set_volatile();
    }
    [[nodiscard]] bool set_nonvolatile() { return m_plugin ? m_plugin->set_nonvolatile() : false; }
    bool sniff() const { return m_plugin ? m_plugin->sniff() : false; }
    bool is_animated() const { return m_plugin ? m_plugin->is_animated() : false; }
    size_t loop_count() const { return m_plugin ? m_plugin->loop_count() : 0; }
    size_t frame_count() const { return m_plugin ? m_plugin->frame_count() : 0; }
    ImageFrameDescriptor frame(size_t i) const { return m_plugin ? m_plugin->frame(i) : ImageFrameDescriptor(); }

private:
    ImageDecoder(const u8*, size_t);

    mutable OwnPtr<ImageDecoderPlugin> m_plugin;
};

}
