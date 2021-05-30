/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Clipper.h"
#include "GLContext.h"
#include "GLStruct.h"
#include "SoftwareRasterizer.h"
#include "Tex/NameAllocator.h"
#include "Tex/Texture.h"
#include "Tex/TextureUnit.h"
#include <AK/HashMap.h>
#include <AK/RefPtr.h>
#include <AK/Tuple.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Vector3.h>

namespace GL {

class SoftwareGLContext : public GLContext {
public:
    SoftwareGLContext(Gfx::Bitmap&);

    virtual void gl_begin(GLenum mode) override;
    virtual void gl_clear(GLbitfield mask) override;
    virtual void gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) override;
    virtual void gl_clear_depth(GLdouble depth) override;
    virtual void gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a) override;
    virtual void gl_delete_textures(GLsizei n, const GLuint* textures) override;
    virtual void gl_end() override;
    virtual void gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) override;
    virtual void gl_gen_textures(GLsizei n, GLuint* textures) override;
    virtual GLenum gl_get_error() override;
    virtual GLubyte* gl_get_string(GLenum name) override;
    virtual void gl_load_identity() override;
    virtual void gl_load_matrix(const FloatMatrix4x4& matrix) override;
    virtual void gl_matrix_mode(GLenum mode) override;
    virtual void gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) override;
    virtual void gl_push_matrix() override;
    virtual void gl_pop_matrix() override;
    virtual void gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z) override;
    virtual void gl_scale(GLdouble x, GLdouble y, GLdouble z) override;
    virtual void gl_translate(GLdouble x, GLdouble y, GLdouble z) override;
    virtual void gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w) override;
    virtual void gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height) override;
    virtual void gl_enable(GLenum) override;
    virtual void gl_disable(GLenum) override;
    virtual void gl_front_face(GLenum) override;
    virtual void gl_cull_face(GLenum) override;
    virtual GLuint gl_gen_lists(GLsizei range) override;
    virtual void gl_call_list(GLuint list) override;
    virtual void gl_delete_lists(GLuint list, GLsizei range) override;
    virtual void gl_end_list(void) override;
    virtual void gl_new_list(GLuint list, GLenum mode) override;
    virtual void gl_flush() override;
    virtual void gl_finish() override;
    virtual void gl_blend_func(GLenum src_factor, GLenum dst_factor) override;
    virtual void gl_shade_model(GLenum mode) override;
    virtual void gl_alpha_func(GLenum func, GLclampf ref) override;
    virtual void gl_hint(GLenum target, GLenum mode) override;
    virtual void gl_read_buffer(GLenum mode) override;
    virtual void gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels) override;
    virtual void gl_tex_image_2d(GLenum target, GLint level, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data) override;
    virtual void gl_tex_coord(GLfloat s, GLfloat t, GLfloat r, GLfloat q) override;
    virtual void gl_bind_texture(GLenum target, GLuint texture) override;
    virtual void gl_active_texture(GLenum texture) override;

    virtual void present() override;

