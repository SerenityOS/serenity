/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGL/SoftwareGLContext.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Device.h>
#include <LibSoftGPU/Enums.h>
#include <LibSoftGPU/ImageFormat.h>

namespace GL {

static constexpr size_t MODELVIEW_MATRIX_STACK_LIMIT = 64;
static constexpr size_t PROJECTION_MATRIX_STACK_LIMIT = 8;
static constexpr size_t TEXTURE_MATRIX_STACK_LIMIT = 8;

#define APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(name, ...)       \
    if (should_append_to_listing()) {                             \
        append_to_listing<&SoftwareGLContext::name>(__VA_ARGS__); \
        if (!should_execute_after_appending_to_listing())         \
            return;                                               \
    }

#define APPEND_TO_CALL_LIST_WITH_ARG_AND_RETURN_IF_NEEDED(name, arg) \
    if (should_append_to_listing()) {                                \
        auto ptr = store_in_listing(arg);                            \
        append_to_listing<&SoftwareGLContext::name>(*ptr);           \
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

SoftwareGLContext::SoftwareGLContext(Gfx::Bitmap& frontbuffer)
    : m_viewport(frontbuffer.rect())
    , m_frontbuffer(frontbuffer)
    , m_rasterizer(frontbuffer.size())
    , m_device_info(m_rasterizer.info())
{
    m_texture_units.resize(m_device_info.num_texture_units);
    m_active_texture_unit = &m_texture_units[0];

    // Query the number lights from the device and set set up their state
    // locally in the GL
    m_light_states.resize(m_device_info.num_lights);

    // Set-up light0's state, as it has a different default state
    // to the other lights, as per the OpenGL 1.5 spec
    auto& light0 = m_light_states.at(0);
    light0.diffuse_intensity = { 1.0f, 1.0f, 1.0f, 1.0f };
    light0.specular_intensity = { 1.0f, 1.0f, 1.0f, 1.0f };
    m_light_state_is_dirty = true;

    m_client_side_texture_coord_array_enabled.resize(m_device_info.num_texture_units);
    m_client_tex_coord_pointer.resize(m_device_info.num_texture_units);
    m_current_vertex_tex_coord.resize(m_device_info.num_texture_units);
    for (auto& tex_coord : m_current_vertex_tex_coord)
        tex_coord = { 0.0f, 0.0f, 0.0f, 1.0f };

    // Initialize the texture coordinate generation coefficients
    // Indices 0,1,2,3 refer to the S,T,R and Q coordinate of the respective texture
    // coordinate generation config.
    m_texture_coordinate_generation.resize(m_device_info.num_texture_units);
    for (auto& texture_coordinate_generation : m_texture_coordinate_generation) {
        texture_coordinate_generation[0].object_plane_coefficients = { 1.0f, 0.0f, 0.0f, 0.0f };
        texture_coordinate_generation[0].eye_plane_coefficients = { 1.0f, 0.0f, 0.0f, 0.0f };
        texture_coordinate_generation[1].object_plane_coefficients = { 0.0f, 1.0f, 0.0f, 0.0f };
        texture_coordinate_generation[1].eye_plane_coefficients = { 0.0f, 1.0f, 0.0f, 0.0f };
        texture_coordinate_generation[2].object_plane_coefficients = { 0.0f, 0.0f, 0.0f, 0.0f };
        texture_coordinate_generation[2].eye_plane_coefficients = { 0.0f, 0.0f, 0.0f, 0.0f };
        texture_coordinate_generation[3].object_plane_coefficients = { 0.0f, 0.0f, 0.0f, 0.0f };
        texture_coordinate_generation[3].eye_plane_coefficients = { 0.0f, 0.0f, 0.0f, 0.0f };
    }

    build_extension_string();
}

Optional<ContextParameter> SoftwareGLContext::get_context_parameter(GLenum name)
{
    switch (name) {
    case GL_ALPHA_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(float) * 8 } };
    case GL_ALPHA_TEST:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_alpha_test_enabled } };
    case GL_BLEND:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_blend_enabled } };
    case GL_BLEND_DST_ALPHA:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_blend_destination_factor) } };
    case GL_BLEND_SRC_ALPHA:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_blend_source_factor) } };
    case GL_BLUE_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(float) * 8 } };
    case GL_COLOR_MATERIAL:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_color_material_enabled } };
    case GL_COLOR_MATERIAL_FACE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_color_material_face) } };
    case GL_COLOR_MATERIAL_MODE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_color_material_mode) } };
    case GL_CULL_FACE:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_cull_faces } };
    case GL_DEPTH_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(float) * 8 } };
    case GL_DEPTH_TEST:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_depth_test_enabled } };
    case GL_DITHER:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_dither_enabled } };
    case GL_DOUBLEBUFFER:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = true } };
    case GL_FOG: {
        auto fog_enabled = m_rasterizer.options().fog_enabled;
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = fog_enabled } };
    }
    case GL_GREEN_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(float) * 8 } };
    case GL_LIGHTING:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_lighting_enabled } };
    case GL_MAX_LIGHTS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_device_info.num_lights) } };
    case GL_MAX_MODELVIEW_STACK_DEPTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = MODELVIEW_MATRIX_STACK_LIMIT } };
    case GL_MAX_PROJECTION_STACK_DEPTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = PROJECTION_MATRIX_STACK_LIMIT } };
    case GL_MAX_TEXTURE_SIZE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 4096 } };
    case GL_MAX_TEXTURE_STACK_DEPTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = TEXTURE_MATRIX_STACK_LIMIT } };
    case GL_MAX_TEXTURE_UNITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_texture_units.size()) } };
    case GL_NORMALIZE:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_normalize } };
    case GL_PACK_ALIGNMENT:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_pack_alignment } };
    case GL_PACK_IMAGE_HEIGHT:
        return ContextParameter { .type = GL_BOOL, .value = { .integer_value = 0 } };
    case GL_PACK_LSB_FIRST:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = false } };
    case GL_PACK_ROW_LENGTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 0 } };
    case GL_PACK_SKIP_PIXELS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 0 } };
    case GL_PACK_SKIP_ROWS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 0 } };
    case GL_PACK_SWAP_BYTES:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = false } };
    case GL_POLYGON_OFFSET_FILL:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_depth_offset_enabled } };
    case GL_RED_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(float) * 8 } };
    case GL_SCISSOR_BOX: {
        auto scissor_box = m_rasterizer.options().scissor_box;
        return ContextParameter {
            .type = GL_INT,
            .count = 4,
            .value = {
                .integer_list = {
                    scissor_box.x(),
                    scissor_box.y(),
                    scissor_box.width(),
                    scissor_box.height(),
                } }
        };
    } break;
    case GL_SCISSOR_TEST: {
        auto scissor_enabled = m_rasterizer.options().scissor_enabled;
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = scissor_enabled } };
    }
    case GL_STENCIL_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_device_info.stencil_bits } };
    case GL_STENCIL_CLEAR_VALUE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_clear_stencil } };
    case GL_STENCIL_TEST:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_stencil_test_enabled } };
    case GL_TEXTURE_1D:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_active_texture_unit->texture_1d_enabled() } };
    case GL_TEXTURE_2D:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_active_texture_unit->texture_2d_enabled() } };
    case GL_TEXTURE_3D:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_active_texture_unit->texture_3d_enabled() } };
    case GL_TEXTURE_CUBE_MAP:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_active_texture_unit->texture_cube_map_enabled() } };
    case GL_TEXTURE_GEN_Q:
    case GL_TEXTURE_GEN_R:
    case GL_TEXTURE_GEN_S:
    case GL_TEXTURE_GEN_T: {
        auto generation_enabled = texture_coordinate_generation(m_active_texture_unit_index, name).enabled;
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = generation_enabled } };
    }
    case GL_UNPACK_ALIGNMENT:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpack_alignment } };
    case GL_UNPACK_IMAGE_HEIGHT:
        return ContextParameter { .type = GL_BOOL, .value = { .integer_value = 0 } };
    case GL_UNPACK_LSB_FIRST:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = false } };
    case GL_UNPACK_ROW_LENGTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpack_row_length } };
    case GL_UNPACK_SKIP_PIXELS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 0 } };
    case GL_UNPACK_SKIP_ROWS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 0 } };
    case GL_UNPACK_SWAP_BYTES:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = false } };
    case GL_VIEWPORT:
        return ContextParameter {
            .type = GL_INT,
            .count = 4,
            .value = {
                .integer_list = {
                    m_viewport.x(),
                    m_viewport.y(),
                    m_viewport.width(),
                    m_viewport.height(),
                } }
        };
    default:
        dbgln_if(GL_DEBUG, "get_context_parameter({:#x}): unknown context parameter", name);
        return {};
    }
}

void SoftwareGLContext::gl_begin(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_begin, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode > GL_POLYGON, GL_INVALID_ENUM);

    m_current_draw_mode = mode;
    m_in_draw_state = true; // Certain commands will now generate an error
}

void SoftwareGLContext::gl_clear(GLbitfield mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear, mask);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mask & ~(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT), GL_INVALID_ENUM);

    if (mask & GL_COLOR_BUFFER_BIT)
        m_rasterizer.clear_color(m_clear_color);

    if (mask & GL_DEPTH_BUFFER_BIT)
        m_rasterizer.clear_depth(m_clear_depth);

    if (mask & GL_STENCIL_BUFFER_BIT)
        m_rasterizer.clear_stencil(m_clear_stencil);
}

void SoftwareGLContext::gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_color, red, green, blue, alpha);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_color = { red, green, blue, alpha };
    m_clear_color.clamp(0.f, 1.f);
}

void SoftwareGLContext::gl_clear_depth(GLdouble depth)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_depth, depth);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_depth = clamp(static_cast<float>(depth), 0.f, 1.f);
}

void SoftwareGLContext::gl_clear_stencil(GLint s)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_stencil, s);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_stencil = static_cast<u8>(s & ((1 << m_device_info.stencil_bits) - 1));
}

void SoftwareGLContext::gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_color, r, g, b, a);

    m_current_vertex_color = {
        static_cast<float>(r),
        static_cast<float>(g),
        static_cast<float>(b),
        static_cast<float>(a),
    };
}

void SoftwareGLContext::gl_end()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_end);

    // Make sure we had a `glBegin` before this call...
    RETURN_WITH_ERROR_IF(!m_in_draw_state, GL_INVALID_OPERATION);

    m_in_draw_state = false;

    // FIXME: Add support for the remaining primitive types.
    if (m_current_draw_mode != GL_TRIANGLES
        && m_current_draw_mode != GL_TRIANGLE_FAN
        && m_current_draw_mode != GL_TRIANGLE_STRIP
        && m_current_draw_mode != GL_QUADS
        && m_current_draw_mode != GL_QUAD_STRIP
        && m_current_draw_mode != GL_POLYGON) {

        m_vertex_list.clear_with_capacity();
        dbgln_if(GL_DEBUG, "gl_end(): draw mode {:#x} unsupported", m_current_draw_mode);
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    Vector<size_t, 32> enabled_texture_units;
    for (size_t i = 0; i < m_texture_units.size(); ++i) {
        if (m_texture_units[i].texture_2d_enabled())
            enabled_texture_units.append(i);
    }

    sync_device_config();

    SoftGPU::PrimitiveType primitive_type;
    switch (m_current_draw_mode) {
    case GL_TRIANGLES:
        primitive_type = SoftGPU::PrimitiveType::Triangles;
        break;
    case GL_TRIANGLE_STRIP:
    case GL_QUAD_STRIP:
        primitive_type = SoftGPU::PrimitiveType::TriangleStrip;
        break;
    case GL_TRIANGLE_FAN:
    case GL_POLYGON:
        primitive_type = SoftGPU::PrimitiveType::TriangleFan;
        break;
    case GL_QUADS:
        primitive_type = SoftGPU::PrimitiveType::Quads;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // Set up normals transform by taking the upper left 3x3 elements from the model view matrix
    // See section 2.11.3 of the OpenGL 1.5 spec
    auto const& mv_elements = m_model_view_matrix.elements();
    auto const model_view_transposed = FloatMatrix3x3(
        mv_elements[0][0], mv_elements[1][0], mv_elements[2][0],
        mv_elements[0][1], mv_elements[1][1], mv_elements[2][1],
        mv_elements[0][2], mv_elements[1][2], mv_elements[2][2]);
    auto const& normal_transform = model_view_transposed.inverse();

    m_rasterizer.draw_primitives(primitive_type, m_model_view_matrix, normal_transform, m_projection_matrix, m_texture_matrix, m_vertex_list, enabled_texture_units);

    m_vertex_list.clear_with_capacity();
}

void SoftwareGLContext::gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_frustum, left, right, bottom, top, near_val, far_val);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(near_val < 0 || far_val < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(left == right || bottom == top || near_val == far_val, GL_INVALID_VALUE);

    // Let's do some math!
    auto a = static_cast<float>((right + left) / (right - left));
    auto b = static_cast<float>((top + bottom) / (top - bottom));
    auto c = static_cast<float>(-((far_val + near_val) / (far_val - near_val)));
    auto d = static_cast<float>(-((2 * far_val * near_val) / (far_val - near_val)));

    FloatMatrix4x4 frustum {
        static_cast<float>(2 * near_val / (right - left)), 0, a, 0,
        0, static_cast<float>(2 * near_val / (top - bottom)), b, 0,
        0, 0, c, d,
        0, 0, -1, 0
    };

    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * frustum;
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * frustum;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = m_texture_matrix * frustum;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_ortho, left, right, bottom, top, near_val, far_val);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(left == right || bottom == top || near_val == far_val, GL_INVALID_VALUE);

    auto rl = right - left;
    auto tb = top - bottom;
    auto fn = far_val - near_val;
    auto tx = -(right + left) / rl;
    auto ty = -(top + bottom) / tb;
    auto tz = -(far_val + near_val) / fn;

    FloatMatrix4x4 projection {
        static_cast<float>(2 / rl), 0, 0, static_cast<float>(tx),
        0, static_cast<float>(2 / tb), 0, static_cast<float>(ty),
        0, 0, static_cast<float>(-2 / fn), static_cast<float>(tz),
        0, 0, 0, 1
    };

    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * projection;
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * projection;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = m_texture_matrix * projection;
    else
        VERIFY_NOT_REACHED();
}

GLenum SoftwareGLContext::gl_get_error()
{
    if (m_in_draw_state)
        return GL_INVALID_OPERATION;

    auto last_error = m_error;
    m_error = GL_NO_ERROR;
    return last_error;
}

