/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGPU/Enums.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGPU/ImageFormat.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Buffer/Typed3DBuffer.h>
#include <LibSoftGPU/Config.h>

namespace SoftGPU {

inline static FloatVector4 unpack_color(void const* ptr, GPU::ImageFormat format)
{
    constexpr auto one_over_255 = 1.0f / 255;
    switch (format) {
    case GPU::ImageFormat::RGB888: {
        auto rgb = reinterpret_cast<u8 const*>(ptr);
        return {
            rgb[0] * one_over_255,
            rgb[1] * one_over_255,
            rgb[2] * one_over_255,
            1.0f,
        };
    }
    case GPU::ImageFormat::BGR888: {
        auto bgr = reinterpret_cast<u8 const*>(ptr);
        return {
            bgr[2] * one_over_255,
            bgr[1] * one_over_255,
            bgr[0] * one_over_255,
            1.0f,
        };
    }
    case GPU::ImageFormat::RGBA8888: {
        auto rgba = *reinterpret_cast<u32 const*>(ptr);
        return {
            (rgba & 0xff) * one_over_255,
            ((rgba >> 8) & 0xff) * one_over_255,
            ((rgba >> 16) & 0xff) * one_over_255,
            ((rgba >> 24) & 0xff) * one_over_255,
        };
    }
    case GPU::ImageFormat::BGRA8888: {
        auto bgra = *reinterpret_cast<u32 const*>(ptr);
        return {
            ((bgra >> 16) & 0xff) * one_over_255,
            ((bgra >> 8) & 0xff) * one_over_255,
            (bgra & 0xff) * one_over_255,
            ((bgra >> 24) & 0xff) * one_over_255,
        };
    }
    case GPU::ImageFormat::RGB565: {
        auto rgb = *reinterpret_cast<u16 const*>(ptr);
        return {
            ((rgb >> 11) & 0x1f) / 31.f,
            ((rgb >> 5) & 0x3f) / 63.f,
            (rgb & 0x1f) / 31.f,
            1.0f
        };
    }
    case GPU::ImageFormat::L8: {
        auto luminance = *reinterpret_cast<u8 const*>(ptr);
        auto clamped_luminance = luminance * one_over_255;
        return {
            clamped_luminance,
            clamped_luminance,
            clamped_luminance,
            1.0f,
        };
    }
    case GPU::ImageFormat::L8A8: {
        auto luminance_and_alpha = reinterpret_cast<u8 const*>(ptr);
        auto clamped_luminance = luminance_and_alpha[0] * one_over_255;
        return {
            clamped_luminance,
            clamped_luminance,
            clamped_luminance,
            luminance_and_alpha[1] * one_over_255,
        };
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

inline static void pack_color(FloatVector4 const& color, void* ptr, GPU::ImageFormat format)
{
    auto r = static_cast<u8>(clamp(color.x(), 0.0f, 1.0f) * 255);
    auto g = static_cast<u8>(clamp(color.y(), 0.0f, 1.0f) * 255);
    auto b = static_cast<u8>(clamp(color.z(), 0.0f, 1.0f) * 255);
    auto a = static_cast<u8>(clamp(color.w(), 0.0f, 1.0f) * 255);

    switch (format) {
    case GPU::ImageFormat::RGB888:
        reinterpret_cast<u8*>(ptr)[0] = r;
        reinterpret_cast<u8*>(ptr)[1] = g;
        reinterpret_cast<u8*>(ptr)[2] = b;
        return;
    case GPU::ImageFormat::BGR888:
        reinterpret_cast<u8*>(ptr)[2] = b;
        reinterpret_cast<u8*>(ptr)[1] = g;
        reinterpret_cast<u8*>(ptr)[0] = r;
        return;
    case GPU::ImageFormat::RGBA8888:
        *reinterpret_cast<u32*>(ptr) = r | (g << 8) | (b << 16) | (a << 24);
        return;
    case GPU::ImageFormat::BGRA8888:
        *reinterpret_cast<u32*>(ptr) = b | (g << 8) | (r << 16) | (a << 24);
        return;
    case GPU::ImageFormat::RGB565:
        *reinterpret_cast<u16*>(ptr) = (r & 0x1f) | ((g & 0x3f) << 5) | ((b & 0x1f) << 11);
        return;
    case GPU::ImageFormat::L8:
        *reinterpret_cast<u8*>(ptr) = r;
        return;
    case GPU::ImageFormat::L8A8:
        reinterpret_cast<u8*>(ptr)[0] = r;
        reinterpret_cast<u8*>(ptr)[1] = a;
        return;
    default:
        VERIFY_NOT_REACHED();
    }
}

class Image final : public RefCounted<Image> {
public:
    Image(unsigned width, unsigned height, unsigned depth, unsigned max_levels, unsigned layers);

    unsigned level_width(unsigned level) const { return m_mipmap_buffers[level]->width(); }
    unsigned level_height(unsigned level) const { return m_mipmap_buffers[level]->height(); }
    unsigned level_depth(unsigned level) const { return m_mipmap_buffers[level]->depth(); }
    unsigned num_levels() const { return m_num_levels; }
    unsigned num_layers() const { return m_num_layers; }
    bool width_is_power_of_two() const { return m_width_is_power_of_two; }
    bool height_is_power_of_two() const { return m_height_is_power_of_two; }
    bool depth_is_power_of_two() const { return m_depth_is_power_of_two; }

    FloatVector4 texel(unsigned layer, unsigned level, int x, int y, int z) const
    {
        return unpack_color(texel_pointer(layer, level, x, y, z), GPU::ImageFormat::BGRA8888);
    }

    void set_texel(unsigned layer, unsigned level, int x, int y, int z, FloatVector4 const& color)
    {
        pack_color(color, texel_pointer(layer, level, x, y, z), GPU::ImageFormat::BGRA8888);
    }

    void write_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void const* data, GPU::ImageDataLayout const& layout);
    void read_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void* data, GPU::ImageDataLayout const& layout) const;
    void copy_texels(Image const& source, unsigned source_layer, unsigned source_level, Vector3<unsigned> const& source_offset, Vector3<unsigned> const& size, unsigned destination_layer, unsigned destination_level, Vector3<unsigned> const& destination_offset);

private:
    void const* texel_pointer(unsigned layer, unsigned level, int x, int y, int z) const
    {
        return m_mipmap_buffers[layer * m_num_layers + level]->buffer_pointer(x, y, z);
    }

    void* texel_pointer(unsigned layer, unsigned level, int x, int y, int z)
    {
        return m_mipmap_buffers[layer * m_num_layers + level]->buffer_pointer(x, y, z);
    }

private:
    unsigned m_num_levels { 0 };
    unsigned m_num_layers { 0 };

    FixedArray<RefPtr<Typed3DBuffer<GPU::ColorType>>> m_mipmap_buffers;

    bool m_width_is_power_of_two { false };
    bool m_height_is_power_of_two { false };
    bool m_depth_is_power_of_two { false };
};

}
