/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_bind_buffer(GLenum target, GLuint buffer)
{
    // FIXME: implement me
    dbgln_if(GL_DEBUG, "{}({:#x}, {})): unimplemented", __FUNCTION__, target, buffer);
}

void GLContext::gl_buffer_data(GLenum target, GLsizeiptr size, void const* data, GLenum usage)
{
    // FIXME: implement me
    dbgln_if(GL_DEBUG, "{}({:#x}, {}, {:p}, {:#x}): unimplemented", __FUNCTION__, target, size, data, usage);
}

void GLContext::gl_buffer_sub_data(GLenum target, GLintptr offset, GLsizeiptr size, void const* data)
{
    // FIXME: implement me
    dbgln_if(GL_DEBUG, "{}({:#x}, {}, {}, {:p}): unimplemented", __FUNCTION__, target, offset, size, data);
}

void GLContext::gl_delete_buffers(GLsizei n, GLuint const* buffers)
{
    // FIXME: implement me
    dbgln_if(GL_DEBUG, "{}({}, {:p}): unimplemented", __FUNCTION__, n, buffers);
}

void GLContext::gl_gen_buffers(GLsizei n, GLuint* buffers)
{
    // FIXME: implement me
    dbgln_if(GL_DEBUG, "{}({}, {:p}): unimplemented", __FUNCTION__, n, buffers);
}

}
