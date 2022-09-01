/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021-2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2021-2022, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Debug.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>

extern GL::GLContext* g_gl_context;

// Transposes input matrices (column-major) to our Matrix (row-major).
template<typename I, typename O>
static constexpr Matrix4x4<O> transpose_input_matrix(I const* matrix)
{
    if constexpr (IsSame<I, O>) {
        // clang-format off
        return {
            matrix[0], matrix[4], matrix[8], matrix[12],
            matrix[1], matrix[5], matrix[9], matrix[13],
            matrix[2], matrix[6], matrix[10], matrix[14],
            matrix[3], matrix[7], matrix[11], matrix[15],
        };
        // clang-format on
    }

    Array<O, 16> elements;
    for (size_t i = 0; i < 16; ++i)
        elements[i] = static_cast<O>(matrix[i]);
    // clang-format off
    return {
        elements[0], elements[4], elements[8], elements[12],
        elements[1], elements[5], elements[9], elements[13],
        elements[2], elements[6], elements[10], elements[14],
        elements[3], elements[7], elements[11], elements[15],
    };
    // clang-format on
}

void glActiveTexture(GLenum texture)
{
    g_gl_context->gl_active_texture(texture);
}

void glActiveTextureARB(GLenum texture)
{
    glActiveTexture(texture);
}

void glAlphaFunc(GLenum func, GLclampf ref)
{
    return g_gl_context->gl_alpha_func(func, ref);
}

void glArrayElement(GLint i)
{
    g_gl_context->gl_array_element(i);
}

void glBegin(GLenum mode)
{
    g_gl_context->gl_begin(mode);
}

void glBindTexture(GLenum target, GLuint texture)
{
    g_gl_context->gl_bind_texture(target, texture);
}

void glBitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap)
{
    g_gl_context->gl_bitmap(width, height, xorig, yorig, xmove, ymove, bitmap);
}

void glBlendFunc(GLenum sfactor, GLenum dfactor)
{
    return g_gl_context->gl_blend_func(sfactor, dfactor);
}

void glCallList(GLuint list)
{
    return g_gl_context->gl_call_list(list);
}

void glCallLists(GLsizei n, GLenum type, void const* lists)
{
    return g_gl_context->gl_call_lists(n, type, lists);
}

void glClear(GLbitfield mask)
{
    g_gl_context->gl_clear(mask);
}

void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    g_gl_context->gl_clear_color(red, green, blue, alpha);
}

void glClearDepth(GLdouble depth)
{
    g_gl_context->gl_clear_depth(depth);
}

void glClearDepthf(GLfloat depth)
{
    g_gl_context->gl_clear_depth(static_cast<double>(depth));
}

void glClearStencil(GLint s)
{
    g_gl_context->gl_clear_stencil(s);
}

void glClientActiveTexture(GLenum target)
{
    g_gl_context->gl_client_active_texture(target);
}

void glClientActiveTextureARB(GLenum target)
{
    glClientActiveTexture(target);
}

void glClipPlane(GLenum plane, GLdouble const* equation)
{
    g_gl_context->gl_clip_plane(plane, equation);
}

void glColor3d(GLdouble r, GLdouble g, GLdouble b)
{
    g_gl_context->gl_color(r, g, b, 1.0);
}

void glColor3dv(GLdouble const* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], 1.0);
}

void glColor3f(GLfloat r, GLfloat g, GLfloat b)
{
    g_gl_context->gl_color(r, g, b, 1.0);
}

void glColor3fv(GLfloat const* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], 1.0);
}

void glColor3ub(GLubyte r, GLubyte g, GLubyte b)
{
    g_gl_context->gl_color(r / 255.0, g / 255.0, b / 255.0, 1.0);
}

void glColor3ubv(GLubyte const* v)
{
    g_gl_context->gl_color(v[0] / 255.0, v[1] / 255.0, v[2] / 255.0, 1.0);
}

void glColor4b(GLbyte r, GLbyte g, GLbyte b, GLbyte a)
{
    g_gl_context->gl_color(
        (static_cast<double>(r) + 128) / 127.5 - 1,
        (static_cast<double>(g) + 128) / 127.5 - 1,
        (static_cast<double>(b) + 128) / 127.5 - 1,
        (static_cast<double>(a) + 128) / 127.5 - 1);
}