GLubyte* SoftwareGLContext::gl_get_string(GLenum name)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, nullptr);

    switch (name) {
    case GL_VENDOR:
        return reinterpret_cast<GLubyte*>(const_cast<char*>(m_device_info.vendor_name.characters()));
    case GL_RENDERER:
        return reinterpret_cast<GLubyte*>(const_cast<char*>(m_device_info.device_name.characters()));
    case GL_VERSION:
        return reinterpret_cast<GLubyte*>(const_cast<char*>("1.5"));
    case GL_EXTENSIONS:
        return reinterpret_cast<GLubyte*>(const_cast<char*>(m_extensions.characters()));
    case GL_SHADING_LANGUAGE_VERSION:
        return reinterpret_cast<GLubyte*>(const_cast<char*>("0.0"));
    default:
        dbgln_if(GL_DEBUG, "gl_get_string({:#x}): unknown name", name);
        break;
    }

    RETURN_VALUE_WITH_ERROR_IF(true, GL_INVALID_ENUM, nullptr);
}

void SoftwareGLContext::gl_load_identity()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_load_identity);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = FloatMatrix4x4::identity();
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = FloatMatrix4x4::identity();
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = FloatMatrix4x4::identity();
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_load_matrix(const FloatMatrix4x4& matrix)
{
    APPEND_TO_CALL_LIST_WITH_ARG_AND_RETURN_IF_NEEDED(gl_load_matrix, matrix);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = matrix;
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = matrix;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = matrix;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_matrix_mode(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_matrix_mode, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode < GL_MODELVIEW || mode > GL_TEXTURE, GL_INVALID_ENUM);

    m_current_matrix_mode = mode;
}

void SoftwareGLContext::gl_push_matrix()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_push_matrix);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    switch (m_current_matrix_mode) {
    case GL_PROJECTION:
        RETURN_WITH_ERROR_IF(m_projection_matrix_stack.size() >= PROJECTION_MATRIX_STACK_LIMIT, GL_STACK_OVERFLOW);
        m_projection_matrix_stack.append(m_projection_matrix);
        break;
    case GL_MODELVIEW:
        RETURN_WITH_ERROR_IF(m_model_view_matrix_stack.size() >= MODELVIEW_MATRIX_STACK_LIMIT, GL_STACK_OVERFLOW);
        m_model_view_matrix_stack.append(m_model_view_matrix);
        break;
    case GL_TEXTURE:
        RETURN_WITH_ERROR_IF(m_texture_matrix_stack.size() >= TEXTURE_MATRIX_STACK_LIMIT, GL_STACK_OVERFLOW);
        m_texture_matrix_stack.append(m_texture_matrix);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_pop_matrix()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_pop_matrix);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    switch (m_current_matrix_mode) {
    case GL_PROJECTION:
        RETURN_WITH_ERROR_IF(m_projection_matrix_stack.size() == 0, GL_STACK_UNDERFLOW);
        m_projection_matrix = m_projection_matrix_stack.take_last();
        break;
    case GL_MODELVIEW:
        RETURN_WITH_ERROR_IF(m_model_view_matrix_stack.size() == 0, GL_STACK_UNDERFLOW);
        m_model_view_matrix = m_model_view_matrix_stack.take_last();
        break;
    case GL_TEXTURE:
        RETURN_WITH_ERROR_IF(m_texture_matrix_stack.size() == 0, GL_STACK_UNDERFLOW);
        m_texture_matrix = m_texture_matrix_stack.take_last();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_mult_matrix(FloatMatrix4x4 const& matrix)
{
    APPEND_TO_CALL_LIST_WITH_ARG_AND_RETURN_IF_NEEDED(gl_mult_matrix, matrix);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * matrix;
    else if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * matrix;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = m_texture_matrix * matrix;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_rotate, angle, x, y, z);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    FloatVector3 axis = { (float)x, (float)y, (float)z };
    axis.normalize();
    auto rotation_mat = Gfx::rotation_matrix(axis, static_cast<float>(angle * M_PI * 2 / 360));

    if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * rotation_mat;
    else if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * rotation_mat;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = m_texture_matrix * rotation_mat;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_scale(GLdouble x, GLdouble y, GLdouble z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_scale, x, y, z);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto scale_matrix = Gfx::scale_matrix(FloatVector3 { static_cast<float>(x), static_cast<float>(y), static_cast<float>(z) });

    if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * scale_matrix;
    else if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * scale_matrix;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = m_texture_matrix * scale_matrix;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_translate(GLdouble x, GLdouble y, GLdouble z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_translate, x, y, z);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto translation_matrix = Gfx::translation_matrix(FloatVector3 { static_cast<float>(x), static_cast<float>(y), static_cast<float>(z) });

    if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * translation_matrix;
    else if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * translation_matrix;
    else if (m_current_matrix_mode == GL_TEXTURE)
        m_texture_matrix = m_texture_matrix * translation_matrix;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_vertex, x, y, z, w);

    SoftGPU::Vertex vertex;

    vertex.position = { static_cast<float>(x), static_cast<float>(y), static_cast<float>(z), static_cast<float>(w) };
    vertex.color = m_current_vertex_color;
    for (size_t i = 0; i < m_device_info.num_texture_units; ++i)
        vertex.tex_coords[i] = m_current_vertex_tex_coord[i];
    vertex.normal = m_current_vertex_normal;

    m_vertex_list.append(vertex);
}

void SoftwareGLContext::gl_tex_coord(GLfloat s, GLfloat t, GLfloat r, GLfloat q)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_tex_coord, s, t, r, q);

    m_current_vertex_tex_coord[0] = { s, t, r, q };
}

void SoftwareGLContext::gl_multi_tex_coord(GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_multi_tex_coord, target, s, t, r, q);

    RETURN_WITH_ERROR_IF(target < GL_TEXTURE0 || target >= GL_TEXTURE0 + m_device_info.num_texture_units, GL_INVALID_ENUM);

    m_current_vertex_tex_coord[target - GL_TEXTURE0] = { s, t, r, q };
}

void SoftwareGLContext::gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_viewport, x, y, width, height);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    m_viewport = { x, y, width, height };

    auto rasterizer_options = m_rasterizer.options();
    rasterizer_options.viewport = m_viewport;
    m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_enable(GLenum capability)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_enable, capability);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer.options();
    bool update_rasterizer_options = false;

    switch (capability) {
    case GL_COLOR_MATERIAL:
        m_color_material_enabled = true;
        break;
    case GL_CULL_FACE:
        m_cull_faces = true;
        rasterizer_options.enable_culling = true;
        update_rasterizer_options = true;
        break;
    case GL_DEPTH_TEST:
        m_depth_test_enabled = true;
        rasterizer_options.enable_depth_test = true;
        update_rasterizer_options = true;
        break;
    case GL_BLEND:
        m_blend_enabled = true;
        rasterizer_options.enable_blending = true;
        update_rasterizer_options = true;
        break;
    case GL_ALPHA_TEST:
        m_alpha_test_enabled = true;
        rasterizer_options.enable_alpha_test = true;
        update_rasterizer_options = true;
        break;
    case GL_DITHER:
        m_dither_enabled = true;
        break;
    case GL_FOG:
        rasterizer_options.fog_enabled = true;
        update_rasterizer_options = true;
        break;
    case GL_LIGHTING:
        m_lighting_enabled = true;
        rasterizer_options.lighting_enabled = true;
        update_rasterizer_options = true;
        break;
    case GL_NORMALIZE:
        m_normalize = true;
        rasterizer_options.normalization_enabled = true;
        update_rasterizer_options = true;
        break;
    case GL_POLYGON_OFFSET_FILL:
        m_depth_offset_enabled = true;
        rasterizer_options.depth_offset_enabled = true;
        update_rasterizer_options = true;
        break;
    case GL_SCISSOR_TEST:
        rasterizer_options.scissor_enabled = true;
        update_rasterizer_options = true;
        break;
    case GL_STENCIL_TEST:
        m_stencil_test_enabled = true;
        rasterizer_options.enable_stencil_test = true;
        update_rasterizer_options = true;
        break;
    case GL_TEXTURE_1D:
        m_active_texture_unit->set_texture_1d_enabled(true);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_2D:
        m_active_texture_unit->set_texture_2d_enabled(true);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_3D:
        m_active_texture_unit->set_texture_3d_enabled(true);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_CUBE_MAP:
        m_active_texture_unit->set_texture_cube_map_enabled(true);
        m_sampler_config_is_dirty = true;
        break;
    case GL_LIGHT0:
    case GL_LIGHT1:
    case GL_LIGHT2:
    case GL_LIGHT3:
    case GL_LIGHT4:
    case GL_LIGHT5:
    case GL_LIGHT6:
    case GL_LIGHT7:
        m_light_states.at(capability - GL_LIGHT0).is_enabled = true;
        m_light_state_is_dirty = true;
        break;
    case GL_TEXTURE_GEN_Q:
    case GL_TEXTURE_GEN_R:
    case GL_TEXTURE_GEN_S:
    case GL_TEXTURE_GEN_T:
        texture_coordinate_generation(m_active_texture_unit_index, capability).enabled = true;
        m_texcoord_generation_dirty = true;
        break;
    default:
        dbgln_if(GL_DEBUG, "gl_enable({:#x}): unknown parameter", capability);
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    if (update_rasterizer_options)
        m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_disable(GLenum capability)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_disable, capability);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer.options();
    bool update_rasterizer_options = false;

    switch (capability) {
    case GL_COLOR_MATERIAL:
        m_color_material_enabled = false;
        break;
    case GL_CULL_FACE:
        m_cull_faces = false;
        rasterizer_options.enable_culling = false;
        update_rasterizer_options = true;
        break;
    case GL_DEPTH_TEST:
        m_depth_test_enabled = false;
        rasterizer_options.enable_depth_test = false;
        update_rasterizer_options = true;
        break;
    case GL_BLEND:
        m_blend_enabled = false;
        rasterizer_options.enable_blending = false;
        update_rasterizer_options = true;
        break;
    case GL_ALPHA_TEST:
        m_alpha_test_enabled = false;
        rasterizer_options.enable_alpha_test = false;
        update_rasterizer_options = true;
        break;
    case GL_DITHER:
        m_dither_enabled = false;
        break;
    case GL_FOG:
        rasterizer_options.fog_enabled = false;
        update_rasterizer_options = true;
        break;
    case GL_LIGHTING:
        m_lighting_enabled = false;
        rasterizer_options.lighting_enabled = false;
        update_rasterizer_options = true;
        break;
    case GL_LIGHT0:
    case GL_LIGHT1:
    case GL_LIGHT2:
    case GL_LIGHT3:
    case GL_LIGHT4:
    case GL_LIGHT5:
    case GL_LIGHT6:
    case GL_LIGHT7:
        m_light_states.at(capability - GL_LIGHT0).is_enabled = false;
        m_light_state_is_dirty = true;
        break;
    case GL_NORMALIZE:
        m_normalize = false;
        rasterizer_options.normalization_enabled = false;
        update_rasterizer_options = true;
        break;
    case GL_POLYGON_OFFSET_FILL:
        m_depth_offset_enabled = false;
        rasterizer_options.depth_offset_enabled = false;
        update_rasterizer_options = true;
        break;
    case GL_SCISSOR_TEST:
        rasterizer_options.scissor_enabled = false;
        update_rasterizer_options = true;
        break;
    case GL_STENCIL_TEST:
        m_stencil_test_enabled = false;
        rasterizer_options.enable_stencil_test = false;
        update_rasterizer_options = true;
        break;
    case GL_TEXTURE_1D:
        m_active_texture_unit->set_texture_1d_enabled(false);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_2D:
        m_active_texture_unit->set_texture_2d_enabled(false);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_3D:
        m_active_texture_unit->set_texture_3d_enabled(false);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_CUBE_MAP:
        m_active_texture_unit->set_texture_cube_map_enabled(false);
        m_sampler_config_is_dirty = true;
        break;
    case GL_TEXTURE_GEN_Q:
    case GL_TEXTURE_GEN_R:
    case GL_TEXTURE_GEN_S:
    case GL_TEXTURE_GEN_T:
        texture_coordinate_generation(m_active_texture_unit_index, capability).enabled = false;
        m_texcoord_generation_dirty = true;
        break;
    default:
        dbgln_if(GL_DEBUG, "gl_disable({:#x}): unknown parameter", capability);
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    if (update_rasterizer_options)
        m_rasterizer.set_options(rasterizer_options);
}

GLboolean SoftwareGLContext::gl_is_enabled(GLenum capability)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, 0);

    auto optional_parameter = get_context_parameter(capability);
    RETURN_VALUE_WITH_ERROR_IF(!optional_parameter.has_value(), GL_INVALID_ENUM, 0);

    auto parameter = optional_parameter.release_value();
    RETURN_VALUE_WITH_ERROR_IF(!parameter.is_capability, GL_INVALID_ENUM, 0);

    return parameter.value.boolean_value;
}

void SoftwareGLContext::gl_gen_textures(GLsizei n, GLuint* textures)
{
    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_name_allocator.allocate(n, textures);

    // Initialize all texture names with a nullptr
    for (auto i = 0; i < n; i++) {
        GLuint name = textures[i];

        m_allocated_textures.set(name, nullptr);
    }
}

void SoftwareGLContext::gl_delete_textures(GLsizei n, const GLuint* textures)
{
    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    for (auto i = 0; i < n; i++) {
        GLuint name = textures[i];
        if (name == 0)
            continue;

        m_name_allocator.free(name);

        auto texture_object = m_allocated_textures.find(name);
        if (texture_object == m_allocated_textures.end() || texture_object->value.is_null())
            continue;

        // Check all texture units
        for (auto& texture_unit : m_texture_units) {
            if (texture_object->value == texture_unit.bound_texture())
                texture_unit.bind_texture_to_target(GL_TEXTURE_2D, nullptr);
        }

        m_allocated_textures.remove(name);
    }
}

