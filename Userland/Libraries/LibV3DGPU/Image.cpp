/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Image.h"

#include <AK/GenericShorthands.h>
#include <LibGPU/PixelConverter.h>
#include <LibV3DGPU/Definitions.h>

namespace V3DGPU {

// FIXME: The microtile dimensions depend on the bpp.
static constexpr size_t MICROTILE_WIDTH = 4;
static constexpr size_t MICROTILE_HEIGHT = 4;

static constexpr size_t UIF_BLOCK_WIDTH = 2 * MICROTILE_WIDTH;
static constexpr size_t UIF_BLOCK_HEIGHT = 2 * MICROTILE_HEIGHT;

static constexpr size_t FOUR_BY_FOUR_WIDTH = 4 * UIF_BLOCK_WIDTH;
static constexpr size_t FOUR_BY_FOUR_HEIGHT = 4 * UIF_BLOCK_HEIGHT;

Image::Image(void const* ownership_token, GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
    : GPU::Image(ownership_token, pixel_format, width, height, depth, max_levels)
{
    VERIFY(pixel_format == GPU::PixelFormat::Alpha
        || pixel_format == GPU::PixelFormat::Intensity
        || pixel_format == GPU::PixelFormat::Luminance
        || pixel_format == GPU::PixelFormat::LuminanceAlpha
        || pixel_format == GPU::PixelFormat::RGB
        || pixel_format == GPU::PixelFormat::RGBA);
}

void Image::regenerate_mipmaps()
{
    dbgln("V3DGPU::Image::regenerate_mipmaps(): unimplemented");
}

ErrorOr<Vector<u32>> Image::convert_to_rgba(u32 level, Vector3<i32> const& output_offset, void const* input_data, GPU::ImageDataLayout const& input_layout) const
{
    auto width = width_at_level(level);
    auto height = height_at_level(level);
    auto depth = depth_at_level(level);

    GPU::ImageDataLayout output_layout = {
        .pixel_type = {
            .format = GPU::PixelFormat::RGBA,
            .bits = GPU::PixelComponentBits::AllBits,
            .data_type = GPU::PixelDataType::UnsignedByte,
        },
        .dimensions = {
            .width = width,
            .height = height,
            .depth = depth,
        },
        .selection = {
            .offset_x = output_offset.x(),
            .offset_y = output_offset.y(),
            .offset_z = output_offset.z(),
            .width = width - output_offset.x(),
            .height = height - output_offset.y(),
            .depth = depth - output_offset.z(),
        },
    };

    GPU::PixelConverter converter { input_layout, output_layout };

    Vector<u32> rgba_texture;
    rgba_texture.resize(static_cast<size_t>(width) * height * depth);

    if (first_is_one_of(pixel_format(), GPU::PixelFormat::RGB, GPU::PixelFormat::Luminance))
        TRY(converter.convert(input_data, rgba_texture.data(), [](auto& components) { components[3] = 1.f; }));
    else
        TRY(converter.convert(input_data, rgba_texture.data(), {}));

    return rgba_texture;
}

Vector<u32> Image::convert_to_tiled(Vector<u32> const& rgba_texture) const
{
    size_t width = width_at_level(0);
    size_t height = height_at_level(0);

    // FIXME: We can probably use smaller alignments for smaller textures that don't use the UIF format.
    size_t aligned_width = align_up_to(width, FOUR_BY_FOUR_WIDTH);
    size_t aligned_height = align_up_to(height, FOUR_BY_FOUR_HEIGHT);

    Vector<u32> tiled_texture;
    tiled_texture.resize(aligned_width * aligned_height);

    auto microtile = [width, height, &rgba_texture, &tiled_texture](u32 start_x, u32 start_y, u32 output_index) -> u32 {
        for (u32 y = start_y; y < start_y + MICROTILE_HEIGHT; y++) {
            for (u32 x = start_x; x < start_x + MICROTILE_WIDTH; x++) {
                if (x < width && y < height)
                    tiled_texture[output_index] = rgba_texture[y * width + x];
                output_index++;
            }
        }

        return output_index;
    };

    auto uif_block = [&microtile](u32 start_x, u32 start_y, u32 output_index) -> u32 {
        for (u32 y = start_y; y < start_y + UIF_BLOCK_HEIGHT; y += MICROTILE_HEIGHT) {
            for (u32 x = start_x; x < start_x + UIF_BLOCK_WIDTH; x += MICROTILE_WIDTH) {
                output_index = microtile(x, y, output_index);
            }
        }

        return output_index;
    };

    auto four_by_four = [height, &uif_block](u32 start_x, u32 start_y, u32 output_index) -> u32 {
        for (u32 y = start_y; y < start_y + FOUR_BY_FOUR_HEIGHT; y += UIF_BLOCK_HEIGHT) {
            if (y >= height)
                continue;

            for (u32 x = start_x; x < start_x + FOUR_BY_FOUR_WIDTH; x += UIF_BLOCK_WIDTH)
                output_index = uif_block(x, y, output_index);
        }

        return output_index;
    };

    u32 output_index = 0;

    if (width <= MICROTILE_WIDTH || height <= MICROTILE_HEIGHT) {
        // Lineartile format
        for (u32 y = 0; y < height; y += MICROTILE_HEIGHT) {
            for (u32 x = 0; x < width; x += MICROTILE_WIDTH) {
                output_index = microtile(x, y, output_index);
            }
        }
    } else if (width <= 2 * UIF_BLOCK_WIDTH) {
        // UB-linear (1/2 UIF blocks wide) format
        for (u32 y = 0; y < height; y += UIF_BLOCK_HEIGHT) {
            for (u32 x = 0; x < width; x += UIF_BLOCK_WIDTH) {
                output_index = uif_block(x, y, output_index);
            }
        }
    } else {
        // UIF (No XOR) format
        // FIXME: Support XOR format.
        for (u32 x = 0; x < width; x += FOUR_BY_FOUR_WIDTH) {
            for (u32 y = 0; y < height; y += FOUR_BY_FOUR_HEIGHT) {
                output_index = four_by_four(x, y, output_index);
            }
        }
    }

    return tiled_texture;
}

void Image::write_texels(u32 level, Vector3<i32> const& output_offset, void const* input_data, GPU::ImageDataLayout const& input_layout)
{
    // FIXME: Support mipmaps.
    if (level != 0)
        return;

    auto rgba_texture_or_error = convert_to_rgba(level, output_offset, input_data, input_layout);
    if (rgba_texture_or_error.is_error()) {
        dbgln("LibV3DGPU: Failed to convert texture to RGBA: {}", rgba_texture_or_error.error());
        return;
    }

    auto tiled_texture = convert_to_tiled(rgba_texture_or_error.release_value());

    size_t buffer_size = tiled_texture.size() * sizeof(u32);
    m_buffer = Buffer::create(align_up_to(buffer_size, V3D_PAGE_SIZE), "LibV3DGPU: Texture"sv).release_value_but_fixme_should_propagate_errors();
    u8* texture_buffer_data = m_buffer->data().data();
    memcpy(texture_buffer_data, tiled_texture.data(), buffer_size);
}

void Image::read_texels(u32, Vector3<i32> const&, void*, GPU::ImageDataLayout const&) const
{
    dbgln("V3DGPU::Image::read_texels(): unimplemented");
}

void Image::copy_texels(GPU::Image const&, u32, Vector3<u32> const&, Vector3<u32> const&, u32, Vector3<u32> const&)
{
    dbgln("V3DGPU::Image::copy_texels(): unimplemented");
}

}