void glColor4dv(GLdouble const* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], v[3]);
}

void glColor4f(GLfloat r, GLfloat g, GLfloat b, GLfloat a)
{
    g_gl_context->gl_color(r, g, b, a);
}

void glColor4fv(GLfloat const* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], v[3]);
}

void glColor4ub(GLubyte r, GLubyte g, GLubyte b, GLubyte a)
{
    g_gl_context->gl_color(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
}

void glColor4ubv(GLubyte const* v)
{
    g_gl_context->gl_color(v[0] / 255.0, v[1] / 255.0, v[2] / 255.0, v[3] / 255.0);
}

void glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    g_gl_context->gl_color_mask(red, green, blue, alpha);
}

void glColorMaterial(GLenum face, GLenum mode)
{
    g_gl_context->gl_color_material(face, mode);
}

void glColorPointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    g_gl_context->gl_color_pointer(size, type, stride, pointer);
}

void glCopyTexImage2D(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border)
{
    g_gl_context->gl_copy_tex_image_2d(target, level, internalformat, x, y, width, height, border);
}

void glCopyTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height)
{
    g_gl_context->gl_copy_tex_sub_image_2d(target, level, xoffset, yoffset, x, y, width, height);
}

void glCullFace(GLenum mode)
{
    g_gl_context->gl_cull_face(mode);
}

void glDepthFunc(GLenum func)
{
    g_gl_context->gl_depth_func(func);
}

void glDepthMask(GLboolean flag)
{
    g_gl_context->gl_depth_mask(flag);
}

void glDepthRange(GLdouble min, GLdouble max)
{
    g_gl_context->gl_depth_range(min, max);
}

void glDeleteLists(GLuint list, GLsizei range)
{
    return g_gl_context->gl_delete_lists(list, range);
}

void glDeleteTextures(GLsizei n, GLuint const* textures)
{
    g_gl_context->gl_delete_textures(n, textures);
}

void glDisable(GLenum cap)
{
    g_gl_context->gl_disable(cap);
}

void glDisableClientState(GLenum cap)
{
    g_gl_context->gl_disable_client_state(cap);
}

void glDrawArrays(GLenum mode, GLint first, GLsizei count)
{
    g_gl_context->gl_draw_arrays(mode, first, count);
}

void glDrawBuffer(GLenum buffer)
{
    g_gl_context->gl_draw_buffer(buffer);
}

void glDrawElements(GLenum mode, GLsizei count, GLenum type, void const* indices)
{
    g_gl_context->gl_draw_elements(mode, count, type, indices);
}

void glDrawPixels(GLsizei width, GLsizei height, GLenum format, GLenum type, void const* data)
{
    g_gl_context->gl_draw_pixels(width, height, format, type, data);
}

void glEnable(GLenum cap)
{
    g_gl_context->gl_enable(cap);
}

void glEnableClientState(GLenum cap)
{
    g_gl_context->gl_enable_client_state(cap);
}

void glEnd()
{
    g_gl_context->gl_end();
}

void glEndList(void)
{
    return g_gl_context->gl_end_list();
}

void glEvalCoord1d(GLdouble u)
{
    dbgln("glEvalCoord1d({}): unimplemented", u);
    TODO();
}

void glEvalCoord1f(GLfloat u)
{
    dbgln("glEvalCoord1f({}): unimplemented", u);
    TODO();
}

void glEvalCoord2d(GLdouble u, GLdouble v)
{
    dbgln("glEvalCoord2d({}, {}): unimplemented", u, v);
    TODO();
}

void glEvalCoord2f(GLfloat u, GLfloat v)
{
    dbgln("glEvalCoord2f({}, {}): unimplemented", u, v);
    TODO();
}

void glEvalMesh1(GLenum mode, GLint i1, GLint i2)
{
    dbgln("glEvalMesh1({:#x}, {}, {}): unimplemented", mode, i1, i2);
    TODO();
}

void glEvalMesh2(GLenum mode, GLint i1, GLint i2, GLint j1, GLint j2)
{
    dbgln("glEvalMesh2({:#x}, {}, {}, {}, {}): unimplemented", mode, i1, i2, j1, j2);
    TODO();
}