void SoftwareGLContext::gl_tex_image_2d(GLenum target, GLint level, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);

    // Check if there is actually a texture bound
    RETURN_WITH_ERROR_IF(target == GL_TEXTURE_2D && m_active_texture_unit->currently_bound_target() != GL_TEXTURE_2D, GL_INVALID_OPERATION);

    // Internal format can also be a number between 1 and 4. Symbolic formats were only added with EXT_texture, promoted to core in OpenGL 1.1
    if (internal_format == 1)
        internal_format = GL_ALPHA;
    else if (internal_format == 2)
        internal_format = GL_LUMINANCE_ALPHA;
    else if (internal_format == 3)
        internal_format = GL_RGB;
    else if (internal_format == 4)
        internal_format = GL_RGBA;

    // We only support symbolic constants for now
    RETURN_WITH_ERROR_IF(!(internal_format == GL_RGB || internal_format == GL_RGBA || internal_format == GL_LUMINANCE8 || internal_format == GL_LUMINANCE8_ALPHA8), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(type == GL_UNSIGNED_BYTE || type == GL_UNSIGNED_SHORT_5_6_5), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(level < 0 || level > Texture2D::LOG2_MAX_TEXTURE_SIZE, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0 || width > (2 + Texture2D::MAX_TEXTURE_SIZE) || height > (2 + Texture2D::MAX_TEXTURE_SIZE), GL_INVALID_VALUE);
    // Check if width and height are a power of 2
    if (!m_device_info.supports_npot_textures) {
        RETURN_WITH_ERROR_IF(!is_power_of_two(width), GL_INVALID_VALUE);
        RETURN_WITH_ERROR_IF(!is_power_of_two(height), GL_INVALID_VALUE);
    }
    RETURN_WITH_ERROR_IF(border != 0, GL_INVALID_VALUE);

    if (level == 0) {
        // FIXME: OpenGL has the concept of texture and mipmap completeness. A texture has to fulfill certain criteria to be considered complete.
        // Trying to render while an incomplete texture is bound will result in an error.
        // Here we simply create a complete device image when mipmap level 0 is attached to the texture object. This has the unfortunate side effect
        // that constructing GL textures in any but the default mipmap order, going from level 0 upwards will cause mip levels to stay uninitialized.
        // To be spec compliant we should create the device image once the texture has become complete and is used for rendering the first time.
        // All images that were attached before the device image was created need to be stored somewhere to be used to initialize the device image once complete.
        m_active_texture_unit->bound_texture_2d()->set_device_image(m_rasterizer.create_image(SoftGPU::ImageFormat::BGRA8888, width, height, 1, 999, 1));
        m_sampler_config_is_dirty = true;
    }

    m_active_texture_unit->bound_texture_2d()->upload_texture_data(level, internal_format, width, height, format, type, data, m_unpack_row_length, m_unpack_alignment);
}

void SoftwareGLContext::gl_tex_sub_image_2d(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* data)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);

    // Check if there is actually a texture bound
    RETURN_WITH_ERROR_IF(target == GL_TEXTURE_2D && m_active_texture_unit->currently_bound_target() != GL_TEXTURE_2D, GL_INVALID_OPERATION);

    // We only support symbolic constants for now
    RETURN_WITH_ERROR_IF(!(format == GL_RGBA || format == GL_RGB), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(!(type == GL_UNSIGNED_BYTE || type == GL_UNSIGNED_SHORT_5_6_5), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(level < 0 || level > Texture2D::LOG2_MAX_TEXTURE_SIZE, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0 || width > (2 + Texture2D::MAX_TEXTURE_SIZE) || height > (2 + Texture2D::MAX_TEXTURE_SIZE), GL_INVALID_VALUE);

    auto texture = m_active_texture_unit->bound_texture_2d();

    RETURN_WITH_ERROR_IF(xoffset < 0 || yoffset < 0 || xoffset + width > texture->width_at_lod(level) || yoffset + height > texture->height_at_lod(level), GL_INVALID_VALUE);

    texture->replace_sub_texture_data(level, xoffset, yoffset, width, height, format, type, data, m_unpack_row_length, m_unpack_alignment);
}

void SoftwareGLContext::gl_tex_parameter(GLenum target, GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_tex_parameter, target, pname, param);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: We currently only support GL_TETXURE_2D targets. 1D, 3D and CUBE should also be supported (https://docs.gl/gl2/glTexParameter)
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);

    // FIXME: implement the remaining parameters. (https://docs.gl/gl2/glTexParameter)
    RETURN_WITH_ERROR_IF(!(pname == GL_TEXTURE_MIN_FILTER
                             || pname == GL_TEXTURE_MAG_FILTER
                             || pname == GL_TEXTURE_WRAP_S
                             || pname == GL_TEXTURE_WRAP_T),
        GL_INVALID_ENUM);

    if (target == GL_TEXTURE_2D) {
        auto texture2d = m_active_texture_unit->bound_texture_2d();
        if (texture2d.is_null())
            return;

        switch (pname) {
        case GL_TEXTURE_MIN_FILTER:
            RETURN_WITH_ERROR_IF(!(param == GL_NEAREST
                                     || param == GL_LINEAR
                                     || param == GL_NEAREST_MIPMAP_NEAREST
                                     || param == GL_LINEAR_MIPMAP_NEAREST
                                     || param == GL_NEAREST_MIPMAP_LINEAR
                                     || param == GL_LINEAR_MIPMAP_LINEAR),
                GL_INVALID_ENUM);

            texture2d->sampler().set_min_filter(param);
            break;

        case GL_TEXTURE_MAG_FILTER:
            RETURN_WITH_ERROR_IF(!(param == GL_NEAREST
                                     || param == GL_LINEAR),
                GL_INVALID_ENUM);

            texture2d->sampler().set_mag_filter(param);
            break;

        case GL_TEXTURE_WRAP_S:
            RETURN_WITH_ERROR_IF(!(param == GL_CLAMP
                                     || param == GL_CLAMP_TO_BORDER
                                     || param == GL_CLAMP_TO_EDGE
                                     || param == GL_MIRRORED_REPEAT
                                     || param == GL_REPEAT),
                GL_INVALID_ENUM);

            texture2d->sampler().set_wrap_s_mode(param);
            break;

        case GL_TEXTURE_WRAP_T:
            RETURN_WITH_ERROR_IF(!(param == GL_CLAMP
                                     || param == GL_CLAMP_TO_BORDER
                                     || param == GL_CLAMP_TO_EDGE
                                     || param == GL_MIRRORED_REPEAT
                                     || param == GL_REPEAT),
                GL_INVALID_ENUM);

            texture2d->sampler().set_wrap_t_mode(param);
            break;

        default:
            VERIFY_NOT_REACHED();
        }
    }

    m_sampler_config_is_dirty = true;
}

void SoftwareGLContext::gl_front_face(GLenum face)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_front_face, face);

    RETURN_WITH_ERROR_IF(face < GL_CW || face > GL_CCW, GL_INVALID_ENUM);

    m_front_face = face;

    auto rasterizer_options = m_rasterizer.options();
    rasterizer_options.front_face = (face == GL_CW) ? SoftGPU::WindingOrder::Clockwise : SoftGPU::WindingOrder::CounterClockwise;
    m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_cull_face(GLenum cull_mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_cull_face, cull_mode);

    RETURN_WITH_ERROR_IF(cull_mode < GL_FRONT || cull_mode > GL_FRONT_AND_BACK, GL_INVALID_ENUM);

    m_culled_sides = cull_mode;

    auto rasterizer_options = m_rasterizer.options();
    rasterizer_options.cull_back = cull_mode == GL_BACK || cull_mode == GL_FRONT_AND_BACK;
    rasterizer_options.cull_front = cull_mode == GL_FRONT || cull_mode == GL_FRONT_AND_BACK;
    m_rasterizer.set_options(rasterizer_options);
}

GLuint SoftwareGLContext::gl_gen_lists(GLsizei range)
{
    RETURN_VALUE_WITH_ERROR_IF(range <= 0, GL_INVALID_VALUE, 0);
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, 0);

    auto initial_entry = m_listings.size();
    m_listings.resize(range + initial_entry);
    return initial_entry + 1;
}

void SoftwareGLContext::invoke_list(size_t list_index)
{
    auto& listing = m_listings[list_index - 1];
    for (auto& entry : listing.entries) {
        entry.function.visit([&](auto& function) {
            entry.arguments.visit([&](auto& arguments) {
                auto apply = [&]<typename... Args>(Args && ... args)
                {
                    if constexpr (requires { (this->*function)(forward<Args>(args)...); })
                        (this->*function)(forward<Args>(args)...);
                };

                arguments.apply_as_args(apply);
            });
        });
    }
}

void SoftwareGLContext::gl_call_list(GLuint list)
{
    if (m_gl_call_depth > max_allowed_gl_call_depth)
        return;

    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_call_list, list);

    if (m_listings.size() < list)
        return;

    TemporaryChange change { m_gl_call_depth, m_gl_call_depth + 1 };

    invoke_list(list);
}

void SoftwareGLContext::gl_call_lists(GLsizei n, GLenum type, void const* lists)
{
    if (m_gl_call_depth > max_allowed_gl_call_depth)
        return;

    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_call_lists, n, type, lists);

    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(!(type == GL_BYTE
                             || type == GL_UNSIGNED_BYTE
                             || type == GL_SHORT
                             || type == GL_UNSIGNED_SHORT
                             || type == GL_INT
                             || type == GL_UNSIGNED_INT
                             || type == GL_FLOAT
                             || type == GL_2_BYTES
                             || type == GL_3_BYTES
                             || type == GL_4_BYTES),
        GL_INVALID_ENUM);

    TemporaryChange change { m_gl_call_depth, m_gl_call_depth + 1 };

    auto invoke_all_lists = [&]<typename T>(T const* lists) {
        for (int i = 0; i < n; ++i) {
            auto list = static_cast<size_t>(lists[i]);
            invoke_list(m_list_base + list);
        }
    };
    switch (type) {
    case GL_BYTE:
        invoke_all_lists(static_cast<GLbyte const*>(lists));
        break;
    case GL_UNSIGNED_BYTE:
        invoke_all_lists(static_cast<GLubyte const*>(lists));
        break;
    case GL_SHORT:
        invoke_all_lists(static_cast<GLshort const*>(lists));
        break;
    case GL_UNSIGNED_SHORT:
        invoke_all_lists(static_cast<GLushort const*>(lists));
        break;
    case GL_INT:
        invoke_all_lists(static_cast<GLint const*>(lists));
        break;
    case GL_UNSIGNED_INT:
        invoke_all_lists(static_cast<GLuint const*>(lists));
        break;
    case GL_FLOAT:
        invoke_all_lists(static_cast<GLfloat const*>(lists));
        break;
    case GL_2_BYTES:
    case GL_3_BYTES:
    case GL_4_BYTES:
        dbgln("SoftwareGLContext FIXME: unimplemented glCallLists() with type {}", type);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_delete_lists(GLuint list, GLsizei range)
{
    if (m_listings.size() < list || m_listings.size() <= list + range)
        return;

    for (auto& entry : m_listings.span().slice(list - 1, range))
        entry.entries.clear_with_capacity();
}

void SoftwareGLContext::gl_list_base(GLuint base)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_list_base, base);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_list_base = base;
}

void SoftwareGLContext::gl_end_list()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!m_current_listing_index.has_value(), GL_INVALID_OPERATION);

    m_listings[m_current_listing_index->index] = move(m_current_listing_index->listing);
    m_current_listing_index.clear();
}

void SoftwareGLContext::gl_new_list(GLuint list, GLenum mode)
{
    RETURN_WITH_ERROR_IF(list == 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(mode != GL_COMPILE && mode != GL_COMPILE_AND_EXECUTE, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(m_current_listing_index.has_value(), GL_INVALID_OPERATION);

    if (m_listings.size() < list)
        return;

    m_current_listing_index = CurrentListing { {}, static_cast<size_t>(list - 1), mode };
}

GLboolean SoftwareGLContext::gl_is_list(GLuint list)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, GL_FALSE);

    return list < m_listings.size() ? GL_TRUE : GL_FALSE;
}

void SoftwareGLContext::gl_flush()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // No-op since SoftwareGLContext is completely synchronous at the moment
}

void SoftwareGLContext::gl_finish()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // No-op since SoftwareGLContext is completely synchronous at the moment
}

