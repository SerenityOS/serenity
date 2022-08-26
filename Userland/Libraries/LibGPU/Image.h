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

    virtual void write_texels(u32 layer, u32 level, Vector3<i32> const& output_offset, void const* input_data, ImageDataLayout const&) = 0;
    virtual void read_texels(u32 layer, u32 level, Vector3<i32> const& input_offset, void* output_data, ImageDataLayout const&) const = 0;
    virtual void copy_texels(Image const& source, u32 source_layer, u32 source_level, Vector3<u32> const& source_offset, Vector3<u32> const& size, u32 destination_layer, u32 destination_level, Vector3<u32> const& destination_offset) = 0;

    void const* ownership_token() const { return m_ownership_token; }
    bool has_same_ownership_token(Image const& other) const { return other.ownership_token() == ownership_token(); }

private:
    void const* const m_ownership_token { nullptr };
};

}
