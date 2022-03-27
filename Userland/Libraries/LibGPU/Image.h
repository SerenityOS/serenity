/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGfx/Vector3.h>

namespace GPU {

class Image : public RefCounted<Image> {
public:
    Image(void const* ownership_token)
        : m_ownership_token { ownership_token }
    {
    }

    virtual ~Image() { }

    virtual void write_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void const* data, ImageDataLayout const& layout) = 0;
    virtual void read_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void* data, ImageDataLayout const& layout) const = 0;
    virtual void copy_texels(Image const& source, unsigned source_layer, unsigned source_level, Vector3<unsigned> const& source_offset, Vector3<unsigned> const& size, unsigned destination_layer, unsigned destination_level, Vector3<unsigned> const& destination_offset) = 0;

    void const* ownership_token() const { return m_ownership_token; }
    bool has_same_ownership_token(Image const& other) const { return other.ownership_token() == ownership_token(); }

private:
    void const* const m_ownership_token { nullptr };
};

}