void glEvalPoint1(GLint i)
{
    dbgln("glEvalPoint1({}): unimplemented", i);
    TODO();
}

void glEvalPoint2(GLint i, GLint j)
{
    dbgln("glEvalPoint2({}, {}): unimplemented", i, j);
    TODO();
}

void glFinish()
{
    g_gl_context->gl_finish();
}

void glFogfv(GLenum pname, GLfloat const* params)
{
    g_gl_context->gl_fogfv(pname, params);
}

void glFogf(GLenum pname, GLfloat param)
{
    g_gl_context->gl_fogf(pname, param);
}

void glFogi(GLenum pname, GLint param)
{
    g_gl_context->gl_fogi(pname, param);
}

void glFlush()
{
    g_gl_context->gl_flush();
}

void glFrontFace(GLenum mode)
{
    g_gl_context->gl_front_face(mode);
}

void glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal)
{
    g_gl_context->gl_frustum(left, right, bottom, top, nearVal, farVal);
}

GLuint glGenLists(GLsizei range)
{
    return g_gl_context->gl_gen_lists(range);
}

void glGenTextures(GLsizei n, GLuint* textures)
{
    g_gl_context->gl_gen_textures(n, textures);
}

void glGetBooleanv(GLenum pname, GLboolean* data)
{
    g_gl_context->gl_get_booleanv(pname, data);
}

void glGetClipPlane(GLenum plane, GLdouble* equation)
{
    g_gl_context->gl_get_clip_plane(plane, equation);
}

void glGetDoublev(GLenum pname, GLdouble* params)
{
    g_gl_context->gl_get_doublev(pname, params);
}

GLenum glGetError()
{
    return g_gl_context->gl_get_error();
}

void glGetFloatv(GLenum pname, GLfloat* params)
{
    g_gl_context->gl_get_floatv(pname, params);
}

void glGetIntegerv(GLenum pname, GLint* data)
{
    g_gl_context->gl_get_integerv(pname, data);
}

void glGetLightfv(GLenum light, GLenum pname, GLfloat* params)
{
    g_gl_context->gl_get_light(light, pname, params, GL_FLOAT);
}

void glGetLightiv(GLenum light, GLenum pname, GLint* params)
{
    g_gl_context->gl_get_light(light, pname, params, GL_INT);
}

void glGetMaterialfv(GLenum face, GLenum pname, GLfloat* params)
{
    g_gl_context->gl_get_material(face, pname, params, GL_FLOAT);
}

void glGetMaterialiv(GLenum face, GLenum pname, GLint* params)
{
    g_gl_context->gl_get_material(face, pname, params, GL_INT);
}

GLubyte const* glGetString(GLenum name)
{
    return g_gl_context->gl_get_string(name);
}

void glGetTexLevelParameteriv(GLenum target, GLint level, GLenum pname, GLint* params)
{
    g_gl_context->gl_get_tex_parameter_integerv(target, level, pname, params);
}

void glHint(GLenum target, GLenum mode)
{
    g_gl_context->gl_hint(target, mode);
}

GLboolean glIsEnabled(GLenum cap)
{
    return g_gl_context->gl_is_enabled(cap);
}

GLboolean glIsList(GLuint list)
{
    return g_gl_context->gl_is_list(list);
}

GLboolean glIsTexture(GLuint texture)
{
    return g_gl_context->gl_is_texture(texture);
}

void glLightf(GLenum light, GLenum pname, GLfloat param)
{
    g_gl_context->gl_lightf(light, pname, param);
}

void glLightfv(GLenum light, GLenum pname, GLfloat const* param)
{
    g_gl_context->gl_lightfv(light, pname, param);
}

void glLighti(GLenum light, GLenum pname, GLint param)
{
    g_gl_context->gl_lightf(light, pname, param);
}

void glLightiv(GLenum light, GLenum pname, GLint const* params)
{
    g_gl_context->gl_lightiv(light, pname, params);
}

void glLightModelf(GLenum pname, GLfloat param)
{
    g_gl_context->gl_light_model(pname, param, 0.0f, 0.0f, 0.0f);
}

void glLightModelfv(GLenum pname, GLfloat const* params)
{
    switch (pname) {
    case GL_LIGHT_MODEL_AMBIENT:
        g_gl_context->gl_light_model(pname, params[0], params[1], params[2], params[3]);
        break;
    default:
        g_gl_context->gl_light_model(pname, params[0], 0.0f, 0.0f, 0.0f);
        break;
    }
}

