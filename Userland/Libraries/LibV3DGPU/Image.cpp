/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Image.h"

namespace V3DGPU {

Image::Image(void const* ownership_token, GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
    : GPU::Image(ownership_token, pixel_format, width, height, depth, max_levels)
{
    dbgln("V3DGPU::Image::Image(): unimplemented");
}

void Image::regenerate_mipmaps()
{
    dbgln("V3DGPU::Image::regenerate_mipmaps(): unimplemented");
}

void Image::write_texels(u32, Vector3<i32> const&, void const*, GPU::ImageDataLayout const&)
{
    dbgln("V3DGPU::Image::write_texels(): unimplemented");
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
