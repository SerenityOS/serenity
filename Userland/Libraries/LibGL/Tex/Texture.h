/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
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

    RefPtr<GPU::Image> device_image() const { return m_device_image; }
    RefPtr<GPU::Image> device_image() { return m_device_image; }
    void set_device_image(RefPtr<GPU::Image> image) { m_device_image = image; }

    float level_of_detail_bias() const { return m_level_of_detail_bias; }
    void set_level_of_detail_bias(float level_of_detail_bias) { m_level_of_detail_bias = level_of_detail_bias; }

private:
    RefPtr<GPU::Image> m_device_image;
    float m_level_of_detail_bias { 0.f };
};

}
