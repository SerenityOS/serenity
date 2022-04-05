/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGPU/Image.h>

namespace GL {

class Texture : public RefCounted<Texture> {
public:
    virtual ~Texture() = default;

    virtual bool is_texture_1d() const { return false; }
    virtual bool is_texture_2d() const { return false; }
    virtual bool is_texture_3d() const { return false; }
    virtual bool is_cube_map() const { return false; }

    RefPtr<GPU::Image> device_image() { return m_device_image; }
    void set_device_image(RefPtr<GPU::Image> image) { m_device_image = image; }

private:
    RefPtr<GPU::Image> m_device_image;
};

}