void glLightModeliv(GLenum pname, GLint const* params)
{
    switch (pname) {
    case GL_LIGHT_MODEL_AMBIENT:
        g_gl_context->gl_light_model(pname, params[0], params[1], params[2], params[3]);
        break;
    default:
        g_gl_context->gl_light_model(pname, params[0], 0.0f, 0.0f, 0.0f);
        break;
    }
}

void glLightModeli(GLenum pname, GLint param)
{
    g_gl_context->gl_light_model(pname, param, 0.0f, 0.0f, 0.0f);
}

void glLineWidth(GLfloat width)
{
    g_gl_context->gl_line_width(width);
}

void glListBase(GLuint base)
{
    return g_gl_context->gl_list_base(base);
}

void glLoadIdentity()
{
    g_gl_context->gl_load_identity();
}

void glLoadMatrixd(GLdouble const* matrix)
{
    g_gl_context->gl_load_matrix(transpose_input_matrix<double, float>(matrix));
}

void glLoadMatrixf(GLfloat const* matrix)
{
    g_gl_context->gl_load_matrix(transpose_input_matrix<float, float>(matrix));
}

void glMap1d(GLenum target, GLdouble u1, GLdouble u2, GLint stride, GLint order, GLdouble const* points)
{
    dbgln("glMap1d({:#x}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, stride, order, points);
    TODO();
}

void glMap1f(GLenum target, GLfloat u1, GLfloat u2, GLint stride, GLint order, GLfloat const* points)
{
    dbgln("glMap1f({:#x}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, stride, order, points);
    TODO();
}

void glMap2d(GLenum target, GLdouble u1, GLdouble u2, GLint ustride, GLint uorder, GLdouble v1, GLdouble v2, GLint vstride, GLint vorder, GLdouble const* points)
{
    dbgln("glMap2d({:#x}, {}, {}, {}, {}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
    TODO();
}

void glMap2f(GLenum target, GLfloat u1, GLfloat u2, GLint ustride, GLint uorder, GLfloat v1, GLfloat v2, GLint vstride, GLint vorder, GLfloat const* points)
{
    dbgln("glMap2f({:#x}, {}, {}, {}, {}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
    TODO();
}

void glMapGrid1d(GLint un, GLdouble u1, GLdouble u2)
{
    dbgln("glMapGrid1d({}, {}, {}): unimplemented", un, u1, u2);
    TODO();
}

void glMapGrid1f(GLint un, GLfloat u1, GLfloat u2)
{
    dbgln("glMapGrid1f({}, {}, {}): unimplemented", un, u1, u2);
    TODO();
}

void glMapGrid2d(GLint un, GLdouble u1, GLdouble u2, GLint vn, GLdouble v1, GLdouble v2)
{
    dbgln("glMapGrid2d({}, {}, {}, {}, {}, {}): unimplemented", un, u1, u2, vn, v1, v2);
    TODO();
}

void glMapGrid2f(GLint un, GLfloat u1, GLfloat u2, GLint vn, GLfloat v1, GLfloat v2)
{
    dbgln("glMapGrid2f({}, {}, {}, {}, {}, {}): unimplemented", un, u1, u2, vn, v1, v2);
    TODO();
}

void glMaterialf(GLenum face, GLenum pname, GLfloat param)
{
    g_gl_context->gl_materialf(face, pname, param);
}

void glMaterialfv(GLenum face, GLenum pname, GLfloat const* params)
{
    g_gl_context->gl_materialfv(face, pname, params);
}

void glMateriali(GLenum face, GLenum pname, GLint param)
{
    g_gl_context->gl_materialf(face, pname, param);
}

void glMaterialiv(GLenum face, GLenum pname, GLint const* params)
{
    g_gl_context->gl_materialiv(face, pname, params);
}

void glMatrixMode(GLenum mode)
{
    g_gl_context->gl_matrix_mode(mode);
}

void glMultiTexCoord2f(GLenum target, GLfloat s, GLfloat t)
{
    g_gl_context->gl_multi_tex_coord(target, s, t, 0.0f, 1.0f);
}

void glMultiTexCoord2fARB(GLenum target, GLfloat s, GLfloat t)
{
    glMultiTexCoord2f(target, s, t);
}

void glMultMatrixd(GLdouble const* matrix)
{
    g_gl_context->gl_mult_matrix(transpose_input_matrix<double, float>(matrix));
}

void glMultMatrixf(GLfloat const* matrix)
{
    g_gl_context->gl_mult_matrix(transpose_input_matrix<float, float>(matrix));
}

void glNewList(GLuint list, GLenum mode)
{
    return g_gl_context->gl_new_list(list, mode);
}

void glNormal3f(GLfloat nx, GLfloat ny, GLfloat nz)
{
    g_gl_context->gl_normal(nx, ny, nz);
}

void glNormal3fv(GLfloat const* v)
{
    g_gl_context->gl_normal(v[0], v[1], v[2]);
}

void glNormalPointer(GLenum type, GLsizei stride, void const* pointer)
{
    g_gl_context->gl_normal_pointer(type, stride, pointer);
}

void glOrtho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal)
{
    g_gl_context->gl_ortho(left, right, bottom, top, nearVal, farVal);
}

void glPixelStorei(GLenum pname, GLint param)
{
    g_gl_context->gl_pixel_storei(pname, param);
}

void glPointSize(GLfloat size)
{
    g_gl_context->gl_point_size(size);
}

void glPolygonMode(GLenum face, GLenum mode)
{
    g_gl_context->gl_polygon_mode(face, mode);
}

void glPolygonOffset(GLfloat factor, GLfloat units)
{
    g_gl_context->gl_polygon_offset(factor, units);
}

void glPopAttrib()
{
    g_gl_context->gl_pop_attrib();
}

void glPopMatrix()
{
    g_gl_context->gl_pop_matrix();
}

void glPushAttrib(GLbitfield mask)
{
    g_gl_context->gl_push_attrib(mask);
}

void glPushMatrix()
{
    g_gl_context->gl_push_matrix();
}

void glRasterPos2i(GLint x, GLint y)
{
    g_gl_context->gl_raster_pos(static_cast<float>(x), static_cast<float>(y), 0.0f, 1.0f);
}

void glReadBuffer(GLenum mode)
{
    g_gl_context->gl_read_buffer(mode);
}

void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    g_gl_context->gl_read_pixels(x, y, width, height, format, type, pixels);
}

void glRectf(GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2)
{
    g_gl_context->gl_rect(x1, y1, x2, y2);
}

void glRecti(GLint x1, GLint y1, GLint x2, GLint y2)
{
    g_gl_context->gl_rect(x1, y1, x2, y2);
}

void glRotated(GLdouble angle, GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_rotate(angle, x, y, z);
}

void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_rotate(angle, x, y, z);
}

void glScaled(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_scale(x, y, z);
}

void glScalef(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_scale(x, y, z);
}

void glScissor(GLint x, GLint y, GLsizei width, GLsizei height)
{
    g_gl_context->gl_scissor(x, y, width, height);
}

void glShadeModel(GLenum mode)
{
    g_gl_context->gl_shade_model(mode);
}

void glStencilFunc(GLenum func, GLint ref, GLuint mask)
{
    g_gl_context->gl_stencil_func_separate(GL_FRONT_AND_BACK, func, ref, mask);
}

void glStencilFuncSeparate(GLenum face, GLenum func, GLint ref, GLuint mask)
{
    g_gl_context->gl_stencil_func_separate(face, func, ref, mask);
}

void glStencilMask(GLuint mask)
{
    g_gl_context->gl_stencil_mask_separate(GL_FRONT_AND_BACK, mask);
}

void glStencilMaskSeparate(GLenum face, GLuint mask)
{
    g_gl_context->gl_stencil_mask_separate(face, mask);
}

void glStencilOp(GLenum sfail, GLenum dpfail, GLenum dppass)
{
    g_gl_context->gl_stencil_op_separate(GL_FRONT_AND_BACK, sfail, dpfail, dppass);
}

void glStencilOpSeparate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass)
{
    g_gl_context->gl_stencil_op_separate(face, sfail, dpfail, dppass);
}