void SoftwareGLContext::gl_blend_func(GLenum src_factor, GLenum dst_factor)
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

    auto map_gl_blend_factor_to_device = [](GLenum factor) constexpr
    {
        switch (factor) {
        case GL_ZERO:
            return SoftGPU::BlendFactor::Zero;
        case GL_ONE:
            return SoftGPU::BlendFactor::One;
        case GL_SRC_ALPHA:
            return SoftGPU::BlendFactor::SrcAlpha;
        case GL_ONE_MINUS_SRC_ALPHA:
            return SoftGPU::BlendFactor::OneMinusSrcAlpha;
        case GL_SRC_COLOR:
            return SoftGPU::BlendFactor::SrcColor;
        case GL_ONE_MINUS_SRC_COLOR:
            return SoftGPU::BlendFactor::OneMinusSrcColor;
        case GL_DST_ALPHA:
            return SoftGPU::BlendFactor::DstAlpha;
        case GL_ONE_MINUS_DST_ALPHA:
            return SoftGPU::BlendFactor::OneMinusDstAlpha;
        case GL_DST_COLOR:
            return SoftGPU::BlendFactor::DstColor;
        case GL_ONE_MINUS_DST_COLOR:
            return SoftGPU::BlendFactor::OneMinusDstColor;
        case GL_SRC_ALPHA_SATURATE:
            return SoftGPU::BlendFactor::SrcAlphaSaturate;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto options = m_rasterizer.options();
    options.blend_source_factor = map_gl_blend_factor_to_device(m_blend_source_factor);
    options.blend_destination_factor = map_gl_blend_factor_to_device(m_blend_destination_factor);
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_shade_model(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_shade_model, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode != GL_FLAT && mode != GL_SMOOTH, GL_INVALID_ENUM);

    auto options = m_rasterizer.options();
    options.shade_smooth = (mode == GL_SMOOTH);
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_alpha_func(GLenum func, GLclampf ref)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_alpha_func, func, ref);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(func < GL_NEVER || func > GL_ALWAYS, GL_INVALID_ENUM);

    m_alpha_test_func = func;
    m_alpha_test_ref_value = ref;

    auto options = m_rasterizer.options();

    switch (func) {
    case GL_NEVER:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::Never;
        break;
    case GL_ALWAYS:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::Always;
        break;
    case GL_LESS:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::Less;
        break;
    case GL_LEQUAL:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::LessOrEqual;
        break;
    case GL_EQUAL:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::Equal;
        break;
    case GL_NOTEQUAL:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::NotEqual;
        break;
    case GL_GEQUAL:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::GreaterOrEqual;
        break;
    case GL_GREATER:
        options.alpha_test_func = SoftGPU::AlphaTestFunction::Greater;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    options.alpha_test_ref_value = m_alpha_test_ref_value;
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_hint(GLenum target, GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_hint, target, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(target != GL_PERSPECTIVE_CORRECTION_HINT
            && target != GL_POINT_SMOOTH_HINT
            && target != GL_LINE_SMOOTH_HINT
            && target != GL_POLYGON_SMOOTH_HINT
            && target != GL_FOG_HINT
            && target != GL_GENERATE_MIPMAP_HINT
            && target != GL_TEXTURE_COMPRESSION_HINT,
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(mode != GL_DONT_CARE
            && mode != GL_FASTEST
            && mode != GL_NICEST,
        GL_INVALID_ENUM);

    // According to the spec implementors are free to ignore glHint. So we do.
}

void SoftwareGLContext::gl_read_buffer(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_read_buffer, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Also allow aux buffers GL_AUX0 through GL_AUX3 here
    // plus any aux buffer between 0 and GL_AUX_BUFFERS
    RETURN_WITH_ERROR_IF(mode != GL_FRONT_LEFT
            && mode != GL_FRONT_RIGHT
            && mode != GL_BACK_LEFT
            && mode != GL_BACK_RIGHT
            && mode != GL_FRONT
            && mode != GL_BACK
            && mode != GL_LEFT
            && mode != GL_RIGHT,
        GL_INVALID_ENUM);

    // FIXME: We do not currently have aux buffers, so make it an invalid
    // operation to select anything but front or back buffers. Also we do
    // not allow selecting the stereoscopic RIGHT buffers since we do not
    // have them configured.
    RETURN_WITH_ERROR_IF(mode != GL_FRONT_LEFT
            && mode != GL_FRONT
            && mode != GL_BACK_LEFT
            && mode != GL_BACK
            && mode != GL_FRONT
            && mode != GL_BACK
            && mode != GL_LEFT,
        GL_INVALID_OPERATION);

    m_current_read_buffer = mode;
}

void SoftwareGLContext::gl_draw_buffer(GLenum buffer)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_draw_buffer, buffer);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Also allow aux buffers GL_AUX0 through GL_AUX3 here
    // plus any aux buffer between 0 and GL_AUX_BUFFERS
    RETURN_WITH_ERROR_IF(buffer != GL_NONE
            && buffer != GL_FRONT_LEFT
            && buffer != GL_FRONT_RIGHT
            && buffer != GL_BACK_LEFT
            && buffer != GL_BACK_RIGHT
            && buffer != GL_FRONT
            && buffer != GL_BACK
            && buffer != GL_LEFT
            && buffer != GL_RIGHT,
        GL_INVALID_ENUM);

    // FIXME: We do not currently have aux buffers, so make it an invalid
    // operation to select anything but front or back buffers. Also we do
    // not allow selecting the stereoscopic RIGHT buffers since we do not
    // have them configured.
    RETURN_WITH_ERROR_IF(buffer != GL_NONE
            && buffer != GL_FRONT_LEFT
            && buffer != GL_FRONT
            && buffer != GL_BACK_LEFT
            && buffer != GL_BACK
            && buffer != GL_FRONT
            && buffer != GL_BACK
            && buffer != GL_LEFT,
        GL_INVALID_OPERATION);

    m_current_draw_buffer = buffer;

    auto rasterizer_options = m_rasterizer.options();
    // FIXME: We only have a single draw buffer in SoftGPU at the moment,
    // so we simply disable color writes if GL_NONE is selected
    rasterizer_options.enable_color_write = m_current_draw_buffer != GL_NONE;
    m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    RETURN_WITH_ERROR_IF(format != GL_COLOR_INDEX
            && format != GL_STENCIL_INDEX
            && format != GL_DEPTH_COMPONENT
            && format != GL_RED
            && format != GL_GREEN
            && format != GL_BLUE
            && format != GL_ALPHA
            && format != GL_RGB
            && format != GL_RGBA
            && format != GL_LUMINANCE
            && format != GL_LUMINANCE_ALPHA,
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(type != GL_UNSIGNED_BYTE
            && type != GL_BYTE
            && type != GL_BITMAP
            && type != GL_UNSIGNED_SHORT
            && type != GL_SHORT
            && type != GL_BLUE
            && type != GL_UNSIGNED_INT
            && type != GL_INT
            && type != GL_FLOAT,
        GL_INVALID_ENUM);

    // FIXME: We only support RGBA buffers for now.
    // Once we add support for indexed color modes do the correct check here
    RETURN_WITH_ERROR_IF(format == GL_COLOR_INDEX, GL_INVALID_OPERATION);

    // FIXME: We do not have stencil buffers yet
    // Once we add support for stencil buffers do the correct check here
    RETURN_WITH_ERROR_IF(format == GL_STENCIL_INDEX, GL_INVALID_OPERATION);

    if (format == GL_DEPTH_COMPONENT) {
        // FIXME: This check needs to be a bit more sophisticated. Currently the buffers
        // are hardcoded. Once we add proper structures for them we need to correct this check

        // Error because only back buffer has a depth buffer
        RETURN_WITH_ERROR_IF(m_current_read_buffer == GL_FRONT
                || m_current_read_buffer == GL_FRONT_LEFT
                || m_current_read_buffer == GL_FRONT_RIGHT,
            GL_INVALID_OPERATION);
    }

    // Some helper functions for converting float values to integer types
    auto float_to_i8 = [](float f) -> GLchar {
        return static_cast<GLchar>((0x7f * min(max(f, 0.0f), 1.0f) - 1) / 2);
    };

    auto float_to_i16 = [](float f) -> GLshort {
        return static_cast<GLshort>((0x7fff * min(max(f, 0.0f), 1.0f) - 1) / 2);
    };

    auto float_to_i32 = [](float f) -> GLint {
        return static_cast<GLint>((0x7fffffff * min(max(f, 0.0f), 1.0f) - 1) / 2);
    };

    auto float_to_u8 = [](float f) -> GLubyte {
        return static_cast<GLubyte>(0xff * min(max(f, 0.0f), 1.0f));
    };

    auto float_to_u16 = [](float f) -> GLushort {
        return static_cast<GLushort>(0xffff * min(max(f, 0.0f), 1.0f));
    };

    auto float_to_u32 = [](float f) -> GLuint {
        return static_cast<GLuint>(0xffffffff * min(max(f, 0.0f), 1.0f));
    };

    u8 component_size = 0;
    switch (type) {
    case GL_BYTE:
    case GL_UNSIGNED_BYTE:
        component_size = 1;
        break;
    case GL_SHORT:
    case GL_UNSIGNED_SHORT:
        component_size = 2;
        break;
    case GL_INT:
    case GL_UNSIGNED_INT:
    case GL_FLOAT:
        component_size = 4;
        break;
    }

    if (format == GL_DEPTH_COMPONENT) {
        auto const row_stride = (width * component_size + m_pack_alignment - 1) / m_pack_alignment * m_pack_alignment;

        // Read from depth buffer
        for (GLsizei i = 0; i < height; ++i) {
            for (GLsizei j = 0; j < width; ++j) {
                float depth = m_rasterizer.get_depthbuffer_value(x + j, y + i);
                auto char_ptr = reinterpret_cast<char*>(pixels) + i * row_stride + j * component_size;

                switch (type) {
                case GL_BYTE:
                    *reinterpret_cast<GLchar*>(char_ptr) = float_to_i8(depth);
                    break;
                case GL_SHORT:
                    *reinterpret_cast<GLshort*>(char_ptr) = float_to_i16(depth);
                    break;
                case GL_INT:
                    *reinterpret_cast<GLint*>(char_ptr) = float_to_i32(depth);
                    break;
                case GL_UNSIGNED_BYTE:
                    *reinterpret_cast<GLubyte*>(char_ptr) = float_to_u8(depth);
                    break;
                case GL_UNSIGNED_SHORT:
                    *reinterpret_cast<GLushort*>(char_ptr) = float_to_u16(depth);
                    break;
                case GL_UNSIGNED_INT:
                    *reinterpret_cast<GLuint*>(char_ptr) = float_to_u32(depth);
                    break;
                case GL_FLOAT:
                    *reinterpret_cast<GLfloat*>(char_ptr) = min(max(depth, 0.0f), 1.0f);
                    break;
                }
            }
        }
        return;
    }

    bool write_red = false;
    bool write_green = false;
    bool write_blue = false;
    bool write_alpha = false;
    size_t component_count = 0;
    size_t red_offset = 0;
    size_t green_offset = 0;
    size_t blue_offset = 0;
    size_t alpha_offset = 0;
    char* red_ptr = nullptr;
    char* green_ptr = nullptr;
    char* blue_ptr = nullptr;
    char* alpha_ptr = nullptr;

    switch (format) {
    case GL_RGB:
        write_red = true;
        write_green = true;
        write_blue = true;
        component_count = 3;
        red_offset = 2;
        green_offset = 1;
        blue_offset = 0;
        break;
    case GL_RGBA:
        write_red = true;
        write_green = true;
        write_blue = true;
        write_alpha = true;
        component_count = 4;
        red_offset = 3;
        green_offset = 2;
        blue_offset = 1;
        alpha_offset = 0;
        break;
    case GL_RED:
        write_red = true;
        component_count = 1;
        red_offset = 0;
        break;
    case GL_GREEN:
        write_green = true;
        component_count = 1;
        green_offset = 0;
        break;
    case GL_BLUE:
        write_blue = true;
        component_count = 1;
        blue_offset = 0;
        break;
    case GL_ALPHA:
        write_alpha = true;
        component_count = 1;
        alpha_offset = 0;
        break;
    }

    auto const pixel_bytes = component_size * component_count;
    auto const row_alignment_bytes = (m_pack_alignment - ((width * pixel_bytes) % m_pack_alignment)) % m_pack_alignment;

    char* out_ptr = reinterpret_cast<char*>(pixels);
    for (int i = 0; i < (int)height; ++i) {
        for (int j = 0; j < (int)width; ++j) {
            Gfx::RGBA32 color {};
            if (m_current_read_buffer == GL_FRONT || m_current_read_buffer == GL_LEFT || m_current_read_buffer == GL_FRONT_LEFT) {
                if (y + i >= m_frontbuffer->width() || x + j >= m_frontbuffer->height())
                    color = 0;
                else
                    color = m_frontbuffer->scanline(y + i)[x + j];
            } else {
                color = m_rasterizer.get_color_buffer_pixel(x + j, y + i);
            }

            float red = ((color >> 24) & 0xff) / 255.0f;
            float green = ((color >> 16) & 0xff) / 255.0f;
            float blue = ((color >> 8) & 0xff) / 255.0f;
            float alpha = (color & 0xff) / 255.0f;

            // FIXME: Set up write pointers based on selected endianness (glPixelStore)
            red_ptr = out_ptr + (component_size * red_offset);
            green_ptr = out_ptr + (component_size * green_offset);
            blue_ptr = out_ptr + (component_size * blue_offset);
            alpha_ptr = out_ptr + (component_size * alpha_offset);

            switch (type) {
            case GL_BYTE:
                if (write_red)
                    *reinterpret_cast<GLchar*>(red_ptr) = float_to_i8(red);
                if (write_green)
                    *reinterpret_cast<GLchar*>(green_ptr) = float_to_i8(green);
                if (write_blue)
                    *reinterpret_cast<GLchar*>(blue_ptr) = float_to_i8(blue);
                if (write_alpha)
                    *reinterpret_cast<GLchar*>(alpha_ptr) = float_to_i8(alpha);
                break;
            case GL_UNSIGNED_BYTE:
                if (write_red)
                    *reinterpret_cast<GLubyte*>(red_ptr) = float_to_u8(red);
                if (write_green)
                    *reinterpret_cast<GLubyte*>(green_ptr) = float_to_u8(green);
                if (write_blue)
                    *reinterpret_cast<GLubyte*>(blue_ptr) = float_to_u8(blue);
                if (write_alpha)
                    *reinterpret_cast<GLubyte*>(alpha_ptr) = float_to_u8(alpha);
                break;
            case GL_SHORT:
                if (write_red)
                    *reinterpret_cast<GLshort*>(red_ptr) = float_to_i16(red);
                if (write_green)
                    *reinterpret_cast<GLshort*>(green_ptr) = float_to_i16(green);
                if (write_blue)
                    *reinterpret_cast<GLshort*>(blue_ptr) = float_to_i16(blue);
                if (write_alpha)
                    *reinterpret_cast<GLshort*>(alpha_ptr) = float_to_i16(alpha);
                break;
            case GL_UNSIGNED_SHORT:
                if (write_red)
                    *reinterpret_cast<GLushort*>(red_ptr) = float_to_u16(red);
                if (write_green)
                    *reinterpret_cast<GLushort*>(green_ptr) = float_to_u16(green);
                if (write_blue)
                    *reinterpret_cast<GLushort*>(blue_ptr) = float_to_u16(blue);
                if (write_alpha)
                    *reinterpret_cast<GLushort*>(alpha_ptr) = float_to_u16(alpha);
                break;
            case GL_INT:
                if (write_red)
                    *reinterpret_cast<GLint*>(red_ptr) = float_to_i32(red);
                if (write_green)
                    *reinterpret_cast<GLint*>(green_ptr) = float_to_i32(green);
                if (write_blue)
                    *reinterpret_cast<GLint*>(blue_ptr) = float_to_i32(blue);
                if (write_alpha)
                    *reinterpret_cast<GLint*>(alpha_ptr) = float_to_i32(alpha);
                break;
            case GL_UNSIGNED_INT:
                if (write_red)
                    *reinterpret_cast<GLuint*>(red_ptr) = float_to_u32(red);
                if (write_green)
                    *reinterpret_cast<GLuint*>(green_ptr) = float_to_u32(green);
                if (write_blue)
                    *reinterpret_cast<GLuint*>(blue_ptr) = float_to_u32(blue);
                if (write_alpha)
                    *reinterpret_cast<GLuint*>(alpha_ptr) = float_to_u32(alpha);
                break;
            case GL_FLOAT:
                if (write_red)
                    *reinterpret_cast<GLfloat*>(red_ptr) = min(max(red, 0.0f), 1.0f);
                if (write_green)
                    *reinterpret_cast<GLfloat*>(green_ptr) = min(max(green, 0.0f), 1.0f);
                if (write_blue)
                    *reinterpret_cast<GLfloat*>(blue_ptr) = min(max(blue, 0.0f), 1.0f);
                if (write_alpha)
                    *reinterpret_cast<GLfloat*>(alpha_ptr) = min(max(alpha, 0.0f), 1.0f);
                break;
            }

            out_ptr += pixel_bytes;
        }

        out_ptr += row_alignment_bytes;
    }
}

void SoftwareGLContext::gl_bind_texture(GLenum target, GLuint texture)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    // FIXME: We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);

    if (texture == 0) {
        switch (target) {
        case GL_TEXTURE_2D:
            m_active_texture_unit->bind_texture_to_target(target, nullptr);
            m_sampler_config_is_dirty = true;
            return;
        default:
            VERIFY_NOT_REACHED();
            return;
        }
    }

    auto it = m_allocated_textures.find(texture);
    RefPtr<Texture> texture_object;

    // OpenGL 1.x supports binding texture names that were not previously generated by glGenTextures.
    // If there is not an allocated texture, meaning it was not previously generated by glGenTextures,
    // we can keep texture_object null to both allocate and bind the texture with the passed in texture name.
    // FIXME: Later OpenGL versions such as 4.x enforce that texture names being bound were previously generated
    //        by glGenTextures.
    if (it != m_allocated_textures.end())
        texture_object = it->value;

    // Binding a texture to a different target than it was first bound is an invalid operation
    // FIXME: We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target == GL_TEXTURE_2D && !texture_object.is_null() && !texture_object->is_texture_2d(), GL_INVALID_OPERATION);

    if (!texture_object) {
        // This is the first time the texture is bound. Allocate an actual texture object
        switch (target) {
        case GL_TEXTURE_2D:
            texture_object = adopt_ref(*new Texture2D());
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        m_allocated_textures.set(texture, texture_object);
    }

    switch (target) {
    case GL_TEXTURE_2D:
        m_active_texture_unit->bind_texture_to_target(target, texture_object);
        break;
    }

    m_sampler_config_is_dirty = true;
}

GLboolean SoftwareGLContext::gl_is_texture(GLuint texture)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, GL_FALSE);

    if (texture == 0)
        return GL_FALSE;

    auto it = m_allocated_textures.find(texture);
    if (it == m_allocated_textures.end())
        return GL_FALSE;

    return it->value.is_null() ? GL_FALSE : GL_TRUE;
}