private:
    template<typename T>
    T* store_in_listing(T value)
    {
        VERIFY(m_current_listing_index.has_value());
        auto& listing = m_current_listing_index->listing;
        listing.saved_arguments.empend(make<Listing::ExtraSavedArguments>(move(value)));
        return listing.saved_arguments.last()->template get_pointer<T>();
    }

    template<auto member, typename... Args>
    void append_to_listing(Args&&... args)
    {
        VERIFY(m_current_listing_index.has_value());
        m_current_listing_index->listing.entries.empend(member, Listing::ArgumentsFor<member> { forward<Args>(args)... });
    }

    [[nodiscard]] bool should_append_to_listing() const { return m_current_listing_index.has_value(); }
    [[nodiscard]] bool should_execute_after_appending_to_listing() const { return m_current_listing_index.has_value() && m_current_listing_index->mode == GL_COMPILE_AND_EXECUTE; }

    GLenum m_current_draw_mode;
    GLenum m_current_matrix_mode;
    FloatMatrix4x4 m_projection_matrix;
    FloatMatrix4x4 m_model_view_matrix;

    FloatMatrix4x4 m_current_matrix;

    Vector<FloatMatrix4x4> m_projection_matrix_stack;
    Vector<FloatMatrix4x4> m_model_view_matrix_stack;

    FloatVector4 m_clear_color = { 0.0f, 0.0f, 0.0f, 0.0f };
    double m_clear_depth = { 1.0 };
    FloatVector4 m_current_vertex_color = { 1.0f, 1.0f, 1.0f, 1.0f };

    Vector<GLVertex, 96> vertex_list;
    Vector<GLTriangle, 32> triangle_list;
    Vector<GLTriangle, 32> processed_triangles;

    GLenum m_error = GL_NO_ERROR;
    bool m_in_draw_state = false;

    bool m_depth_test_enabled = false;

    bool m_cull_faces = false;
    GLenum m_front_face = GL_CCW;
    GLenum m_culled_sides = GL_BACK;

    bool m_blend_enabled = false;
    GLenum m_blend_source_factor = GL_ONE;
    GLenum m_blend_destination_factor = GL_ZERO;

    bool m_alpha_test_enabled = false;
    GLenum m_alpha_test_func = GL_ALWAYS;
    GLclampf m_alpha_test_ref_value = 0;

    GLenum m_current_read_buffer = GL_BACK;

    NonnullRefPtr<Gfx::Bitmap> m_frontbuffer;

    Clipper m_clipper;

    // Texture objects
    TextureNameAllocator m_name_allocator;
    HashMap<GLuint, RefPtr<Texture>> m_allocated_textures;
    Array<TextureUnit, 32> m_texture_units;
    TextureUnit* m_active_texture_unit { &m_texture_units[0] };

    SoftwareRasterizer m_rasterizer;

    struct Listing {

        template<typename F>
        struct TupleTypeForArgumentListOf_;

        template<typename Ret, typename C, typename... Args>
        struct TupleTypeForArgumentListOf_<Ret (C::*)(Args...)> {
            using Type = Tuple<Args...>;
        };

        template<typename F>
        using TupleTypeForArgumentListOf = typename TupleTypeForArgumentListOf_<F>::Type;

        template<auto member>
        using ArgumentsFor = TupleTypeForArgumentListOf<decltype(member)>;

        template<typename... Fns>
        struct FunctionAndArgs {
            Variant<Fns...> function;
            Variant<TupleTypeForArgumentListOf<Fns>...> arguments;
        };

        using FunctionsAndArgs = FunctionAndArgs<
            decltype(&SoftwareGLContext::gl_begin),
            decltype(&SoftwareGLContext::gl_clear),
            decltype(&SoftwareGLContext::gl_clear_color),
            decltype(&SoftwareGLContext::gl_clear_depth),
            decltype(&SoftwareGLContext::gl_color),
            decltype(&SoftwareGLContext::gl_end),
            decltype(&SoftwareGLContext::gl_frustum),
            decltype(&SoftwareGLContext::gl_load_identity),
            decltype(&SoftwareGLContext::gl_load_matrix),
            decltype(&SoftwareGLContext::gl_matrix_mode),
            decltype(&SoftwareGLContext::gl_ortho),
            decltype(&SoftwareGLContext::gl_push_matrix),
            decltype(&SoftwareGLContext::gl_pop_matrix),
            decltype(&SoftwareGLContext::gl_rotate),
            decltype(&SoftwareGLContext::gl_scale),
            decltype(&SoftwareGLContext::gl_translate),
            decltype(&SoftwareGLContext::gl_vertex),
            decltype(&SoftwareGLContext::gl_viewport),
            decltype(&SoftwareGLContext::gl_enable),
            decltype(&SoftwareGLContext::gl_disable),
            decltype(&SoftwareGLContext::gl_front_face),
            decltype(&SoftwareGLContext::gl_cull_face),
            decltype(&SoftwareGLContext::gl_call_list),
            decltype(&SoftwareGLContext::gl_blend_func),
            decltype(&SoftwareGLContext::gl_shade_model),
            decltype(&SoftwareGLContext::gl_alpha_func),
            decltype(&SoftwareGLContext::gl_hint),
            decltype(&SoftwareGLContext::gl_read_buffer)>;

        using ExtraSavedArguments = Variant<
            FloatMatrix4x4>;

        Vector<NonnullOwnPtr<ExtraSavedArguments>> saved_arguments;
        Vector<FunctionsAndArgs> entries;
    };

    static constexpr size_t max_allowed_gl_call_depth { 128 };
    size_t m_gl_call_depth { 0 };
    Vector<Listing> m_listings;
    struct CurrentListing {
        Listing listing;
        size_t index { 0 };
        GLenum mode { GL_COMPILE };
    };
    Optional<CurrentListing> m_current_listing_index;
};

}