void glTexCoord1f(GLfloat s)
{
    g_gl_context->gl_tex_coord(s, 0.0f, 0.0f, 1.0f);
}

void glTexCoord1fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], 0.0f, 0.0f, 1.0f);
}

void glTexCoord2d(GLdouble s, GLdouble t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 1.0f);
}

void glTexCoord2dv(GLdouble const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], 0.0f, 1.0f);
}

void glTexCoord2f(GLfloat s, GLfloat t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 1.0f);
}

void glTexCoord2fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], 0.0f, 1.0f);
}

void glTexCoord2i(GLint s, GLint t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 1.0f);
}

void glTexCoord3f(GLfloat s, GLfloat t, GLfloat r)
{
    g_gl_context->gl_tex_coord(s, t, r, 1.0f);
}

void glTexCoord3fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], v[2], 1.0f);
}

void glTexCoord4f(GLfloat s, GLfloat t, GLfloat r, GLfloat q)
{
    g_gl_context->gl_tex_coord(s, t, r, q);
}

void glTexCoord4fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], v[2], v[3]);
}

void glTexCoordPointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    g_gl_context->gl_tex_coord_pointer(size, type, stride, pointer);
}

void glTexEnvf(GLenum target, GLenum pname, GLfloat param)
{
    g_gl_context->gl_tex_env(target, pname, param);
}

