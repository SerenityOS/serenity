/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021-2022, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/Tuple.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGL/Tex/NameAllocator.h>
#include <LibGL/Tex/Texture.h>
#include <LibGL/Tex/TextureUnit.h>
#include <LibGPU/Device.h>
#include <LibGPU/DeviceInfo.h>
#include <LibGPU/Driver.h>
#include <LibGPU/Light.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector3.h>

namespace GL {

#define APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(name, ...) \
    if (should_append_to_listing()) {                       \
        append_to_listing<&GLContext::name>(__VA_ARGS__);   \
        if (!should_execute_after_appending_to_listing())   \
            return;                                         \
    }

#define APPEND_TO_CALL_LIST_WITH_ARG_AND_RETURN_IF_NEEDED(name, arg) \
    if (should_append_to_listing()) {                                \
        auto ptr = store_in_listing(arg);                            \
        append_to_listing<&GLContext::name>(*ptr);                   \
        if (!should_execute_after_appending_to_listing())            \
            return;                                                  \
    }

#define RETURN_WITH_ERROR_IF(condition, error)                    \
    if (condition) {                                              \
        dbgln_if(GL_DEBUG, "{}(): error {:#x}", __func__, error); \
        if (m_error == GL_NO_ERROR)                               \
            m_error = error;                                      \
        return;                                                   \
    }

#define RETURN_VALUE_WITH_ERROR_IF(condition, error, return_value) \
    if (condition) {                                               \
        dbgln_if(GL_DEBUG, "{}(): error {:#x}", __func__, error);  \
        if (m_error == GL_NO_ERROR)                                \
            m_error = error;                                       \
        return return_value;                                       \
    }

constexpr size_t MODELVIEW_MATRIX_STACK_LIMIT = 64;
constexpr size_t PROJECTION_MATRIX_STACK_LIMIT = 8;
constexpr size_t TEXTURE_MATRIX_STACK_LIMIT = 8;

struct ContextParameter {
    GLenum type;
    bool is_capability { false };
    u8 count { 1 };
    union {
        bool boolean_value;
        GLint integer_value;
        GLint integer_list[4];
        GLdouble double_value;
        GLdouble double_list[4];
    } value;
};

enum Face {
    Front = 0,
    Back = 1,
};

class GLContext final {
public:
    GLContext(RefPtr<GPU::Driver> driver, NonnullOwnPtr<GPU::Device>, Gfx::Bitmap&);
    ~GLContext();

    NonnullRefPtr<Gfx::Bitmap> frontbuffer() const { return m_frontbuffer; };
    void present();

