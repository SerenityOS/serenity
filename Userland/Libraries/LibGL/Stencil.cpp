/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_clear_stencil(GLint s)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_stencil, s);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_stencil = static_cast<u8>(s & ((1 << m_device_info.stencil_bits) - 1));
}

void GLContext::gl_stencil_func_separate(GLenum face, GLenum func, GLint ref, GLuint mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_stencil_func_separate, face, func, ref, mask);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(!(func == GL_NEVER
                             || func == GL_LESS
                             || func == GL_LEQUAL
                             || func == GL_GREATER
                             || func == GL_GEQUAL
                             || func == GL_EQUAL
                             || func == GL_NOTEQUAL
                             || func == GL_ALWAYS),
        GL_INVALID_ENUM);

    ref = clamp(ref, 0, (1 << m_device_info.stencil_bits) - 1);

    StencilFunctionOptions new_options = { func, ref, mask };
    if (face == GL_FRONT || face == GL_FRONT_AND_BACK)
        m_stencil_function[Face::Front] = new_options;
    if (face == GL_BACK || face == GL_FRONT_AND_BACK)
        m_stencil_function[Face::Back] = new_options;

    m_stencil_configuration_dirty = true;
}

void GLContext::gl_stencil_mask_separate(GLenum face, GLuint mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_stencil_mask_separate, face, mask);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (face == GL_FRONT || face == GL_FRONT_AND_BACK)
        m_stencil_operation[Face::Front].write_mask = mask;
    if (face == GL_BACK || face == GL_FRONT_AND_BACK)
        m_stencil_operation[Face::Back].write_mask = mask;

    m_stencil_configuration_dirty = true;
}

void GLContext::gl_stencil_op_separate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_stencil_op_separate, face, sfail, dpfail, dppass);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);

    auto is_valid_op = [](GLenum op) -> bool {
        return op == GL_KEEP || op == GL_ZERO || op == GL_REPLACE || op == GL_INCR || op == GL_INCR_WRAP
            || op == GL_DECR || op == GL_DECR_WRAP || op == GL_INVERT;
    };
    RETURN_WITH_ERROR_IF(!is_valid_op(sfail), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!is_valid_op(dpfail), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!is_valid_op(dppass), GL_INVALID_ENUM);

    auto update_stencil_operation = [&](Face face, GLenum sfail, GLenum dpfail, GLenum dppass) {
        auto& stencil_operation = m_stencil_operation[face];
        stencil_operation.op_fail = sfail;
        stencil_operation.op_depth_fail = dpfail;
        stencil_operation.op_pass = dppass;
    };
    if (face == GL_FRONT || face == GL_FRONT_AND_BACK)
        update_stencil_operation(Face::Front, sfail, dpfail, dppass);
    if (face == GL_BACK || face == GL_FRONT_AND_BACK)
        update_stencil_operation(Face::Back, sfail, dpfail, dppass);

    m_stencil_configuration_dirty = true;
}

void GLContext::sync_stencil_configuration()
{
    if (!m_stencil_configuration_dirty)
        return;
    m_stencil_configuration_dirty = false;

    auto set_device_stencil = [&](GPU::Face face, StencilFunctionOptions func, StencilOperationOptions op) {
        GPU::StencilConfiguration device_configuration;

        // Stencil test function
        auto map_func = [](GLenum func) -> GPU::StencilTestFunction {
            switch (func) {
            case GL_ALWAYS:
                return GPU::StencilTestFunction::Always;
            case GL_EQUAL:
                return GPU::StencilTestFunction::Equal;
            case GL_GEQUAL:
                return GPU::StencilTestFunction::GreaterOrEqual;
            case GL_GREATER:
                return GPU::StencilTestFunction::Greater;
            case GL_LESS:
                return GPU::StencilTestFunction::Less;
            case GL_LEQUAL:
                return GPU::StencilTestFunction::LessOrEqual;
            case GL_NEVER:
                return GPU::StencilTestFunction::Never;
            case GL_NOTEQUAL:
                return GPU::StencilTestFunction::NotEqual;
            }
            VERIFY_NOT_REACHED();
        };
        device_configuration.test_function = map_func(func.func);
        device_configuration.reference_value = func.reference_value;
        device_configuration.test_mask = func.mask;

        // Stencil operation
        auto map_operation = [](GLenum operation) -> GPU::StencilOperation {
            switch (operation) {
            case GL_DECR:
                return GPU::StencilOperation::Decrement;
            case GL_DECR_WRAP:
                return GPU::StencilOperation::DecrementWrap;
            case GL_INCR:
                return GPU::StencilOperation::Increment;
            case GL_INCR_WRAP:
                return GPU::StencilOperation::IncrementWrap;
            case GL_INVERT:
                return GPU::StencilOperation::Invert;
            case GL_KEEP:
                return GPU::StencilOperation::Keep;
            case GL_REPLACE:
                return GPU::StencilOperation::Replace;
            case GL_ZERO:
                return GPU::StencilOperation::Zero;
            }
            VERIFY_NOT_REACHED();
        };
        device_configuration.on_stencil_test_fail = map_operation(op.op_fail);
        device_configuration.on_depth_test_fail = map_operation(op.op_depth_fail);
        device_configuration.on_pass = map_operation(op.op_pass);
        device_configuration.write_mask = op.write_mask;

        m_rasterizer->set_stencil_configuration(face, device_configuration);
    };
    set_device_stencil(GPU::Face::Front, m_stencil_function[Face::Front], m_stencil_operation[Face::Front]);
    set_device_stencil(GPU::Face::Back, m_stencil_function[Face::Back], m_stencil_operation[Face::Back]);
}

}
