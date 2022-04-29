/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/Image.h>

namespace SoftGPU {

static constexpr FloatVector4 unpack_color(void const* ptr, GPU::ImageFormat format)
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

static constexpr void pack_color(FloatVector4 const& color, void* ptr, GPU::ImageFormat format)
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

Image::Image(void* const ownership_token, unsigned width, unsigned height, unsigned depth, unsigned max_levels, unsigned layers)
    : GPU::Image(ownership_token)
    , m_num_layers(layers)
    , m_mipmap_buffers(FixedArray<RefPtr<Typed3DBuffer<FloatVector4>>>::must_create_but_fixme_should_propagate_errors(layers * max_levels))
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(max_levels > 0);
    VERIFY(layers > 0);

    m_width_is_power_of_two = is_power_of_two(width);
    m_height_is_power_of_two = is_power_of_two(height);
    m_depth_is_power_of_two = is_power_of_two(depth);

    unsigned level;
    for (level = 0; level < max_levels; ++level) {
        for (unsigned layer = 0; layer < layers; ++layer)
            m_mipmap_buffers[layer * layers + level] = MUST(Typed3DBuffer<FloatVector4>::try_create(width, height, depth));

        if (width <= 1 && height <= 1 && depth <= 1)
            break;

        width = max(width / 2, 1);
        height = max(height / 2, 1);
        depth = max(depth / 2, 1);
    }

    m_num_levels = level + 1;
}

void Image::write_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void const* data, GPU::ImageDataLayout const& layout)
{
    VERIFY(layer < num_layers());
    VERIFY(level < num_levels());
    VERIFY(offset.x() + size.x() <= level_width(level));
    VERIFY(offset.y() + size.y() <= level_height(level));
    VERIFY(offset.z() + size.z() <= level_depth(level));

    for (unsigned z = 0; z < size.z(); ++z) {
        for (unsigned y = 0; y < size.y(); ++y) {
            for (unsigned x = 0; x < size.x(); ++x) {
                auto ptr = reinterpret_cast<u8 const*>(data) + layout.depth_stride * z + layout.row_stride * y + layout.column_stride * x;
                auto color = unpack_color(ptr, layout.format);
                set_texel(layer, level, offset.x() + x, offset.y() + y, offset.z() + z, color);
            }
        }
    }
}

void Image::read_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void* data, GPU::ImageDataLayout const& layout) const
{
    VERIFY(layer < num_layers());
    VERIFY(level < num_levels());
    VERIFY(offset.x() + size.x() <= level_width(level));
    VERIFY(offset.y() + size.y() <= level_height(level));
    VERIFY(offset.z() + size.z() <= level_depth(level));

    for (unsigned z = 0; z < size.z(); ++z) {
        for (unsigned y = 0; y < size.y(); ++y) {
            for (unsigned x = 0; x < size.x(); ++x) {
                auto color = texel(layer, level, offset.x() + x, offset.y() + y, offset.z() + z);
                auto ptr = reinterpret_cast<u8*>(data) + layout.depth_stride * z + layout.row_stride * y + layout.column_stride * x;
                pack_color(color, ptr, layout.format);
            }
        }
    }
}

void Image::copy_texels(GPU::Image const& source, unsigned source_layer, unsigned source_level, Vector3<unsigned> const& source_offset, Vector3<unsigned> const& size, unsigned destination_layer, unsigned destination_level, Vector3<unsigned> const& destination_offset)
{
    VERIFY(source.has_same_ownership_token(*this));

    auto const& src_image = static_cast<Image const&>(source);

    VERIFY(source_layer < src_image.num_layers());
    VERIFY(source_level < src_image.num_levels());
    VERIFY(source_offset.x() + size.x() <= src_image.level_width(source_level));
    VERIFY(source_offset.y() + size.y() <= src_image.level_height(source_level));
    VERIFY(source_offset.z() + size.z() <= src_image.level_depth(source_level));
    VERIFY(destination_layer < num_layers());
    VERIFY(destination_level < num_levels());
    VERIFY(destination_offset.x() + size.x() <= level_width(destination_level));
    VERIFY(destination_offset.y() + size.y() <= level_height(destination_level));
    VERIFY(destination_offset.z() + size.z() <= level_depth(destination_level));

    for (unsigned z = 0; z < size.z(); ++z) {
        for (unsigned y = 0; y < size.y(); ++y) {
            for (unsigned x = 0; x < size.x(); ++x) {
                auto color = src_image.texel(source_layer, source_level, source_offset.x() + x, source_offset.y() + y, source_offset.z() + z);
                set_texel(destination_layer, destination_level, destination_offset.x() + x, destination_offset.y() + y, destination_offset.z() + z, color);
            }
        }
    }
}

}
