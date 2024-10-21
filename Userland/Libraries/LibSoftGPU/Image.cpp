/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Size.h>
#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/PixelConverter.h>

namespace SoftGPU {

Image::Image(void const* ownership_token, GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
    : GPU::Image(ownership_token, pixel_format, width, height, depth, max_levels)
    , m_mipmap_buffers(FixedArray<RefPtr<Typed3DBuffer<FloatVector4>>>::must_create_but_fixme_should_propagate_errors(number_of_levels()))
{
    VERIFY(pixel_format == GPU::PixelFormat::Alpha
        || pixel_format == GPU::PixelFormat::Intensity
        || pixel_format == GPU::PixelFormat::Luminance
        || pixel_format == GPU::PixelFormat::LuminanceAlpha
        || pixel_format == GPU::PixelFormat::RGB
        || pixel_format == GPU::PixelFormat::RGBA);

    m_width_is_power_of_two = is_power_of_two(width);
    m_height_is_power_of_two = is_power_of_two(height);
    m_depth_is_power_of_two = is_power_of_two(depth);

    for (u32 level = 0; level < number_of_levels(); ++level) {
        m_mipmap_buffers[level] = MUST(Typed3DBuffer<FloatVector4>::try_create(width_at_level(level), height_at_level(level), depth_at_level(level)));
    }
}

GPU::ImageDataLayout Image::image_data_layout(u32 level, Vector3<i32> offset) const
{
    auto const width = width_at_level(level);
    auto const height = height_at_level(level);
    auto const depth = depth_at_level(level);

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
    VERIFY(level < number_of_levels());

    auto output_layout = image_data_layout(level, output_offset);
    auto texel_data = texel_pointer(level, 0, 0, 0);

    PixelConverter converter { input_layout, output_layout };
    ErrorOr<void> conversion_result;
    switch (pixel_format()) {
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
    VERIFY(level < number_of_levels());

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

    VERIFY(source_level < src_image.number_of_levels());
    VERIFY(source_offset.x() + size.x() <= src_image.width_at_level(source_level));
    VERIFY(source_offset.y() + size.y() <= src_image.height_at_level(source_level));
    VERIFY(source_offset.z() + size.z() <= src_image.depth_at_level(source_level));
    VERIFY(destination_level < number_of_levels());
    VERIFY(destination_offset.x() + size.x() <= width_at_level(destination_level));
    VERIFY(destination_offset.y() + size.y() <= height_at_level(destination_level));
    VERIFY(destination_offset.z() + size.z() <= depth_at_level(destination_level));

    for (u32 z = 0; z < size.z(); ++z) {
        for (u32 y = 0; y < size.y(); ++y) {
            for (u32 x = 0; x < size.x(); ++x) {
                auto const& color = src_image.texel(source_level, source_offset.x() + x, source_offset.y() + y, source_offset.z() + z);
                set_texel(destination_level, destination_offset.x() + x, destination_offset.y() + y, destination_offset.z() + z, color);
            }
        }
    }
}

static GPU::ImageDataLayout image_data_layout_for_bitmap(Gfx::Bitmap& bitmap)
{
    VERIFY(bitmap.format() == Gfx::BitmapFormat::BGRA8888);
    return GPU::ImageDataLayout {
        .pixel_type = {
            .format = GPU::PixelFormat::BGRA,
            .bits = GPU::PixelComponentBits::B8_8_8_8,
            .data_type = GPU::PixelDataType::UnsignedInt,
            .components_order = GPU::ComponentsOrder::Reversed,
        },
        .dimensions = {
            .width = static_cast<u32>(bitmap.width()),
            .height = static_cast<u32>(bitmap.height()),
            .depth = 1,
        },
        .selection = {
            .width = static_cast<u32>(bitmap.width()),
            .height = static_cast<u32>(bitmap.height()),
            .depth = 1,
        },
    };
}

void Image::regenerate_mipmaps()
{
    // FIXME: currently this only works for 2D Images
    VERIFY(depth_at_level(0) == 1);

    auto empty_bitmap_for_level = [&](u32 level) -> NonnullRefPtr<Gfx::Bitmap> {
        Gfx::IntSize size = { width_at_level(level), height_at_level(level) };
        return MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size));
    };
    auto copy_image_into_bitmap = [&](u32 level) -> NonnullRefPtr<Gfx::Bitmap> {
        auto bitmap = empty_bitmap_for_level(level);

        auto input_layout = image_data_layout(level, { 0, 0, 0 });
        auto const* input_data = texel_pointer(level, 0, 0, 0);

        auto output_layout = image_data_layout_for_bitmap(bitmap);
        auto* output_data = bitmap->scanline(0);

        PixelConverter converter { input_layout, output_layout };
        MUST(converter.convert(input_data, output_data, {}));
        return bitmap;
    };
    auto copy_bitmap_into_level = [&](NonnullRefPtr<Gfx::Bitmap> bitmap, u32 level) {
        VERIFY(level >= 1);

        auto input_layout = image_data_layout_for_bitmap(bitmap);
        auto const* input_data = bitmap->scanline(0);

        auto output_layout = image_data_layout(level, { 0, 0, 0 });
        auto* output_data = texel_pointer(level, 0, 0, 0);

        PixelConverter converter { input_layout, output_layout };
        MUST(converter.convert(input_data, output_data, {}));
    };

    // For levels 1..number_of_levels-1, we generate downscaled versions of the level above
    for (u32 level = 1; level < number_of_levels(); ++level) {
        auto higher_level_bitmap = copy_image_into_bitmap(level - 1);
        auto current_level_bitmap = empty_bitmap_for_level(level);

        Gfx::Painter current_level_painter { current_level_bitmap };
        current_level_painter.draw_scaled_bitmap(
            current_level_bitmap->rect(),
            higher_level_bitmap,
            higher_level_bitmap->rect(),
            1.f,
            Gfx::ScalingMode::BilinearBlend);

        copy_bitmap_into_level(current_level_bitmap, level);
    }
}

}
