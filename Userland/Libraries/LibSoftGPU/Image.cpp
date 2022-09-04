/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/PixelConverter.h>

namespace SoftGPU {

Image::Image(void const* ownership_token, GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
    : GPU::Image(ownership_token)
    , m_pixel_format(pixel_format)
    , m_mipmap_buffers(FixedArray<RefPtr<Typed3DBuffer<FloatVector4>>>::must_create_but_fixme_should_propagate_errors(max_levels))
{
    VERIFY(pixel_format == GPU::PixelFormat::Alpha
        || pixel_format == GPU::PixelFormat::Intensity
        || pixel_format == GPU::PixelFormat::Luminance
        || pixel_format == GPU::PixelFormat::LuminanceAlpha
        || pixel_format == GPU::PixelFormat::RGB
        || pixel_format == GPU::PixelFormat::RGBA);
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(max_levels > 0);

    m_width_is_power_of_two = is_power_of_two(width);
    m_height_is_power_of_two = is_power_of_two(height);
    m_depth_is_power_of_two = is_power_of_two(depth);

    u32 level;
    for (level = 0; level < max_levels; ++level) {
        m_mipmap_buffers[level] = MUST(Typed3DBuffer<FloatVector4>::try_create(width, height, depth));

        if (width <= 1 && height <= 1 && depth <= 1)
            break;

        width = max(width / 2, 1);
        height = max(height / 2, 1);
        depth = max(depth / 2, 1);
    }

    m_num_levels = level + 1;
}

GPU::ImageDataLayout Image::image_data_layout(u32 level, Vector3<i32> offset) const
{
    auto const width = level_width(level);
    auto const height = level_height(level);
    auto const depth = level_depth(level);

    // FIXME: we are directly writing to FloatVector4s. We should probably find a better way to do this
    return {
        .pixel_type = {
            .format = GPU::PixelFormat::RGBA,
            .bits = GPU::PixelComponentBits::AllBits,
            .data_type = GPU::PixelDataType::Float,
        },
        .dimensions = {
            .width = width,
            .height = height,
            .depth = depth,
        },
        .selection = {
            .offset_x = offset.x(),
            .offset_y = offset.y(),
            .offset_z = offset.z(),
            .width = width - offset.x(),
            .height = height - offset.y(),
            .depth = depth - offset.z(),
        },
    };
}

void Image::write_texels(u32 level, Vector3<i32> const& output_offset, void const* input_data, GPU::ImageDataLayout const& input_layout)
{
    VERIFY(level < num_levels());

    auto output_layout = image_data_layout(level, output_offset);
    auto texel_data = texel_pointer(level, 0, 0, 0);

    PixelConverter converter { input_layout, output_layout };
    ErrorOr<void> conversion_result;
    switch (m_pixel_format) {
    case GPU::PixelFormat::Luminance:
    case GPU::PixelFormat::RGB:
        // Both Luminance and RGB set the alpha to 1, regardless of the source texel
        conversion_result = converter.convert(input_data, texel_data, [](auto& components) { components[3] = 1.f; });
        break;
    default:
        conversion_result = converter.convert(input_data, texel_data, {});
    }
    if (conversion_result.is_error())
        dbgln("Pixel conversion failed: {}", conversion_result.error().string_literal());
}

void Image::read_texels(u32 level, Vector3<i32> const& input_offset, void* output_data, GPU::ImageDataLayout const& output_layout) const
{
    VERIFY(level < num_levels());

    auto input_layout = image_data_layout(level, input_offset);

    PixelConverter converter { input_layout, output_layout };
    auto conversion_result = converter.convert(texel_pointer(level, 0, 0, 0), output_data, {});
    if (conversion_result.is_error())
        dbgln("Pixel conversion failed: {}", conversion_result.error().string_literal());
}

void Image::copy_texels(GPU::Image const& source, u32 source_level, Vector3<u32> const& source_offset, Vector3<u32> const& size, u32 destination_level, Vector3<u32> const& destination_offset)
{
    VERIFY(source.has_same_ownership_token(*this));

    auto const& src_image = static_cast<Image const&>(source);

    VERIFY(source_level < src_image.num_levels());
    VERIFY(source_offset.x() + size.x() <= src_image.level_width(source_level));
    VERIFY(source_offset.y() + size.y() <= src_image.level_height(source_level));
    VERIFY(source_offset.z() + size.z() <= src_image.level_depth(source_level));
    VERIFY(destination_level < num_levels());
    VERIFY(destination_offset.x() + size.x() <= level_width(destination_level));
    VERIFY(destination_offset.y() + size.y() <= level_height(destination_level));
    VERIFY(destination_offset.z() + size.z() <= level_depth(destination_level));

    for (u32 z = 0; z < size.z(); ++z) {
        for (u32 y = 0; y < size.y(); ++y) {
            for (u32 x = 0; x < size.x(); ++x) {
                auto color = src_image.texel(source_level, source_offset.x() + x, source_offset.y() + y, source_offset.z() + z);
                set_texel(destination_level, destination_offset.x() + x, destination_offset.y() + y, destination_offset.z() + z, color);
            }
        }
    }
}

}
