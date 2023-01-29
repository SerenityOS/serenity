/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGPU/ImageFormat.h>

namespace GL {

GPU::PixelType get_format_specification(GLenum format, GLenum type);
ErrorOr<GPU::PixelType> get_validated_pixel_type(GLenum target, GLenum internal_format, GLenum format, GLenum type);
GPU::PixelFormat pixel_format_for_internal_format(GLenum internal_format);

}
