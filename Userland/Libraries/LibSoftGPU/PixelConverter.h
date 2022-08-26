/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGfx/Vector4.h>

namespace SoftGPU {

class PixelConverter {
public:
    PixelConverter(GPU::ImageDataLayout input_specification, GPU::ImageDataLayout output_specification)
        : m_input_specification { input_specification }
        , m_output_specification { output_specification }
    {
    }

    ErrorOr<void> convert(void const* input_data, void* output_data, Function<void(FloatVector4&)> transform);

private:
    FloatVector4 read_pixel(u8 const**);
    void write_pixel(u8**, FloatVector4 const&);

    GPU::ImageDataLayout m_input_specification;
    GPU::ImageDataLayout m_output_specification;
};

}
