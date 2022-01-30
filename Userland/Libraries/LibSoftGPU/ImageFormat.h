/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector4.h>

namespace SoftGPU {

enum class ImageFormat {
    RGB565,
    RGB888,
    BGR888,
    RGBA8888,
    BGRA8888,
    L8,
    L8A8,
};

static constexpr size_t element_size(ImageFormat format)
{
    switch (format) {
    case ImageFormat::L8:
        return 1;
    case ImageFormat::RGB565:
    case ImageFormat::L8A8:
        return 2;
    case ImageFormat::RGB888:
    case ImageFormat::BGR888:
        return 3;
    case ImageFormat::RGBA8888:
    case ImageFormat::BGRA8888:
        return 4;
    default:
        VERIFY_NOT_REACHED();
    }
}

inline static FloatVector4 unpack_color(void const* ptr, ImageFormat format)
{
    constexpr auto one_over_255 = 1.0f / 255;
    switch (format) {
    case ImageFormat::RGB888: {
        auto rgb = reinterpret_cast<u8 const*>(ptr);
        return {
            rgb[0] * one_over_255,
            rgb[1] * one_over_255,
            rgb[2] * one_over_255,
            1.0f,
        };
    }
    case ImageFormat::BGR888: {
        auto bgr = reinterpret_cast<u8 const*>(ptr);
        return {
            bgr[2] * one_over_255,
            bgr[1] * one_over_255,
            bgr[0] * one_over_255,
            1.0f,
        };
    }
    case ImageFormat::RGBA8888: {
        auto rgba = *reinterpret_cast<u32 const*>(ptr);
        return {
            (rgba & 0xff) * one_over_255,
            ((rgba >> 8) & 0xff) * one_over_255,
            ((rgba >> 16) & 0xff) * one_over_255,
            ((rgba >> 24) & 0xff) * one_over_255,
        };
    }
    case ImageFormat::BGRA8888: {
        auto bgra = *reinterpret_cast<u32 const*>(ptr);
        return {
            ((bgra >> 16) & 0xff) * one_over_255,
            ((bgra >> 8) & 0xff) * one_over_255,
            (bgra & 0xff) * one_over_255,
            ((bgra >> 24) & 0xff) * one_over_255,
        };
    }
    case ImageFormat::RGB565: {
        auto rgb = *reinterpret_cast<u16 const*>(ptr);
        return {
            ((rgb >> 11) & 0x1f) / 31.f,
            ((rgb >> 5) & 0x3f) / 63.f,
            (rgb & 0x1f) / 31.f,
            1.0f
        };
    }
    case ImageFormat::L8: {
        auto luminance = *reinterpret_cast<u8 const*>(ptr);
        auto clamped_luminance = luminance * one_over_255;
        return {
            clamped_luminance,
            clamped_luminance,
            clamped_luminance,
            1.0f,
        };
    }
    case ImageFormat::L8A8: {
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

inline static void pack_color(FloatVector4 const& color, void* ptr, ImageFormat format)
{
    auto r = static_cast<u8>(clamp(color.x(), 0.0f, 1.0f) * 255);
    auto g = static_cast<u8>(clamp(color.y(), 0.0f, 1.0f) * 255);
    auto b = static_cast<u8>(clamp(color.z(), 0.0f, 1.0f) * 255);
    auto a = static_cast<u8>(clamp(color.w(), 0.0f, 1.0f) * 255);

    switch (format) {
    case ImageFormat::RGB888:
        reinterpret_cast<u8*>(ptr)[0] = r;
        reinterpret_cast<u8*>(ptr)[1] = g;
        reinterpret_cast<u8*>(ptr)[2] = b;
        return;
    case ImageFormat::BGR888:
        reinterpret_cast<u8*>(ptr)[2] = b;
        reinterpret_cast<u8*>(ptr)[1] = g;
        reinterpret_cast<u8*>(ptr)[0] = r;
        return;
    case ImageFormat::RGBA8888:
        *reinterpret_cast<u32*>(ptr) = r | (g << 8) | (b << 16) | (a << 24);
        return;
    case ImageFormat::BGRA8888:
        *reinterpret_cast<u32*>(ptr) = b | (g << 8) | (r << 16) | (a << 24);
        return;
    case ImageFormat::RGB565:
        *reinterpret_cast<u16*>(ptr) = (r & 0x1f) | ((g & 0x3f) << 5) | ((b & 0x1f) << 11);
        return;
    case ImageFormat::L8:
        *reinterpret_cast<u8*>(ptr) = r;
        return;
    case ImageFormat::L8A8:
        reinterpret_cast<u8*>(ptr)[0] = r;
        reinterpret_cast<u8*>(ptr)[1] = a;
        return;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