void SoftwareGLContext::gl_active_texture(GLenum texture)
{
    RETURN_WITH_ERROR_IF(texture < GL_TEXTURE0 || texture >= GL_TEXTURE0 + m_device_info.num_texture_units, GL_INVALID_ENUM);

    m_active_texture_unit_index = texture - GL_TEXTURE0;
    m_active_texture_unit = &m_texture_units.at(m_active_texture_unit_index);
}

void SoftwareGLContext::gl_get_booleanv(GLenum pname, GLboolean* data)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto optional_parameter = get_context_parameter(pname);
    RETURN_WITH_ERROR_IF(!optional_parameter.has_value(), GL_INVALID_ENUM);
    auto parameter = optional_parameter.release_value();

    switch (parameter.type) {
    case GL_BOOL:
        *data = parameter.value.boolean_value ? GL_TRUE : GL_FALSE;
        break;
    case GL_DOUBLE:
        *data = (parameter.value.double_value == 0.0) ? GL_FALSE : GL_TRUE;
        break;
    case GL_INT:
        *data = (parameter.value.integer_value == 0) ? GL_FALSE : GL_TRUE;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_get_doublev(GLenum pname, GLdouble* params)
{
    get_floating_point(pname, params);
}

void SoftwareGLContext::gl_get_floatv(GLenum pname, GLfloat* params)
{
    get_floating_point(pname, params);
}

template<typename T>
void SoftwareGLContext::get_floating_point(GLenum pname, T* params)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // Handle matrix retrieval first
    auto flatten_and_assign_matrix = [&params](FloatMatrix4x4 const& matrix) {
        auto elements = matrix.elements();
        for (size_t i = 0; i < 4; ++i) {
            for (size_t j = 0; j < 4; ++j) {
                // Return transposed matrix since OpenGL defines them as column-major
                params[i * 4 + j] = static_cast<T>(elements[j][i]);
            }
        }
    };
    switch (pname) {
    case GL_MODELVIEW_MATRIX:
        flatten_and_assign_matrix(m_model_view_matrix);
        return;
    case GL_PROJECTION_MATRIX:
        flatten_and_assign_matrix(m_projection_matrix);
        return;
    }

    // Regular parameters
    auto optional_parameter = get_context_parameter(pname);
    RETURN_WITH_ERROR_IF(!optional_parameter.has_value(), GL_INVALID_ENUM);
    auto parameter = optional_parameter.release_value();

    switch (parameter.type) {
    case GL_BOOL:
        *params = parameter.value.boolean_value ? GL_TRUE : GL_FALSE;
        break;
    case GL_DOUBLE:
        for (size_t i = 0; i < parameter.count; ++i)
            params[i] = parameter.value.double_list[i];
        break;
    case GL_INT:
        for (size_t i = 0; i < parameter.count; ++i)
            params[i] = parameter.value.integer_list[i];
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_get_integerv(GLenum pname, GLint* data)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto optional_parameter = get_context_parameter(pname);
    RETURN_WITH_ERROR_IF(!optional_parameter.has_value(), GL_INVALID_ENUM);
    auto parameter = optional_parameter.release_value();

    switch (parameter.type) {
    case GL_BOOL:
        *data = parameter.value.boolean_value ? GL_TRUE : GL_FALSE;
        break;
    case GL_DOUBLE: {
        double const int_range = static_cast<double>(NumericLimits<GLint>::max()) - NumericLimits<GLint>::min();
        for (size_t i = 0; i < parameter.count; ++i) {
            double const result_factor = (clamp(parameter.value.double_list[i], -1.0, 1.0) + 1.0) / 2.0;
            data[i] = static_cast<GLint>(NumericLimits<GLint>::min() + result_factor * int_range);
        }
        break;
    }
    case GL_INT:
        for (size_t i = 0; i < parameter.count; ++i)
            data[i] = parameter.value.integer_list[i];
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_depth_mask(GLboolean flag)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_depth_mask, flag);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer.options();
    options.enable_depth_write = (flag != GL_FALSE);
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_enable_client_state(GLenum cap)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    switch (cap) {
    case GL_VERTEX_ARRAY:
        m_client_side_vertex_array_enabled = true;
        break;

    case GL_COLOR_ARRAY:
        m_client_side_color_array_enabled = true;
        break;

    case GL_TEXTURE_COORD_ARRAY:
        m_client_side_texture_coord_array_enabled[m_client_active_texture] = true;
        break;

    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }
}

void SoftwareGLContext::gl_disable_client_state(GLenum cap)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    switch (cap) {
    case GL_VERTEX_ARRAY:
        m_client_side_vertex_array_enabled = false;
        break;

    case GL_COLOR_ARRAY:
        m_client_side_color_array_enabled = false;
        break;

    case GL_TEXTURE_COORD_ARRAY:
        m_client_side_texture_coord_array_enabled[m_client_active_texture] = false;
        break;

    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }
}

void SoftwareGLContext::gl_client_active_texture(GLenum target)
{
    RETURN_WITH_ERROR_IF(target < GL_TEXTURE0 || target >= GL_TEXTURE0 + m_device_info.num_texture_units, GL_INVALID_ENUM);

    m_client_active_texture = target - GL_TEXTURE0;
}

void SoftwareGLContext::gl_vertex_pointer(GLint size, GLenum type, GLsizei stride, const void* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(size == 2 || size == 3 || size == 4), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(!(type == GL_SHORT || type == GL_INT || type == GL_FLOAT || type == GL_DOUBLE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    m_client_vertex_pointer.size = size;
    m_client_vertex_pointer.type = type;
    m_client_vertex_pointer.stride = stride;
    m_client_vertex_pointer.pointer = pointer;
}

void SoftwareGLContext::gl_color_pointer(GLint size, GLenum type, GLsizei stride, const void* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(size == 3 || size == 4), GL_INVALID_VALUE);

    RETURN_WITH_ERROR_IF(!(type == GL_BYTE
                             || type == GL_UNSIGNED_BYTE
                             || type == GL_SHORT
                             || type == GL_UNSIGNED_SHORT
                             || type == GL_INT
                             || type == GL_UNSIGNED_INT
                             || type == GL_FLOAT
                             || type == GL_DOUBLE),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    m_client_color_pointer.size = size;
    m_client_color_pointer.type = type;
    m_client_color_pointer.stride = stride;
    m_client_color_pointer.pointer = pointer;
}

void SoftwareGLContext::gl_tex_coord_pointer(GLint size, GLenum type, GLsizei stride, const void* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(size == 1 || size == 2 || size == 3 || size == 4), GL_INVALID_VALUE);

    RETURN_WITH_ERROR_IF(!(type == GL_SHORT || type == GL_INT || type == GL_FLOAT || type == GL_DOUBLE), GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    auto& tex_coord_pointer = m_client_tex_coord_pointer[m_client_active_texture];

    tex_coord_pointer.size = size;
    tex_coord_pointer.type = type;
    tex_coord_pointer.stride = stride;
    tex_coord_pointer.pointer = pointer;
}

void SoftwareGLContext::gl_tex_env(GLenum target, GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_tex_env, target, pname, param);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: We currently only support a subset of possible target values. Implement the rest!
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_ENV, GL_INVALID_ENUM);

    // FIXME: We currently only support a subset of possible pname values. Implement the rest!
    RETURN_WITH_ERROR_IF(pname != GL_TEXTURE_ENV_MODE, GL_INVALID_ENUM);

    auto param_enum = static_cast<GLenum>(param);

    switch (param_enum) {
    case GL_MODULATE:
    case GL_REPLACE:
    case GL_DECAL:
        m_active_texture_unit->set_env_mode(param_enum);
        break;
    default:
        // FIXME: We currently only support a subset of possible param values. Implement the rest!
        dbgln_if(GL_DEBUG, "gl_tex_env({:#x}, {:#x}, {}): param unimplemented", target, pname, param);
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }
}

void SoftwareGLContext::gl_draw_arrays(GLenum mode, GLint first, GLsizei count)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_draw_arrays, mode, first, count);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Some modes are still missing (GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES)
    RETURN_WITH_ERROR_IF(!(mode == GL_TRIANGLE_STRIP
                             || mode == GL_TRIANGLE_FAN
                             || mode == GL_TRIANGLES
                             || mode == GL_QUADS
                             || mode == GL_QUAD_STRIP
                             || mode == GL_POLYGON),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(count < 0, GL_INVALID_VALUE);

    // At least the vertex array needs to be enabled
    if (!m_client_side_vertex_array_enabled)
        return;

    auto last = first + count;
    gl_begin(mode);
    for (int i = first; i < last; i++) {
        for (size_t t = 0; t < m_client_tex_coord_pointer.size(); ++t) {
            if (m_client_side_texture_coord_array_enabled[t]) {
                float tex_coords[4] { 0, 0, 0, 0 };
                read_from_vertex_attribute_pointer(m_client_tex_coord_pointer[t], i, tex_coords, false);
                gl_multi_tex_coord(GL_TEXTURE0 + t, tex_coords[0], tex_coords[1], tex_coords[2], tex_coords[3]);
            }
        }

        if (m_client_side_color_array_enabled) {
            float color[4] { 0, 0, 0, 1 };
            read_from_vertex_attribute_pointer(m_client_color_pointer, i, color, true);
            gl_color(color[0], color[1], color[2], color[3]);
        }

        float vertex[4] { 0, 0, 0, 1 };
        read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex, false);
        gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
    }
    gl_end();
}

void SoftwareGLContext::gl_draw_elements(GLenum mode, GLsizei count, GLenum type, const void* indices)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_draw_elements, mode, count, type, indices);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Some modes are still missing (GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES)
    RETURN_WITH_ERROR_IF(!(mode == GL_TRIANGLE_STRIP
                             || mode == GL_TRIANGLE_FAN
                             || mode == GL_TRIANGLES
                             || mode == GL_QUADS
                             || mode == GL_QUAD_STRIP
                             || mode == GL_POLYGON),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(!(type == GL_UNSIGNED_BYTE
                             || type == GL_UNSIGNED_SHORT
                             || type == GL_UNSIGNED_INT),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(count < 0, GL_INVALID_VALUE);

    // At least the vertex array needs to be enabled
    if (!m_client_side_vertex_array_enabled)
        return;

    gl_begin(mode);
    for (int index = 0; index < count; index++) {
        int i = 0;
        switch (type) {
        case GL_UNSIGNED_BYTE:
            i = reinterpret_cast<const GLubyte*>(indices)[index];
            break;
        case GL_UNSIGNED_SHORT:
            i = reinterpret_cast<const GLushort*>(indices)[index];
            break;
        case GL_UNSIGNED_INT:
            i = reinterpret_cast<const GLuint*>(indices)[index];
            break;
        }

        for (size_t t = 0; t < m_client_tex_coord_pointer.size(); ++t) {
            if (m_client_side_texture_coord_array_enabled[t]) {
                float tex_coords[4] { 0, 0, 0, 0 };
                read_from_vertex_attribute_pointer(m_client_tex_coord_pointer[t], i, tex_coords, false);
                gl_multi_tex_coord(GL_TEXTURE0 + t, tex_coords[0], tex_coords[1], tex_coords[2], tex_coords[3]);
            }
        }

        if (m_client_side_color_array_enabled) {
            float color[4] { 0, 0, 0, 1 };
            read_from_vertex_attribute_pointer(m_client_color_pointer, i, color, true);
            gl_color(color[0], color[1], color[2], color[3]);
        }

        float vertex[4] { 0, 0, 0, 1 };
        read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex, false);
        gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
    }
    gl_end();
}

void SoftwareGLContext::gl_draw_pixels(GLsizei width, GLsizei height, GLenum format, GLenum type, const void* data)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_draw_pixels, width, height, format, type, data);

    RETURN_WITH_ERROR_IF(format < GL_COLOR_INDEX || format > GL_BGRA, GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF((type < GL_BYTE || type > GL_FLOAT)
            && (type < GL_UNSIGNED_BYTE_3_3_2 || type > GL_UNSIGNED_INT_10_10_10_2)
            && (type < GL_UNSIGNED_BYTE_2_3_3_REV || type > GL_UNSIGNED_INT_2_10_10_10_REV),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(type == GL_BITMAP && !(format == GL_COLOR_INDEX || format == GL_STENCIL_INDEX), GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    // FIXME: GL_INVALID_OPERATION is generated if format is GL_STENCIL_INDEX and there is no stencil buffer
    // FIXME: GL_INVALID_OPERATION is generated if format is GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA, GL_RGB, GL_RGBA,
    //        GL_BGR, GL_BGRA, GL_LUMINANCE, or GL_LUMINANCE_ALPHA, and the GL is in color index mode

    RETURN_WITH_ERROR_IF(format != GL_RGB
            && (type == GL_UNSIGNED_BYTE_3_3_2
                || type == GL_UNSIGNED_BYTE_2_3_3_REV
                || type == GL_UNSIGNED_SHORT_5_6_5
                || type == GL_UNSIGNED_SHORT_5_6_5_REV),
        GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(format == GL_RGBA || format == GL_BGRA)
            && (type == GL_UNSIGNED_SHORT_4_4_4_4
                || type == GL_UNSIGNED_SHORT_4_4_4_4_REV
                || type == GL_UNSIGNED_SHORT_5_5_5_1
                || type == GL_UNSIGNED_SHORT_1_5_5_5_REV
                || type == GL_UNSIGNED_INT_8_8_8_8
                || type == GL_UNSIGNED_INT_8_8_8_8_REV
                || type == GL_UNSIGNED_INT_10_10_10_2
                || type == GL_UNSIGNED_INT_2_10_10_10_REV),
        GL_INVALID_OPERATION);

    // FIXME: GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to the GL_PIXEL_UNPACK_BUFFER
    //        target and the buffer object's data store is currently mapped.
    // FIXME: GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to the GL_PIXEL_UNPACK_BUFFER
    //        target and the data would be unpacked from the buffer object such that the memory reads required would
    //        exceed the data store size.
    // FIXME: GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to the GL_PIXEL_UNPACK_BUFFER
    //        target and data is not evenly divisible into the number of bytes needed to store in memory a datum
    //        indicated by type.

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: we only support RGBA + UNSIGNED_BYTE and DEPTH_COMPONENT + UNSIGNED_SHORT, implement all combinations!
    if (!((format == GL_RGBA && type == GL_UNSIGNED_BYTE) || (format == GL_DEPTH_COMPONENT && type == GL_UNSIGNED_SHORT))) {
        dbgln_if(GL_DEBUG, "gl_draw_pixels(): support for format {:#x} and/or type {:#x} not implemented", format, type);
        return;
    }

    // FIXME: implement support for pixel parameters such as GL_UNPACK_ALIGNMENT

    if (format == GL_RGBA) {
        auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { width, height });
        RETURN_WITH_ERROR_IF(bitmap_or_error.is_error(), GL_OUT_OF_MEMORY);
        auto bitmap = bitmap_or_error.release_value();

        auto pixel_data = static_cast<u32 const*>(data);
        for (int y = 0; y < height; ++y)
            for (int x = 0; x < width; ++x)
                bitmap->set_pixel(x, y, Color::from_rgba(*(pixel_data++)));

        m_rasterizer.blit_to_color_buffer_at_raster_position(bitmap);
    } else if (format == GL_DEPTH_COMPONENT) {
        Vector<float> depth_values;
        depth_values.ensure_capacity(width * height);

        auto depth_data = static_cast<u16 const*>(data);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                auto u16_value = *(depth_data++);
                auto float_value = static_cast<float>(u16_value) / NumericLimits<u16>::max();
                depth_values.append(float_value);
            }
        }

        m_rasterizer.blit_to_depth_buffer_at_raster_position(depth_values, width, height);
    } else {
        VERIFY_NOT_REACHED();
    }
}

