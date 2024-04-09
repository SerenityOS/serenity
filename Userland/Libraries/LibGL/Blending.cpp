/*
 * Copyright (c) 2024, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_blend_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_blend_color, red, green, blue, alpha);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_blend_color = { red, green, blue, alpha };
    m_blend_color.clamp(0.f, 1.f);

    auto options = m_rasterizer->options();
    options.blend_color = m_blend_color;
    m_rasterizer->set_options(options);
}

void GLContext::gl_blend_equation_separate(GLenum rgb_mode, GLenum alpha_mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_blend_equation_separate, rgb_mode, alpha_mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!(rgb_mode == GL_FUNC_ADD
                             || rgb_mode == GL_FUNC_SUBTRACT
                             || rgb_mode == GL_FUNC_REVERSE_SUBTRACT
                             || rgb_mode == GL_MIN
                             || rgb_mode == GL_MAX),
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(alpha_mode == GL_FUNC_ADD
                             || alpha_mode == GL_FUNC_SUBTRACT
                             || alpha_mode == GL_FUNC_REVERSE_SUBTRACT
                             || alpha_mode == GL_MIN
                             || alpha_mode == GL_MAX),
        GL_INVALID_ENUM);

    m_blend_equation_rgb = rgb_mode;
    m_blend_equation_alpha = alpha_mode;

    auto map_gl_blend_equation_to_device = [](GLenum equation) constexpr {
        switch (equation) {
        case GL_FUNC_ADD:
            return GPU::BlendEquation::Add;
        case GL_FUNC_SUBTRACT:
            return GPU::BlendEquation::Subtract;
        case GL_FUNC_REVERSE_SUBTRACT:
            return GPU::BlendEquation::ReverseSubtract;
        case GL_MIN:
            return GPU::BlendEquation::Min;
        case GL_MAX:
            return GPU::BlendEquation::Max;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto options = m_rasterizer->options();
    options.blend_equation_rgb = map_gl_blend_equation_to_device(m_blend_equation_rgb);
    options.blend_equation_alpha = map_gl_blend_equation_to_device(m_blend_equation_alpha);
    m_rasterizer->set_options(options);
}

void GLContext::gl_blend_func(GLenum src_factor, GLenum dst_factor)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_blend_func, src_factor, dst_factor);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: The list of allowed enums differs between API versions
    // This was taken from the 2.0 spec on https://docs.gl/gl2/glBlendFunc

    RETURN_WITH_ERROR_IF(!(src_factor == GL_ZERO
                             || src_factor == GL_ONE
                             || src_factor == GL_SRC_COLOR
                             || src_factor == GL_ONE_MINUS_SRC_COLOR
                             || src_factor == GL_DST_COLOR
                             || src_factor == GL_ONE_MINUS_DST_COLOR
                             || src_factor == GL_SRC_ALPHA
                             || src_factor == GL_ONE_MINUS_SRC_ALPHA
                             || src_factor == GL_DST_ALPHA
                             || src_factor == GL_ONE_MINUS_DST_ALPHA
                             || src_factor == GL_CONSTANT_COLOR
                             || src_factor == GL_ONE_MINUS_CONSTANT_COLOR
                             || src_factor == GL_CONSTANT_ALPHA
                             || src_factor == GL_ONE_MINUS_CONSTANT_ALPHA
                             || src_factor == GL_SRC_ALPHA_SATURATE),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(!(dst_factor == GL_ZERO
                             || dst_factor == GL_ONE
                             || dst_factor == GL_SRC_COLOR
                             || dst_factor == GL_ONE_MINUS_SRC_COLOR
                             || dst_factor == GL_DST_COLOR
                             || dst_factor == GL_ONE_MINUS_DST_COLOR
                             || dst_factor == GL_SRC_ALPHA
                             || dst_factor == GL_ONE_MINUS_SRC_ALPHA
                             || dst_factor == GL_DST_ALPHA
                             || dst_factor == GL_ONE_MINUS_DST_ALPHA
                             || dst_factor == GL_CONSTANT_COLOR
                             || dst_factor == GL_ONE_MINUS_CONSTANT_COLOR
                             || dst_factor == GL_CONSTANT_ALPHA
                             || dst_factor == GL_ONE_MINUS_CONSTANT_ALPHA),
        GL_INVALID_ENUM);

    m_blend_source_factor = src_factor;
    m_blend_destination_factor = dst_factor;

    auto map_gl_blend_factor_to_device = [](GLenum factor) constexpr {
        switch (factor) {
        case GL_ZERO:
            return GPU::BlendFactor::Zero;
        case GL_ONE:
            return GPU::BlendFactor::One;
        case GL_SRC_COLOR:
            return GPU::BlendFactor::SrcColor;
        case GL_ONE_MINUS_SRC_COLOR:
            return GPU::BlendFactor::OneMinusSrcColor;
        case GL_DST_COLOR:
            return GPU::BlendFactor::DstColor;
        case GL_ONE_MINUS_DST_COLOR:
            return GPU::BlendFactor::OneMinusDstColor;
        case GL_SRC_ALPHA:
            return GPU::BlendFactor::SrcAlpha;
        case GL_ONE_MINUS_SRC_ALPHA:
            return GPU::BlendFactor::OneMinusSrcAlpha;
        case GL_DST_ALPHA:
            return GPU::BlendFactor::DstAlpha;
        case GL_ONE_MINUS_DST_ALPHA:
            return GPU::BlendFactor::OneMinusDstAlpha;
        case GL_CONSTANT_COLOR:
            return GPU::BlendFactor::ConstantColor;
        case GL_ONE_MINUS_CONSTANT_COLOR:
            return GPU::BlendFactor::OneMinusConstantColor;
        case GL_CONSTANT_ALPHA:
            return GPU::BlendFactor::ConstantAlpha;
        case GL_ONE_MINUS_CONSTANT_ALPHA:
            return GPU::BlendFactor::OneMinusConstantAlpha;
        case GL_SRC_ALPHA_SATURATE:
            return GPU::BlendFactor::SrcAlphaSaturate;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto options = m_rasterizer->options();
    options.blend_source_factor = map_gl_blend_factor_to_device(m_blend_source_factor);
    options.blend_destination_factor = map_gl_blend_factor_to_device(m_blend_destination_factor);
    m_rasterizer->set_options(options);
}

}