void glTexEnvi(GLenum target, GLenum pname, GLint param)
{
    g_gl_context->gl_tex_env(target, pname, param);
}

void glTexGend(GLenum coord, GLenum pname, GLdouble param)
{
    g_gl_context->gl_tex_gen(coord, pname, param);
}

void glTexGenf(GLenum coord, GLenum pname, GLfloat param)
{
    g_gl_context->gl_tex_gen(coord, pname, param);
}

void glTexGenfv(GLenum coord, GLenum pname, GLfloat const* params)
{
    g_gl_context->gl_tex_gen_floatv(coord, pname, params);
}

void glTexGeni(GLenum coord, GLenum pname, GLint param)
{
    g_gl_context->gl_tex_gen(coord, pname, param);
}

void glTexImage1D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLint border, GLenum format, GLenum type, GLvoid const* data)
{
    dbgln("glTexImage1D({:#x}, {}, {:#x}, {}, {}, {:#x}, {:#x}, {:p}): unimplemented", target, level, internalFormat, width, border, format, type, data);
    TODO();
}

void glTexImage2D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, GLvoid const* data)
{
    g_gl_context->gl_tex_image_2d(target, level, internalFormat, width, height, border, format, type, data);
}

void glTexImage3D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, GLvoid const* data)
{
    dbgln("glTexImage3D({:#x}, {}, {:#x}, {}, {}, {}, {}, {:#x}, {:#x}, {:p}): unimplemented", target, level, internalFormat, width, height, depth, border, format, type, data);
    TODO();
}

void glTexParameteri(GLenum target, GLenum pname, GLint param)
{
    g_gl_context->gl_tex_parameter(target, pname, param);
}

void glTexParameterf(GLenum target, GLenum pname, GLfloat param)
{
    g_gl_context->gl_tex_parameter(target, pname, param);
}

void glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid const* data)
{
    g_gl_context->gl_tex_sub_image_2d(target, level, xoffset, yoffset, width, height, format, type, data);
}

void glTranslated(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_translate(x, y, z);
}

void glTranslatef(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_translate(x, y, z);
}

void glVertex2d(GLdouble x, GLdouble y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2dv(GLdouble const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2f(GLfloat x, GLfloat y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2fv(GLfloat const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2i(GLint x, GLint y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2iv(GLint const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2s(GLshort x, GLshort y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2sv(GLshort const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex3d(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3dv(GLdouble const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3f(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3fv(GLfloat const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3i(GLint x, GLint y, GLint z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3iv(GLint const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3s(GLshort x, GLshort y, GLshort z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3sv(GLshort const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex4d(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4dv(GLdouble const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4f(GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4fv(GLfloat const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4i(GLint x, GLint y, GLint z, GLint w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4iv(GLint const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4s(GLshort x, GLshort y, GLshort z, GLshort w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4sv(GLshort const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertexPointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    g_gl_context->gl_vertex_pointer(size, type, stride, pointer);
}

void glViewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    g_gl_context->gl_viewport(x, y, width, height);
}