void SoftwareGLContext::gl_depth_range(GLdouble min, GLdouble max)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_depth_range, min, max);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer.options();
    options.depth_min = clamp<float>(min, 0.f, 1.f);
    options.depth_max = clamp<float>(max, 0.f, 1.f);
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_depth_func(GLenum func)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_depth_func, func);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(!(func == GL_NEVER
                             || func == GL_LESS
                             || func == GL_EQUAL
                             || func == GL_LEQUAL
                             || func == GL_GREATER
                             || func == GL_NOTEQUAL
                             || func == GL_GEQUAL
                             || func == GL_ALWAYS),
        GL_INVALID_ENUM);

    auto options = m_rasterizer.options();

    switch (func) {
    case GL_NEVER:
        options.depth_func = SoftGPU::DepthTestFunction::Never;
        break;
    case GL_ALWAYS:
        options.depth_func = SoftGPU::DepthTestFunction::Always;
        break;
    case GL_LESS:
        options.depth_func = SoftGPU::DepthTestFunction::Less;
        break;
    case GL_LEQUAL:
        options.depth_func = SoftGPU::DepthTestFunction::LessOrEqual;
        break;
    case GL_EQUAL:
        options.depth_func = SoftGPU::DepthTestFunction::Equal;
        break;
    case GL_NOTEQUAL:
        options.depth_func = SoftGPU::DepthTestFunction::NotEqual;
        break;
    case GL_GEQUAL:
        options.depth_func = SoftGPU::DepthTestFunction::GreaterOrEqual;
        break;
    case GL_GREATER:
        options.depth_func = SoftGPU::DepthTestFunction::Greater;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_rasterizer.set_options(options);
}

// General helper function to read arbitrary vertex attribute data into a float array
void SoftwareGLContext::read_from_vertex_attribute_pointer(VertexAttribPointer const& attrib, int index, float* elements, bool normalize)
{
    auto byte_ptr = reinterpret_cast<const char*>(attrib.pointer);
    size_t stride = attrib.stride;

    switch (attrib.type) {
    case GL_BYTE: {
        if (stride == 0)
            stride = sizeof(GLbyte) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLbyte*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0x80;
        }
        break;
    }
    case GL_UNSIGNED_BYTE: {
        if (stride == 0)
            stride = sizeof(GLubyte) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLubyte*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0xff;
        }
        break;
    }
    case GL_SHORT: {
        if (stride == 0)
            stride = sizeof(GLshort) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLshort*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0x8000;
        }
        break;
    }
    case GL_UNSIGNED_SHORT: {
        if (stride == 0)
            stride = sizeof(GLushort) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLushort*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0xffff;
        }
        break;
    }
    case GL_INT: {
        if (stride == 0)
            stride = sizeof(GLint) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLint*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0x80000000;
        }
        break;
    }
    case GL_UNSIGNED_INT: {
        if (stride == 0)
            stride = sizeof(GLuint) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLuint*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0xffffffff;
        }
        break;
    }
    case GL_FLOAT: {
        if (stride == 0)
            stride = sizeof(GLfloat) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<const GLfloat*>(byte_ptr + stride * index) + i);
        }
        break;
    }
    case GL_DOUBLE: {
        if (stride == 0)
            stride = sizeof(GLdouble) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = static_cast<float>(*(reinterpret_cast<const GLdouble*>(byte_ptr + stride * index) + i));
        }
        break;
    }
    }
}

void SoftwareGLContext::gl_color_mask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    auto options = m_rasterizer.options();
    auto mask = options.color_mask;

    if (!red)
        mask &= ~0x000000ff;
    else
        mask |= 0x000000ff;

    if (!green)
        mask &= ~0x0000ff00;
    else
        mask |= 0x0000ff00;

    if (!blue)
        mask &= ~0x00ff0000;
    else
        mask |= 0x00ff0000;

    if (!alpha)
        mask &= ~0xff000000;
    else
        mask |= 0xff000000;

    options.color_mask = mask;
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_polygon_mode(GLenum face, GLenum mode)
{
    RETURN_WITH_ERROR_IF(!(face == GL_BACK || face == GL_FRONT || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(mode == GL_POINT || mode == GL_LINE || mode == GL_FILL), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer.options();

    // FIXME: This must support different polygon modes for front- and backside
    if (face == GL_BACK) {
        dbgln_if(GL_DEBUG, "gl_polygon_mode(GL_BACK, {:#x}): unimplemented", mode);
        return;
    }

    auto map_mode = [](GLenum mode) -> SoftGPU::PolygonMode {
        switch (mode) {
        case GL_FILL:
            return SoftGPU::PolygonMode::Fill;
        case GL_LINE:
            return SoftGPU::PolygonMode::Line;
        case GL_POINT:
            return SoftGPU::PolygonMode::Point;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    options.polygon_mode = map_mode(mode);
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_polygon_offset(GLfloat factor, GLfloat units)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_polygon_offset, factor, units);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer.options();
    rasterizer_options.depth_offset_factor = factor;
    rasterizer_options.depth_offset_constant = units;
    m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_fogfv(GLenum pname, GLfloat* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_fogfv, pname, params);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer.options();

    switch (pname) {
    case GL_FOG_COLOR:
        options.fog_color = { params[0], params[1], params[2], params[3] };
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_fogf(GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_fogf, pname, param);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(param < 0.0f, GL_INVALID_VALUE);

    auto options = m_rasterizer.options();

    switch (pname) {
    case GL_FOG_DENSITY:
        options.fog_density = param;
        break;
    case GL_FOG_END:
        options.fog_end = param;
        break;
    case GL_FOG_START:
        options.fog_start = param;
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_fogi(GLenum pname, GLint param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_fogi, pname, param);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(param != GL_LINEAR && param != GL_EXP && param != GL_EXP2, GL_INVALID_ENUM);

    auto options = m_rasterizer.options();

    switch (pname) {
    case GL_FOG_MODE:
        switch (param) {
        case GL_LINEAR:
            options.fog_mode = SoftGPU::FogMode::Linear;
            break;
        case GL_EXP:
            options.fog_mode = SoftGPU::FogMode::Exp;
            break;
        case GL_EXP2:
            options.fog_mode = SoftGPU::FogMode::Exp2;
            break;
        }
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_pixel_storei(GLenum pname, GLint param)
{
    // FIXME: Implement missing parameters
    switch (pname) {
    case GL_PACK_ALIGNMENT:
        RETURN_WITH_ERROR_IF(param != 1 && param != 2 && param != 4 && param != 8, GL_INVALID_VALUE);
        m_pack_alignment = param;
        break;
    case GL_UNPACK_ROW_LENGTH:
        RETURN_WITH_ERROR_IF(param < 0, GL_INVALID_VALUE);
        m_unpack_row_length = static_cast<size_t>(param);
        break;
    case GL_UNPACK_ALIGNMENT:
        RETURN_WITH_ERROR_IF(param != 1 && param != 2 && param != 4 && param != 8, GL_INVALID_VALUE);
        m_unpack_alignment = param;
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
        break;
    }
}

void SoftwareGLContext::gl_scissor(GLint x, GLint y, GLsizei width, GLsizei height)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_scissor, x, y, width, height);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    auto options = m_rasterizer.options();
    options.scissor_box = { x, y, width, height };
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_stencil_func_separate(GLenum face, GLenum func, GLint ref, GLuint mask)
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

void SoftwareGLContext::gl_stencil_mask_separate(GLenum face, GLuint mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_stencil_mask_separate, face, mask);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (face == GL_FRONT || face == GL_FRONT_AND_BACK)
        m_stencil_operation[Face::Front].write_mask = mask;
    if (face == GL_BACK || face == GL_FRONT_AND_BACK)
        m_stencil_operation[Face::Back].write_mask = mask;

    m_stencil_configuration_dirty = true;
}

void SoftwareGLContext::gl_stencil_op_separate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass)
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

void SoftwareGLContext::gl_normal(GLfloat nx, GLfloat ny, GLfloat nz)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_normal, nx, ny, nz);

    m_current_vertex_normal = { nx, ny, nz };
}

void SoftwareGLContext::gl_normal_pointer(GLenum type, GLsizei stride, void const* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(type != GL_BYTE
            && type != GL_SHORT
            && type != GL_INT
            && type != GL_FLOAT
            && type != GL_DOUBLE,
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    dbgln_if(GL_DEBUG, "gl_normal_pointer({:#x}, {}, {:p}): unimplemented", type, stride, pointer);
}

void SoftwareGLContext::gl_raster_pos(GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_raster_pos, x, y, z, w);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_rasterizer.set_raster_position({ x, y, z, w }, m_model_view_matrix, m_projection_matrix);
}

void SoftwareGLContext::gl_line_width(GLfloat width)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_line_width, width);

    RETURN_WITH_ERROR_IF(width <= 0, GL_INVALID_VALUE);

    m_line_width = width;
}

void SoftwareGLContext::gl_push_attrib(GLbitfield mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_push_attrib, mask);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: implement
    dbgln_if(GL_DEBUG, "SoftwareGLContext FIXME: implement gl_push_attrib({})", mask);
}

void SoftwareGLContext::gl_pop_attrib()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_pop_attrib);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: implement
    dbgln_if(GL_DEBUG, "SoftwareGLContext FIXME: implement gl_pop_attrib()");
}

void SoftwareGLContext::gl_light_model(GLenum pname, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_light_model, pname, x, y, z, w);

    RETURN_WITH_ERROR_IF(!(pname == GL_LIGHT_MODEL_AMBIENT
                             || pname == GL_LIGHT_MODEL_TWO_SIDE),
        GL_INVALID_ENUM);

    auto lighting_params = m_rasterizer.light_model();
    bool update_lighting_model = false;

    switch (pname) {
    case GL_LIGHT_MODEL_AMBIENT:
        lighting_params.scene_ambient_color = { x, y, z, w };
        update_lighting_model = true;
        break;
    case GL_LIGHT_MODEL_TWO_SIDE:
        VERIFY(y == 0.0f && z == 0.0f && w == 0.0f);
        lighting_params.two_sided_lighting = x;
        update_lighting_model = true;
        break;
    case GL_LIGHT_MODEL_LOCAL_VIEWER:
        // 0 means the viewer is at infinity
        // 1 means they're in local (eye) space
        lighting_params.viewer_at_infinity = (x != 1.0f);
        update_lighting_model = true;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    if (update_lighting_model)
        m_rasterizer.set_light_model_params(lighting_params);
}

void SoftwareGLContext::gl_bitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_bitmap, width, height, xorig, yorig, xmove, ymove, bitmap);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (bitmap != nullptr) {
        // FIXME: implement
        dbgln_if(GL_DEBUG, "gl_bitmap({}, {}, {}, {}, {}, {}, {}): unimplemented", width, height, xorig, yorig, xmove, ymove, bitmap);
    }

    auto raster_position = m_rasterizer.raster_position();
    raster_position.window_coordinates += { xmove, ymove, 0.f, 0.f };
    m_rasterizer.set_raster_position(raster_position);
}

void SoftwareGLContext::gl_copy_tex_image_2d(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_copy_tex_image_2d, target, level, internalformat, x, y, width, height, border);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: implement
    dbgln_if(GL_DEBUG, "SoftwareGLContext FIXME: implement gl_copy_tex_image_2d({:#x}, {}, {:#x}, {}, {}, {}, {}, {})",
        target, level, internalformat, x, y, width, height, border);
}

void SoftwareGLContext::gl_get_tex_parameter_integerv(GLenum target, GLint level, GLenum pname, GLint* params)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    // FIXME: support targets other than GL_TEXTURE_2D
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);
    // FIXME: support other parameter names
    RETURN_WITH_ERROR_IF(pname < GL_TEXTURE_WIDTH || pname > GL_TEXTURE_HEIGHT, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(level < 0 || level > Texture2D::LOG2_MAX_TEXTURE_SIZE, GL_INVALID_VALUE);
    // FIXME: GL_INVALID_VALUE is generated if target is GL_TEXTURE_BUFFER and level is not zero
    // FIXME: GL_INVALID_OPERATION is generated if GL_TEXTURE_COMPRESSED_IMAGE_SIZE is queried on texture images with an uncompressed internal format or on proxy targets

    switch (pname) {
    case GL_TEXTURE_HEIGHT:
        *params = m_active_texture_unit->bound_texture_2d()->height_at_lod(level);
        break;
    case GL_TEXTURE_WIDTH:
        *params = m_active_texture_unit->bound_texture_2d()->width_at_lod(level);
        break;
    }
}

void SoftwareGLContext::gl_rect(GLdouble x1, GLdouble y1, GLdouble x2, GLdouble y2)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_rect, x1, y1, x2, y2);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    gl_begin(GL_POLYGON);
    gl_vertex(x1, y1, 0.0, 0.0);
    gl_vertex(x2, y1, 0.0, 0.0);
    gl_vertex(x2, y2, 0.0, 0.0);
    gl_vertex(x1, y2, 0.0, 0.0);
    gl_end();
}

