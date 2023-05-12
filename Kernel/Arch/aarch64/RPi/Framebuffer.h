/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::RPi {

class Framebuffer {
public:
    enum class PixelOrder {
        RGB,
        BGR,
    };

    static Framebuffer& the();
    static void initialize();

    bool initialized() const { return m_initialized; }
    u16 width() const { return m_width; }
    u16 height() const { return m_height; }
    u8 depth() const { return m_depth; }
    u8* gpu_buffer() const { return m_gpu_buffer; }
    u32 buffer_size() const { return m_buffer_size; }
    u32 pitch() const { return m_pitch; }
    PixelOrder pixel_order() { return m_pixel_order; }

private:
    u16 m_width;
    u16 m_height;
    u8 m_depth;
    u8* m_gpu_buffer;
    u32 m_buffer_size;
    u32 m_pitch;
    bool m_initialized;
    PixelOrder m_pixel_order;

    Framebuffer();
};
}