    void gl_begin(GLenum mode);
    void gl_clear(GLbitfield mask);
    void gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
    void gl_clear_depth(GLdouble depth);
    void gl_clear_stencil(GLint s);
    void gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a);
    void gl_delete_textures(GLsizei n, GLuint const* textures);
    void gl_end();
    void gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val);
    void gl_gen_textures(GLsizei n, GLuint* textures);
    GLenum gl_get_error();
    GLubyte* gl_get_string(GLenum name);
    void gl_load_identity();
    void gl_load_matrix(FloatMatrix4x4 const& matrix);
    void gl_matrix_mode(GLenum mode);
    void gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val);
    void gl_push_matrix();
    void gl_pop_matrix();
    void gl_mult_matrix(FloatMatrix4x4 const& matrix);
    void gl_rotate(GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
    void gl_scale(GLdouble x, GLdouble y, GLdouble z);
    void gl_translate(GLdouble x, GLdouble y, GLdouble z);
    void gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w);
    void gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height);
    void gl_enable(GLenum);
    void gl_disable(GLenum);
    GLboolean gl_is_enabled(GLenum);
    void gl_front_face(GLenum);
    void gl_cull_face(GLenum);
    GLuint gl_gen_lists(GLsizei range);
    void gl_call_list(GLuint list);
    void gl_call_lists(GLsizei n, GLenum type, void const* lists);
    void gl_delete_lists(GLuint list, GLsizei range);
    void gl_list_base(GLuint base);
    void gl_end_list(void);
    void gl_new_list(GLuint list, GLenum mode);
    GLboolean gl_is_list(GLuint list);
    void gl_flush();
    void gl_finish();
    void gl_blend_func(GLenum src_factor, GLenum dst_factor);
    void gl_shade_model(GLenum mode);
    void gl_alpha_func(GLenum func, GLclampf ref);
    void gl_hint(GLenum target, GLenum mode);
    void gl_read_buffer(GLenum mode);
    void gl_draw_buffer(GLenum buffer);
    void gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels);
    void gl_tex_image_2d(GLenum target, GLint level, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, GLvoid const* data);
    void gl_tex_sub_image_2d(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid const* data);
    void gl_tex_parameter(GLenum target, GLenum pname, GLfloat param);
    void gl_tex_parameterfv(GLenum target, GLenum pname, GLfloat const* params);
    void gl_tex_coord(GLfloat s, GLfloat t, GLfloat r, GLfloat q);
    void gl_multi_tex_coord(GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q);
    void gl_tex_env(GLenum target, GLenum pname, GLfloat param);
    void gl_bind_texture(GLenum target, GLuint texture);
    GLboolean gl_is_texture(GLuint texture);
    void gl_active_texture(GLenum texture);
    void gl_depth_mask(GLboolean flag);
    void gl_enable_client_state(GLenum cap);
    void gl_disable_client_state(GLenum cap);
    void gl_client_active_texture(GLenum target);
    void gl_vertex_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer);
    void gl_color_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer);
    void gl_tex_coord_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer);
    void gl_draw_arrays(GLenum mode, GLint first, GLsizei count);
    void gl_draw_elements(GLenum mode, GLsizei count, GLenum type, void const* indices);
    void gl_draw_pixels(GLsizei width, GLsizei height, GLenum format, GLenum type, void const* data);
    void gl_color_mask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
    void gl_get_booleanv(GLenum pname, GLboolean* data);
    void gl_get_doublev(GLenum pname, GLdouble* params);
    void gl_get_floatv(GLenum pname, GLfloat* params);
    void gl_get_integerv(GLenum pname, GLint* data);
    void gl_depth_range(GLdouble min, GLdouble max);
    void gl_depth_func(GLenum func);
    void gl_polygon_mode(GLenum face, GLenum mode);
    void gl_polygon_offset(GLfloat factor, GLfloat units);
    void gl_fogfv(GLenum pname, GLfloat* params);
    void gl_fogf(GLenum pname, GLfloat param);
    void gl_fogi(GLenum pname, GLint param);
    void gl_pixel_storei(GLenum pname, GLint param);
    void gl_scissor(GLint x, GLint y, GLsizei width, GLsizei height);
    void gl_stencil_func_separate(GLenum face, GLenum func, GLint ref, GLuint mask);
    void gl_stencil_mask_separate(GLenum face, GLuint mask);
    void gl_stencil_op_separate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass);
    void gl_normal(GLfloat nx, GLfloat ny, GLfloat nz);
    void gl_normal_pointer(GLenum type, GLsizei stride, void const* pointer);
    void gl_raster_pos(GLfloat x, GLfloat y, GLfloat z, GLfloat w);
    void gl_line_width(GLfloat width);
    void gl_push_attrib(GLbitfield mask);
    void gl_pop_attrib();
    void gl_light_model(GLenum pname, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
    void gl_bitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap);
    void gl_copy_tex_image_2d(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
    void gl_get_tex_parameter_integerv(GLenum target, GLint level, GLenum pname, GLint* params);
    void gl_rect(GLdouble x1, GLdouble y1, GLdouble x2, GLdouble y2);
    void gl_tex_gen(GLenum coord, GLenum pname, GLint param);
    void gl_tex_gen_floatv(GLenum coord, GLenum pname, GLfloat const* params);
    void gl_lightf(GLenum light, GLenum pname, GLfloat param);
    void gl_lightfv(GLenum light, GLenum pname, GLfloat const* params);
    void gl_lightiv(GLenum light, GLenum pname, GLint const* params);
    void gl_materialf(GLenum face, GLenum pname, GLfloat param);
    void gl_materialfv(GLenum face, GLenum pname, GLfloat const* params);
    void gl_materialiv(GLenum face, GLenum pname, GLint const* params);
    void gl_color_material(GLenum face, GLenum mode);
    void gl_get_light(GLenum light, GLenum pname, void* params, GLenum type);
    void gl_get_material(GLenum face, GLenum pname, void* params, GLenum type);
    void gl_clip_plane(GLenum plane, GLdouble const* equation);
    void gl_get_clip_plane(GLenum plane, GLdouble* equation);
    void gl_array_element(GLint i);
    void gl_copy_tex_sub_image_2d(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
    void gl_point_size(GLfloat size);

private:
    void sync_device_config();
    void sync_device_sampler_config();
    void sync_device_texcoord_config();
    void sync_light_state();
    void sync_stencil_configuration();
    void sync_clip_planes();

    void build_extension_string();

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

    Optional<ContextParameter> get_context_parameter(GLenum pname);

    template<typename T>
    void get_floating_point(GLenum pname, T* params);
    template<typename T>
    void get_light_param(GLenum light, GLenum pname, T* params);
    template<typename T>
    void get_material_param(Face face, GLenum pname, T* params);

    void invoke_list(size_t list_index);
    [[nodiscard]] bool should_append_to_listing() const { return m_current_listing_index.has_value(); }
    [[nodiscard]] bool should_execute_after_appending_to_listing() const { return m_current_listing_index.has_value() && m_current_listing_index->mode == GL_COMPILE_AND_EXECUTE; }

    GLenum m_current_draw_mode;
    GLenum m_current_matrix_mode { GL_MODELVIEW };
    FloatMatrix4x4 m_projection_matrix { FloatMatrix4x4::identity() };
    FloatMatrix4x4 m_model_view_matrix { FloatMatrix4x4::identity() };
    FloatMatrix4x4 m_texture_matrix { FloatMatrix4x4::identity() };
    FloatMatrix4x4* m_current_matrix { &m_model_view_matrix };

    Vector<FloatMatrix4x4> m_projection_matrix_stack;
    Vector<FloatMatrix4x4> m_model_view_matrix_stack;
    // FIXME: implement multi-texturing: the texture matrix stack should live inside a texture unit
    Vector<FloatMatrix4x4> m_texture_matrix_stack;
    Vector<FloatMatrix4x4>* m_current_matrix_stack { &m_model_view_matrix_stack };

    Gfx::IntRect m_viewport;

    FloatVector4 m_clear_color { 0.0f, 0.0f, 0.0f, 0.0f };
    float m_clear_depth { 1.f };
    u8 m_clear_stencil { 0 };

    FloatVector4 m_current_vertex_color { 1.0f, 1.0f, 1.0f, 1.0f };
    Vector<FloatVector4> m_current_vertex_tex_coord;
    FloatVector3 m_current_vertex_normal { 0.0f, 0.0f, 1.0f };

    Vector<GPU::Vertex> m_vertex_list;

    GLenum m_error = GL_NO_ERROR;
    bool m_in_draw_state = false;

    bool m_depth_test_enabled { false };
    bool m_depth_offset_enabled { false };

    bool m_cull_faces = false;
    GLenum m_front_face = GL_CCW;
    GLenum m_culled_sides = GL_BACK;

    bool m_blend_enabled = false;
    GLenum m_blend_source_factor = GL_ONE;
    GLenum m_blend_destination_factor = GL_ZERO;

    bool m_alpha_test_enabled = false;
    GLenum m_alpha_test_func = GL_ALWAYS;
    GLclampf m_alpha_test_ref_value = 0;

    bool m_dither_enabled { true };
    bool m_normalize { false };

    // Stencil configuration
    bool m_stencil_test_enabled { false };
    bool m_stencil_configuration_dirty { true };

    struct StencilFunctionOptions {
        GLenum func { GL_ALWAYS };
        GLint reference_value { 0 };
        GLuint mask { NumericLimits<GLuint>::max() };
    };
    Array<StencilFunctionOptions, 2u> m_stencil_function;

    struct StencilOperationOptions {
        GLenum op_fail { GL_KEEP };
        GLenum op_depth_fail { GL_KEEP };
        GLenum op_pass { GL_KEEP };
        GLuint write_mask { NumericLimits<GLuint>::max() };
    };
    Array<StencilOperationOptions, 2u> m_stencil_operation;

    GLenum m_current_read_buffer = GL_BACK;
    GLenum m_current_draw_buffer = GL_BACK;

    // User-defined clip planes
    struct ClipPlaneAttributes {
        Array<FloatVector4, 6> eye_clip_plane; // TODO: Change to use device-defined constant
        GLuint enabled { 0 };
    } m_clip_plane_attributes;
    bool m_clip_planes_dirty { true };

    // Client side arrays
    bool m_client_side_vertex_array_enabled { false };
    bool m_client_side_color_array_enabled { false };
    Vector<bool> m_client_side_texture_coord_array_enabled;
    size_t m_client_active_texture = 0;
    bool m_client_side_normal_array_enabled { false };

    NonnullRefPtr<Gfx::Bitmap> m_frontbuffer;

    // Texture objects
    template<typename T>
    RefPtr<T> get_default_texture(GLenum target)
    {
        auto default_texture = m_default_textures.get(target);
        VERIFY(default_texture.has_value());
        return static_cast<T*>(default_texture.value());
    }

    TextureNameAllocator m_name_allocator;
    HashMap<GLuint, RefPtr<Texture>> m_allocated_textures;
    HashMap<GLenum, RefPtr<Texture>> m_default_textures;
    Vector<TextureUnit> m_texture_units;
    TextureUnit* m_active_texture_unit;
    size_t m_active_texture_unit_index { 0 };

    // Texture coordinate generation state
    struct TextureCoordinateGeneration {
        bool enabled { false };
        GLenum generation_mode { GL_EYE_LINEAR };
        FloatVector4 object_plane_coefficients { 0.0f, 0.0f, 0.0f, 0.0f };
        FloatVector4 eye_plane_coefficients { 0.0f, 0.0f, 0.0f, 0.0f };
    };
    Vector<Array<TextureCoordinateGeneration, 4>> m_texture_coordinate_generation;
    bool m_texcoord_generation_dirty { true };

    ALWAYS_INLINE TextureCoordinateGeneration& texture_coordinate_generation(size_t texture_unit, GLenum capability)
    {
        return m_texture_coordinate_generation[texture_unit][capability - GL_TEXTURE_GEN_S];
    }

    RefPtr<GPU::Driver> m_driver;
    NonnullOwnPtr<GPU::Device> m_rasterizer;
    GPU::DeviceInfo const m_device_info;
    bool m_sampler_config_is_dirty { true };
    bool m_light_state_is_dirty { true };

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
            decltype(&GLContext::gl_begin),
            decltype(&GLContext::gl_clear),
            decltype(&GLContext::gl_clear_color),
            decltype(&GLContext::gl_clear_depth),
            decltype(&GLContext::gl_clear_stencil),
            decltype(&GLContext::gl_color),
            decltype(&GLContext::gl_end),
            decltype(&GLContext::gl_frustum),
            decltype(&GLContext::gl_load_identity),
            decltype(&GLContext::gl_load_matrix),
            decltype(&GLContext::gl_matrix_mode),
            decltype(&GLContext::gl_ortho),
            decltype(&GLContext::gl_push_matrix),
            decltype(&GLContext::gl_pop_matrix),
            decltype(&GLContext::gl_mult_matrix),
            decltype(&GLContext::gl_rotate),
            decltype(&GLContext::gl_scale),
            decltype(&GLContext::gl_translate),
            decltype(&GLContext::gl_vertex),
            decltype(&GLContext::gl_viewport),
            decltype(&GLContext::gl_enable),
            decltype(&GLContext::gl_disable),
            decltype(&GLContext::gl_front_face),
            decltype(&GLContext::gl_cull_face),
            decltype(&GLContext::gl_call_list),
            decltype(&GLContext::gl_call_lists),
            decltype(&GLContext::gl_blend_func),
            decltype(&GLContext::gl_shade_model),
            decltype(&GLContext::gl_alpha_func),
            decltype(&GLContext::gl_hint),
            decltype(&GLContext::gl_read_buffer),
            decltype(&GLContext::gl_tex_parameter),
            decltype(&GLContext::gl_tex_parameterfv),
            decltype(&GLContext::gl_depth_mask),
            decltype(&GLContext::gl_draw_arrays),
            decltype(&GLContext::gl_draw_elements),
            decltype(&GLContext::gl_draw_pixels),
            decltype(&GLContext::gl_depth_range),
            decltype(&GLContext::gl_polygon_offset),
            decltype(&GLContext::gl_scissor),
            decltype(&GLContext::gl_stencil_func_separate),
            decltype(&GLContext::gl_stencil_mask_separate),
            decltype(&GLContext::gl_stencil_op_separate),
            decltype(&GLContext::gl_normal),
            decltype(&GLContext::gl_raster_pos),
            decltype(&GLContext::gl_line_width),
            decltype(&GLContext::gl_push_attrib),
            decltype(&GLContext::gl_pop_attrib),
            decltype(&GLContext::gl_light_model),
            decltype(&GLContext::gl_bitmap),
            decltype(&GLContext::gl_copy_tex_image_2d),
            decltype(&GLContext::gl_rect),
            decltype(&GLContext::gl_tex_gen),
            decltype(&GLContext::gl_tex_gen_floatv),
            decltype(&GLContext::gl_fogf),
            decltype(&GLContext::gl_fogfv),
            decltype(&GLContext::gl_fogi),
            decltype(&GLContext::gl_lightf),
            decltype(&GLContext::gl_lightfv),
            decltype(&GLContext::gl_lightiv),
            decltype(&GLContext::gl_materialf),
            decltype(&GLContext::gl_materialfv),
            decltype(&GLContext::gl_materialiv),
            decltype(&GLContext::gl_color_material),
            decltype(&GLContext::gl_get_light),
            decltype(&GLContext::gl_clip_plane),
            decltype(&GLContext::gl_array_element),
            decltype(&GLContext::gl_copy_tex_sub_image_2d),
            decltype(&GLContext::gl_point_size)>;

        using ExtraSavedArguments = Variant<
            FloatMatrix4x4>;

        Vector<NonnullOwnPtr<ExtraSavedArguments>> saved_arguments;
        Vector<FunctionsAndArgs> entries;
    };

    static constexpr size_t max_allowed_gl_call_depth { 128 };
    size_t m_gl_call_depth { 0 };
    Vector<Listing> m_listings;
    size_t m_list_base { 0 };
    struct CurrentListing {
        Listing listing;
        size_t index { 0 };
        GLenum mode { GL_COMPILE };
    };
    Optional<CurrentListing> m_current_listing_index;

    struct VertexAttribPointer {
        GLint size { 4 };
        GLenum type { GL_FLOAT };
        bool normalize { true };
        GLsizei stride { 0 };
        void const* pointer { 0 };
    };

    static void read_from_vertex_attribute_pointer(VertexAttribPointer const&, int index, float* elements);

    VertexAttribPointer m_client_vertex_pointer;
    VertexAttribPointer m_client_color_pointer;
    Vector<VertexAttribPointer> m_client_tex_coord_pointer;
    VertexAttribPointer m_client_normal_pointer;

    u8 m_pack_alignment { 4 };
    GLsizei m_unpack_row_length { 0 };
    u8 m_unpack_alignment { 4 };

    // Point drawing configuration
    bool m_point_smooth { false };
    float m_point_size { 1.f };

    // Line drawing configuration
    bool m_line_smooth { false };
    float m_line_width { 1.f };

    // Lighting configuration
    bool m_lighting_enabled { false };
    Vector<GPU::Light> m_light_states;
    Array<GPU::Material, 2u> m_material_states;

    // Color material
    bool m_color_material_enabled { false };
    GLenum m_color_material_face { GL_FRONT_AND_BACK };
    GLenum m_color_material_mode { GL_AMBIENT_AND_DIFFUSE };

    // GL Extension string
    String m_extensions;
};

NonnullOwnPtr<GLContext> create_context(Gfx::Bitmap&);
void make_context_current(GLContext*);
void present_context(GLContext*);

}