void SoftwareGLContext::gl_tex_gen(GLenum coord, GLenum pname, GLint param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_tex_gen, coord, pname, param);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(coord < GL_S || coord > GL_Q, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(pname != GL_TEXTURE_GEN_MODE, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(param != GL_EYE_LINEAR
            && param != GL_OBJECT_LINEAR
            && param != GL_SPHERE_MAP
            && param != GL_NORMAL_MAP
            && param != GL_REFLECTION_MAP,
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF((coord == GL_R || coord == GL_Q) && param == GL_SPHERE_MAP, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(coord == GL_Q && (param == GL_REFLECTION_MAP || param == GL_NORMAL_MAP), GL_INVALID_ENUM);

    GLenum const capability = GL_TEXTURE_GEN_S + (coord - GL_S);
    texture_coordinate_generation(m_active_texture_unit_index, capability).generation_mode = param;
    m_texcoord_generation_dirty = true;
}

void SoftwareGLContext::gl_tex_gen_floatv(GLenum coord, GLenum pname, GLfloat const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_tex_gen_floatv, coord, pname, params);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(coord < GL_S || coord > GL_Q, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(pname != GL_TEXTURE_GEN_MODE
            && pname != GL_OBJECT_PLANE
            && pname != GL_EYE_PLANE,
        GL_INVALID_ENUM);

    GLenum const capability = GL_TEXTURE_GEN_S + (coord - GL_S);

    switch (pname) {
    case GL_TEXTURE_GEN_MODE: {
        auto param = static_cast<GLenum>(params[0]);
        RETURN_WITH_ERROR_IF(param != GL_EYE_LINEAR
                && param != GL_OBJECT_LINEAR
                && param != GL_SPHERE_MAP
                && param != GL_NORMAL_MAP
                && param != GL_REFLECTION_MAP,
            GL_INVALID_ENUM);
        RETURN_WITH_ERROR_IF((coord == GL_R || coord == GL_Q) && param == GL_SPHERE_MAP, GL_INVALID_ENUM);
        RETURN_WITH_ERROR_IF(coord == GL_Q && (param == GL_REFLECTION_MAP || param == GL_NORMAL_MAP), GL_INVALID_ENUM);

        texture_coordinate_generation(m_active_texture_unit_index, capability).generation_mode = param;
        break;
    }
    case GL_OBJECT_PLANE:
        texture_coordinate_generation(m_active_texture_unit_index, capability).object_plane_coefficients = { params[0], params[1], params[2], params[3] };
        break;
    case GL_EYE_PLANE: {
        auto const& inverse_model_view = m_model_view_matrix.inverse();
        auto input_coefficients = FloatVector4 { params[0], params[1], params[2], params[3] };

        // Note: we are allowed to store transformed coefficients here, according to the documentation on
        //       `glGetTexGen`:
        //
        // "The returned values are those maintained in eye coordinates. They are not equal to the values
        //  specified using glTexGen, unless the modelview matrix was identity when glTexGen was called."

        texture_coordinate_generation(m_active_texture_unit_index, capability).eye_plane_coefficients = inverse_model_view * input_coefficients;
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    m_texcoord_generation_dirty = true;
}

void SoftwareGLContext::present()
{
    m_rasterizer.blit_color_buffer_to(*m_frontbuffer);
}

void SoftwareGLContext::sync_device_config()
{
    sync_device_sampler_config();
    sync_device_texcoord_config();
    sync_light_state();
    sync_stencil_configuration();
}

void SoftwareGLContext::sync_device_sampler_config()
{
    if (!m_sampler_config_is_dirty)
        return;

    m_sampler_config_is_dirty = false;

    for (unsigned i = 0; i < m_texture_units.size(); ++i) {
        SoftGPU::SamplerConfig config;

        if (!m_texture_units[i].texture_2d_enabled())
            continue;

        auto texture = m_texture_units[i].bound_texture_2d();

        config.bound_image = texture.is_null() ? nullptr : texture->device_image();

        auto const& sampler = texture->sampler();

        switch (sampler.min_filter()) {
        case GL_NEAREST:
            config.texture_min_filter = SoftGPU::TextureFilter::Nearest;
            config.mipmap_filter = SoftGPU::MipMapFilter::None;
            break;
        case GL_LINEAR:
            config.texture_min_filter = SoftGPU::TextureFilter::Linear;
            config.mipmap_filter = SoftGPU::MipMapFilter::None;
            break;
        case GL_NEAREST_MIPMAP_NEAREST:
            config.texture_min_filter = SoftGPU::TextureFilter::Nearest;
            config.mipmap_filter = SoftGPU::MipMapFilter::Nearest;
            break;
        case GL_LINEAR_MIPMAP_NEAREST:
            config.texture_min_filter = SoftGPU::TextureFilter::Nearest;
            config.mipmap_filter = SoftGPU::MipMapFilter::Linear;
            break;
        case GL_NEAREST_MIPMAP_LINEAR:
            config.texture_min_filter = SoftGPU::TextureFilter::Linear;
            config.mipmap_filter = SoftGPU::MipMapFilter::Nearest;
            break;
        case GL_LINEAR_MIPMAP_LINEAR:
            config.texture_min_filter = SoftGPU::TextureFilter::Linear;
            config.mipmap_filter = SoftGPU::MipMapFilter::Linear;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        switch (sampler.mag_filter()) {
        case GL_NEAREST:
            config.texture_mag_filter = SoftGPU::TextureFilter::Nearest;
            break;
        case GL_LINEAR:
            config.texture_mag_filter = SoftGPU::TextureFilter::Linear;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        switch (sampler.wrap_s_mode()) {
        case GL_CLAMP:
            config.texture_wrap_u = SoftGPU::TextureWrapMode::Clamp;
            break;
        case GL_CLAMP_TO_BORDER:
            config.texture_wrap_u = SoftGPU::TextureWrapMode::ClampToBorder;
            break;
        case GL_CLAMP_TO_EDGE:
            config.texture_wrap_u = SoftGPU::TextureWrapMode::ClampToEdge;
            break;
        case GL_REPEAT:
            config.texture_wrap_u = SoftGPU::TextureWrapMode::Repeat;
            break;
        case GL_MIRRORED_REPEAT:
            config.texture_wrap_u = SoftGPU::TextureWrapMode::MirroredRepeat;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        switch (sampler.wrap_t_mode()) {
        case GL_CLAMP:
            config.texture_wrap_v = SoftGPU::TextureWrapMode::Clamp;
            break;
        case GL_CLAMP_TO_BORDER:
            config.texture_wrap_v = SoftGPU::TextureWrapMode::ClampToBorder;
            break;
        case GL_CLAMP_TO_EDGE:
            config.texture_wrap_v = SoftGPU::TextureWrapMode::ClampToEdge;
            break;
        case GL_REPEAT:
            config.texture_wrap_v = SoftGPU::TextureWrapMode::Repeat;
            break;
        case GL_MIRRORED_REPEAT:
            config.texture_wrap_v = SoftGPU::TextureWrapMode::MirroredRepeat;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        switch (m_texture_units[i].env_mode()) {
        case GL_MODULATE:
            config.fixed_function_texture_env_mode = SoftGPU::TextureEnvMode::Modulate;
            break;
        case GL_REPLACE:
            config.fixed_function_texture_env_mode = SoftGPU::TextureEnvMode::Replace;
            break;
        case GL_DECAL:
            config.fixed_function_texture_env_mode = SoftGPU::TextureEnvMode::Decal;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        m_rasterizer.set_sampler_config(i, config);
    }
}

void SoftwareGLContext::sync_light_state()
{
    if (!m_light_state_is_dirty)
        return;

    m_light_state_is_dirty = false;

    auto options = m_rasterizer.options();
    options.color_material_enabled = m_color_material_enabled;
    switch (m_color_material_face) {
    case GL_BACK:
        options.color_material_face = SoftGPU::ColorMaterialFace::Back;
        break;
    case GL_FRONT:
        options.color_material_face = SoftGPU::ColorMaterialFace::Front;
        break;
    case GL_FRONT_AND_BACK:
        options.color_material_face = SoftGPU::ColorMaterialFace::FrontAndBack;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    switch (m_color_material_mode) {
    case GL_AMBIENT:
        options.color_material_mode = SoftGPU::ColorMaterialMode::Ambient;
        break;
    case GL_AMBIENT_AND_DIFFUSE:
        options.color_material_mode = SoftGPU::ColorMaterialMode::Ambient;
        options.color_material_mode = SoftGPU::ColorMaterialMode::Diffuse;
        break;
    case GL_DIFFUSE:
        options.color_material_mode = SoftGPU::ColorMaterialMode::Diffuse;
        break;
    case GL_EMISSION:
        options.color_material_mode = SoftGPU::ColorMaterialMode::Emissive;
        break;
    case GL_SPECULAR:
        options.color_material_mode = SoftGPU::ColorMaterialMode::Specular;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    m_rasterizer.set_options(options);

    for (auto light_id = 0u; light_id < SoftGPU::NUM_LIGHTS; light_id++) {
        auto const& current_light_state = m_light_states.at(light_id);
        m_rasterizer.set_light_state(light_id, current_light_state);
    }

    m_rasterizer.set_material_state(SoftGPU::Face::Front, m_material_states[Face::Front]);
    m_rasterizer.set_material_state(SoftGPU::Face::Back, m_material_states[Face::Back]);
}

void SoftwareGLContext::sync_device_texcoord_config()
{
    if (!m_texcoord_generation_dirty)
        return;
    m_texcoord_generation_dirty = false;

    auto options = m_rasterizer.options();

    for (size_t i = 0; i < m_device_info.num_texture_units; ++i) {

        u8 enabled_coordinates = SoftGPU::TexCoordGenerationCoordinate::None;
        for (GLenum capability = GL_TEXTURE_GEN_S; capability <= GL_TEXTURE_GEN_Q; ++capability) {
            auto const context_coordinate_config = texture_coordinate_generation(i, capability);
            if (!context_coordinate_config.enabled)
                continue;

            SoftGPU::TexCoordGenerationConfig* texcoord_generation_config;
            switch (capability) {
            case GL_TEXTURE_GEN_S:
                enabled_coordinates |= SoftGPU::TexCoordGenerationCoordinate::S;
                texcoord_generation_config = &options.texcoord_generation_config[i][0];
                break;
            case GL_TEXTURE_GEN_T:
                enabled_coordinates |= SoftGPU::TexCoordGenerationCoordinate::T;
                texcoord_generation_config = &options.texcoord_generation_config[i][1];
                break;
            case GL_TEXTURE_GEN_R:
                enabled_coordinates |= SoftGPU::TexCoordGenerationCoordinate::R;
                texcoord_generation_config = &options.texcoord_generation_config[i][2];
                break;
            case GL_TEXTURE_GEN_Q:
                enabled_coordinates |= SoftGPU::TexCoordGenerationCoordinate::Q;
                texcoord_generation_config = &options.texcoord_generation_config[i][3];
                break;
            default:
                VERIFY_NOT_REACHED();
            }

            switch (context_coordinate_config.generation_mode) {
            case GL_OBJECT_LINEAR:
                texcoord_generation_config->mode = SoftGPU::TexCoordGenerationMode::ObjectLinear;
                texcoord_generation_config->coefficients = context_coordinate_config.object_plane_coefficients;
                break;
            case GL_EYE_LINEAR:
                texcoord_generation_config->mode = SoftGPU::TexCoordGenerationMode::EyeLinear;
                texcoord_generation_config->coefficients = context_coordinate_config.eye_plane_coefficients;
                break;
            case GL_SPHERE_MAP:
                texcoord_generation_config->mode = SoftGPU::TexCoordGenerationMode::SphereMap;
                break;
            case GL_REFLECTION_MAP:
                texcoord_generation_config->mode = SoftGPU::TexCoordGenerationMode::ReflectionMap;
                break;
            case GL_NORMAL_MAP:
                texcoord_generation_config->mode = SoftGPU::TexCoordGenerationMode::NormalMap;
                break;
            }
        }
        options.texcoord_generation_enabled_coordinates[i] = enabled_coordinates;
    }

    m_rasterizer.set_options(options);
}

void SoftwareGLContext::sync_stencil_configuration()
{
    if (!m_stencil_configuration_dirty)
        return;
    m_stencil_configuration_dirty = false;

    auto set_device_stencil = [&](SoftGPU::Face face, StencilFunctionOptions func, StencilOperationOptions op) {
        SoftGPU::StencilConfiguration device_configuration;

        // Stencil test function
        auto map_func = [](GLenum func) -> SoftGPU::StencilTestFunction {
            switch (func) {
            case GL_ALWAYS:
                return SoftGPU::StencilTestFunction::Always;
            case GL_EQUAL:
                return SoftGPU::StencilTestFunction::Equal;
            case GL_GEQUAL:
                return SoftGPU::StencilTestFunction::GreaterOrEqual;
            case GL_GREATER:
                return SoftGPU::StencilTestFunction::Greater;
            case GL_LESS:
                return SoftGPU::StencilTestFunction::Less;
            case GL_LEQUAL:
                return SoftGPU::StencilTestFunction::LessOrEqual;
            case GL_NEVER:
                return SoftGPU::StencilTestFunction::Never;
            case GL_NOTEQUAL:
                return SoftGPU::StencilTestFunction::NotEqual;
            }
            VERIFY_NOT_REACHED();
        };
        device_configuration.test_function = map_func(func.func);
        device_configuration.reference_value = func.reference_value;
        device_configuration.test_mask = func.mask;

        // Stencil operation
        auto map_operation = [](GLenum operation) -> SoftGPU::StencilOperation {
            switch (operation) {
            case GL_DECR:
                return SoftGPU::StencilOperation::Decrement;
            case GL_DECR_WRAP:
                return SoftGPU::StencilOperation::DecrementWrap;
            case GL_INCR:
                return SoftGPU::StencilOperation::Increment;
            case GL_INCR_WRAP:
                return SoftGPU::StencilOperation::IncrementWrap;
            case GL_INVERT:
                return SoftGPU::StencilOperation::Invert;
            case GL_KEEP:
                return SoftGPU::StencilOperation::Keep;
            case GL_REPLACE:
                return SoftGPU::StencilOperation::Replace;
            case GL_ZERO:
                return SoftGPU::StencilOperation::Zero;
            }
            VERIFY_NOT_REACHED();
        };
        device_configuration.on_stencil_test_fail = map_operation(op.op_fail);
        device_configuration.on_depth_test_fail = map_operation(op.op_depth_fail);
        device_configuration.on_pass = map_operation(op.op_pass);
        device_configuration.write_mask = op.write_mask;

        m_rasterizer.set_stencil_configuration(face, device_configuration);
    };
    set_device_stencil(SoftGPU::Face::Front, m_stencil_function[Face::Front], m_stencil_operation[Face::Front]);
    set_device_stencil(SoftGPU::Face::Back, m_stencil_function[Face::Back], m_stencil_operation[Face::Back]);
}

void SoftwareGLContext::build_extension_string()
{
    Vector<StringView> extensions;

    // FIXME: npot texture support became a required core feature starting with OpenGL 2.0 (https://www.khronos.org/opengl/wiki/NPOT_Texture)
    // Ideally we would verify if the selected device adheres to the requested OpenGL context version before context creation
    // and refuse to create a context if it doesn't.
    if (m_device_info.supports_npot_textures)
        extensions.append("GL_ARB_texture_non_power_of_two");

    if (m_device_info.num_texture_units > 1)
        extensions.append("GL_ARB_multitexture");

    m_extensions = String::join(" ", extensions);
}

void SoftwareGLContext::gl_lightf(GLenum light, GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_lightf, light, pname, param);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(light < GL_LIGHT0 || light >= (GL_LIGHT0 + m_device_info.num_lights), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_CONSTANT_ATTENUATION || pname == GL_LINEAR_ATTENUATION || pname == GL_QUADRATIC_ATTENUATION || pname != GL_SPOT_EXPONENT || pname != GL_SPOT_CUTOFF), GL_INVALID_ENUM);

    auto& light_state = m_light_states.at(light - GL_LIGHT0);

    switch (pname) {
    case GL_CONSTANT_ATTENUATION:
        light_state.constant_attenuation = param;
        break;
    case GL_LINEAR_ATTENUATION:
        light_state.linear_attenuation = param;
        break;
    case GL_QUADRATIC_ATTENUATION:
        light_state.quadratic_attenuation = param;
        break;
    case GL_SPOT_EXPONENT:
        light_state.spotlight_exponent = param;
        break;
    case GL_SPOT_CUTOFF:
        light_state.spotlight_cutoff_angle = param;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_lightfv(GLenum light, GLenum pname, GLfloat const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_lightfv, light, pname, params);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(light < GL_LIGHT0 || light >= (GL_LIGHT0 + m_device_info.num_lights), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_POSITION || pname == GL_CONSTANT_ATTENUATION || pname == GL_LINEAR_ATTENUATION || pname == GL_QUADRATIC_ATTENUATION || pname == GL_SPOT_CUTOFF || pname == GL_SPOT_EXPONENT || pname == GL_SPOT_DIRECTION), GL_INVALID_ENUM);

    auto& light_state = m_light_states.at(light - GL_LIGHT0);

    switch (pname) {
    case GL_AMBIENT:
        light_state.ambient_intensity = { params[0], params[1], params[2], params[3] };
        break;
    case GL_DIFFUSE:
        light_state.diffuse_intensity = { params[0], params[1], params[2], params[3] };
        break;
    case GL_SPECULAR:
        light_state.specular_intensity = { params[0], params[1], params[2], params[3] };
        break;
    case GL_POSITION:
        light_state.position = { params[0], params[1], params[2], params[3] };
        light_state.position = m_model_view_matrix * light_state.position;
        break;
    case GL_CONSTANT_ATTENUATION:
        light_state.constant_attenuation = *params;
        break;
    case GL_LINEAR_ATTENUATION:
        light_state.linear_attenuation = *params;
        break;
    case GL_QUADRATIC_ATTENUATION:
        light_state.quadratic_attenuation = *params;
        break;
    case GL_SPOT_EXPONENT:
        light_state.spotlight_exponent = *params;
        break;
    case GL_SPOT_CUTOFF:
        light_state.spotlight_cutoff_angle = *params;
        break;
    case GL_SPOT_DIRECTION: {
        FloatVector4 direction_vector = { params[0], params[1], params[2], 0.0f };
        direction_vector = m_model_view_matrix * direction_vector;
        light_state.spotlight_direction = { direction_vector.x(), direction_vector.y(), direction_vector.z() };
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_lightiv(GLenum light, GLenum pname, GLint const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_lightiv, light, pname, params);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(light < GL_LIGHT0 || light >= (GL_LIGHT0 + m_device_info.num_lights), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_POSITION || pname == GL_CONSTANT_ATTENUATION || pname == GL_LINEAR_ATTENUATION || pname == GL_QUADRATIC_ATTENUATION || pname == GL_SPOT_CUTOFF || pname == GL_SPOT_EXPONENT || pname == GL_SPOT_DIRECTION), GL_INVALID_ENUM);

    auto& light_state = m_light_states[light - GL_LIGHT0];

    auto const to_float_vector = [](GLfloat x, GLfloat y, GLfloat z, GLfloat w) {
        return FloatVector4(x, y, z, w);
    };

    switch (pname) {
    case GL_AMBIENT:
        light_state.ambient_intensity = to_float_vector(params[0], params[1], params[2], params[3]);
        break;
    case GL_DIFFUSE:
        light_state.diffuse_intensity = to_float_vector(params[0], params[1], params[2], params[3]);
        break;
    case GL_SPECULAR:
        light_state.specular_intensity = to_float_vector(params[0], params[1], params[2], params[3]);
        break;
    case GL_POSITION:
        light_state.position = to_float_vector(params[0], params[1], params[2], params[3]);
        light_state.position = m_model_view_matrix * light_state.position;
        break;
    case GL_CONSTANT_ATTENUATION:
        light_state.constant_attenuation = static_cast<float>(params[0]);
        break;
    case GL_LINEAR_ATTENUATION:
        light_state.linear_attenuation = static_cast<float>(params[0]);
        break;
    case GL_QUADRATIC_ATTENUATION:
        light_state.quadratic_attenuation = static_cast<float>(params[0]);
        break;
    case GL_SPOT_EXPONENT:
        light_state.spotlight_exponent = static_cast<float>(params[0]);
        break;
    case GL_SPOT_CUTOFF:
        light_state.spotlight_cutoff_angle = static_cast<float>(params[0]);
        break;
    case GL_SPOT_DIRECTION: {
        FloatVector4 direction_vector = to_float_vector(params[0], params[1], params[2], 0.0f);
        direction_vector = m_model_view_matrix * direction_vector;
        light_state.spotlight_direction = direction_vector.xyz();
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_materialf(GLenum face, GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_materialf, face, pname, param);
    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(pname != GL_SHININESS, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(param > 128.0f, GL_INVALID_VALUE);

    switch (face) {
    case GL_FRONT:
        m_material_states[Face::Front].shininess = param;
        break;
    case GL_BACK:
        m_material_states[Face::Back].shininess = param;
        break;
    case GL_FRONT_AND_BACK:
        m_material_states[Face::Front].shininess = param;
        m_material_states[Face::Back].shininess = param;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_materialfv(GLenum face, GLenum pname, GLfloat const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_materialfv, face, pname, params);
    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_EMISSION || pname == GL_SHININESS || pname == GL_AMBIENT_AND_DIFFUSE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF((pname == GL_SHININESS && *params > 128.0f), GL_INVALID_VALUE);

    auto update_material = [](SoftGPU::Material& material, GLenum pname, GLfloat const* params) {
        switch (pname) {
        case GL_AMBIENT:
            material.ambient = { params[0], params[1], params[2], params[3] };
            break;
        case GL_DIFFUSE:
            material.diffuse = { params[0], params[1], params[2], params[3] };
            break;
        case GL_SPECULAR:
            material.specular = { params[0], params[1], params[2], params[3] };
            break;
        case GL_EMISSION:
            material.emissive = { params[0], params[1], params[2], params[3] };
            break;
        case GL_SHININESS:
            material.shininess = *params;
            break;
        case GL_AMBIENT_AND_DIFFUSE:
            material.ambient = { params[0], params[1], params[2], params[3] };
            material.diffuse = { params[0], params[1], params[2], params[3] };
            break;
        }
    };

    switch (face) {
    case GL_FRONT:
        update_material(m_material_states[Face::Front], pname, params);
        break;
    case GL_BACK:
        update_material(m_material_states[Face::Back], pname, params);
        break;
    case GL_FRONT_AND_BACK:
        update_material(m_material_states[Face::Front], pname, params);
        update_material(m_material_states[Face::Back], pname, params);
        break;
    }

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_materialiv(GLenum face, GLenum pname, GLint const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_materialiv, face, pname, params);
    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_EMISSION || pname == GL_SHININESS || pname == GL_AMBIENT_AND_DIFFUSE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF((pname == GL_SHININESS && *params > 128), GL_INVALID_VALUE);

    auto update_material = [](SoftGPU::Material& material, GLenum pname, GLint const* params) {
        switch (pname) {
        case GL_AMBIENT:
            material.ambient = { static_cast<float>(params[0]), static_cast<float>(params[1]), static_cast<float>(params[2]), static_cast<float>(params[3]) };
            break;
        case GL_DIFFUSE:
            material.diffuse = { static_cast<float>(params[0]), static_cast<float>(params[1]), static_cast<float>(params[2]), static_cast<float>(params[3]) };
            break;
        case GL_SPECULAR:
            material.specular = { static_cast<float>(params[0]), static_cast<float>(params[1]), static_cast<float>(params[2]), static_cast<float>(params[3]) };
            break;
        case GL_EMISSION:
            material.emissive = { static_cast<float>(params[0]), static_cast<float>(params[1]), static_cast<float>(params[2]), static_cast<float>(params[3]) };
            break;
        case GL_SHININESS:
            material.shininess = static_cast<float>(params[0]);
            break;
        case GL_AMBIENT_AND_DIFFUSE:
            material.ambient = { static_cast<float>(params[0]), static_cast<float>(params[1]), static_cast<float>(params[2]), static_cast<float>(params[3]) };
            material.diffuse = { static_cast<float>(params[0]), static_cast<float>(params[1]), static_cast<float>(params[2]), static_cast<float>(params[3]) };
            break;
        }
    };

    switch (face) {
    case GL_FRONT:
        update_material(m_material_states[Face::Front], pname, params);
        break;
    case GL_BACK:
        update_material(m_material_states[Face::Back], pname, params);
        break;
    case GL_FRONT_AND_BACK:
        update_material(m_material_states[Face::Front], pname, params);
        update_material(m_material_states[Face::Back], pname, params);
        break;
    }

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_color_material(GLenum face, GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_color_material, face, mode);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(face != GL_FRONT
            && face != GL_BACK
            && face != GL_FRONT_AND_BACK,
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(mode != GL_EMISSION
            && mode != GL_AMBIENT
            && mode != GL_DIFFUSE
            && mode != GL_SPECULAR
            && mode != GL_AMBIENT_AND_DIFFUSE,
        GL_INVALID_ENUM);

    m_color_material_face = face;
    m_color_material_mode = mode;

    m_light_state_is_dirty = true;
}

void SoftwareGLContext::gl_get_light(GLenum light, GLenum pname, void* params, GLenum type)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_get_light, light, pname, params, type);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(light < GL_LIGHT0 || light > GL_LIGHT0 + m_device_info.num_lights, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_SPOT_DIRECTION || pname == GL_SPOT_EXPONENT || pname == GL_SPOT_CUTOFF || pname == GL_CONSTANT_ATTENUATION || pname == GL_LINEAR_ATTENUATION || pname == GL_QUADRATIC_ATTENUATION), GL_INVALID_ENUM);

    if (type == GL_FLOAT)
        get_light_param<GLfloat>(light, pname, static_cast<GLfloat*>(params));
    else if (type == GL_INT)
        get_light_param<GLint>(light, pname, static_cast<GLint*>(params));
    else
        VERIFY_NOT_REACHED();
}

template<typename T>
void SoftwareGLContext::get_light_param(GLenum light, GLenum pname, T* params)
{
    auto const& light_state = m_light_states[light - GL_LIGHT0];
    switch (pname) {
    case GL_AMBIENT:
        params[0] = light_state.ambient_intensity.x();
        params[1] = light_state.ambient_intensity.y();
        params[2] = light_state.ambient_intensity.z();
        params[3] = light_state.ambient_intensity.w();
        break;
    case GL_DIFFUSE:
        params[0] = light_state.diffuse_intensity.x();
        params[1] = light_state.diffuse_intensity.y();
        params[2] = light_state.diffuse_intensity.z();
        params[3] = light_state.diffuse_intensity.w();
        break;
    case GL_SPECULAR:
        params[0] = light_state.specular_intensity.x();
        params[1] = light_state.specular_intensity.y();
        params[2] = light_state.specular_intensity.z();
        params[3] = light_state.specular_intensity.w();
        break;
    case GL_SPOT_DIRECTION:
        params[0] = light_state.spotlight_direction.x();
        params[1] = light_state.spotlight_direction.y();
        params[2] = light_state.spotlight_direction.z();
        break;
    case GL_SPOT_EXPONENT:
        *params = light_state.spotlight_exponent;
        break;
    case GL_SPOT_CUTOFF:
        *params = light_state.spotlight_cutoff_angle;
        break;
    case GL_CONSTANT_ATTENUATION:
        *params = light_state.constant_attenuation;
        break;
    case GL_LINEAR_ATTENUATION:
        *params = light_state.linear_attenuation;
        break;
    case GL_QUADRATIC_ATTENUATION:
        *params = light_state.quadratic_attenuation;
        break;
    }
}

void SoftwareGLContext::gl_get_material(GLenum face, GLenum pname, void* params, GLenum type)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_get_material, face, pname, params, type);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_EMISSION), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK), GL_INVALID_ENUM);

    Face material_face = Front;
    switch (face) {
    case GL_FRONT:
        material_face = Front;
        break;
    case GL_BACK:
        material_face = Back;
        break;
    }

    if (type == GL_FLOAT)
        get_material_param<GLfloat>(material_face, pname, static_cast<GLfloat*>(params));
    else if (type == GL_INT)
        get_material_param<GLint>(material_face, pname, static_cast<GLint*>(params));
    else
        VERIFY_NOT_REACHED();
}

template<typename T>
void SoftwareGLContext::get_material_param(Face face, GLenum pname, T* params)
{
    auto const& material = m_material_states[face];
    switch (pname) {
    case GL_AMBIENT:
        params[0] = static_cast<T>(material.ambient.x());
        params[1] = static_cast<T>(material.ambient.y());
        params[2] = static_cast<T>(material.ambient.z());
        params[3] = static_cast<T>(material.ambient.w());
        break;
    case GL_DIFFUSE:
        params[0] = static_cast<T>(material.diffuse.x());
        params[1] = static_cast<T>(material.diffuse.y());
        params[2] = static_cast<T>(material.diffuse.z());
        params[3] = static_cast<T>(material.diffuse.w());
        break;
    case GL_SPECULAR:
        params[0] = static_cast<T>(material.specular.x());
        params[1] = static_cast<T>(material.specular.y());
        params[2] = static_cast<T>(material.specular.z());
        params[3] = static_cast<T>(material.specular.w());
        break;
    case GL_EMISSION:
        params[0] = static_cast<T>(material.emissive.x());
        params[1] = static_cast<T>(material.emissive.y());
        params[2] = static_cast<T>(material.emissive.z());
        params[3] = static_cast<T>(material.emissive.w());
        break;
    case GL_SHININESS:
        *params = material.shininess;
        break;
    }
}

}
