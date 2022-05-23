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
#include <AK/Vector.h>
#include <LibGL/GLContext.h>
#include <LibGPU/Device.h>
#include <LibGPU/Enums.h>
#include <LibGPU/ImageFormat.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Vector3.h>

__attribute__((visibility("hidden"))) GL::GLContext* g_gl_context;

namespace GL {

GLContext::GLContext(RefPtr<GPU::Driver> driver, NonnullOwnPtr<GPU::Device> device, Gfx::Bitmap& frontbuffer)
    : m_viewport { frontbuffer.rect() }
    , m_frontbuffer { frontbuffer }
    , m_driver { driver }
    , m_rasterizer { move(device) }
    , m_device_info { m_rasterizer->info() }
{
    m_texture_units.resize(m_device_info.num_texture_units);
    m_active_texture_unit = &m_texture_units[0];

    // All texture units are initialized with default textures for all targets; these
    // can be referenced later on with texture name 0 in operations like glBindTexture().
    auto default_texture_2d = adopt_ref(*new Texture2D());
    m_default_textures.set(GL_TEXTURE_2D, default_texture_2d);
    for (auto& texture_unit : m_texture_units)
        texture_unit.set_texture_2d_target_texture(default_texture_2d);

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

GLContext::~GLContext()
{
    dbgln_if(GL_DEBUG, "GLContext::~GLContext() {:p}", this);
    if (g_gl_context == this)
        make_context_current(nullptr);
}

void GLContext::gl_begin(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_begin, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode > GL_POLYGON, GL_INVALID_ENUM);

    m_current_draw_mode = mode;
    m_in_draw_state = true; // Certain commands will now generate an error
}

void GLContext::gl_clear(GLbitfield mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear, mask);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mask & ~(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT), GL_INVALID_ENUM);

    if (mask & GL_COLOR_BUFFER_BIT)
        m_rasterizer->clear_color(m_clear_color);

    if (mask & GL_DEPTH_BUFFER_BIT)
        m_rasterizer->clear_depth(m_clear_depth);

    if (mask & GL_STENCIL_BUFFER_BIT)
        m_rasterizer->clear_stencil(m_clear_stencil);
}

void GLContext::gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_color, red, green, blue, alpha);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_color = { red, green, blue, alpha };
    m_clear_color.clamp(0.f, 1.f);
}

void GLContext::gl_clear_depth(GLdouble depth)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_depth, depth);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_depth = clamp(static_cast<float>(depth), 0.f, 1.f);
}

void GLContext::gl_end()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_end);

    // Make sure we had a `glBegin` before this call...
    RETURN_WITH_ERROR_IF(!m_in_draw_state, GL_INVALID_OPERATION);
    m_in_draw_state = false;

    Vector<size_t, 32> enabled_texture_units;
    for (size_t i = 0; i < m_texture_units.size(); ++i) {
        if (m_texture_units[i].texture_2d_enabled())
            enabled_texture_units.append(i);
    }

    sync_device_config();

    GPU::PrimitiveType primitive_type;
    switch (m_current_draw_mode) {
    case GL_LINE_LOOP:
        primitive_type = GPU::PrimitiveType::LineLoop;
        break;
    case GL_LINE_STRIP:
        primitive_type = GPU::PrimitiveType::LineStrip;
        break;
    case GL_LINES:
        primitive_type = GPU::PrimitiveType::Lines;
        break;
    case GL_POINTS:
        primitive_type = GPU::PrimitiveType::Points;
        break;
    case GL_TRIANGLES:
        primitive_type = GPU::PrimitiveType::Triangles;
        break;
    case GL_TRIANGLE_STRIP:
    case GL_QUAD_STRIP:
        primitive_type = GPU::PrimitiveType::TriangleStrip;
        break;
    case GL_TRIANGLE_FAN:
    case GL_POLYGON:
        primitive_type = GPU::PrimitiveType::TriangleFan;
        break;
    case GL_QUADS:
        primitive_type = GPU::PrimitiveType::Quads;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_rasterizer->draw_primitives(primitive_type, m_model_view_matrix, m_projection_matrix, m_texture_matrix, m_vertex_list, enabled_texture_units);
    m_vertex_list.clear_with_capacity();
}

GLenum GLContext::gl_get_error()
{
    if (m_in_draw_state)
        return GL_INVALID_OPERATION;

    auto last_error = m_error;
    m_error = GL_NO_ERROR;
    return last_error;
}

GLubyte* GLContext::gl_get_string(GLenum name)
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

void GLContext::gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_viewport, x, y, width, height);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    m_viewport = { x, y, width, height };

    auto rasterizer_options = m_rasterizer->options();
    rasterizer_options.viewport = m_viewport;
    m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_front_face(GLenum face)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_front_face, face);

    RETURN_WITH_ERROR_IF(face < GL_CW || face > GL_CCW, GL_INVALID_ENUM);

    m_front_face = face;

    auto rasterizer_options = m_rasterizer->options();
    rasterizer_options.front_face = (face == GL_CW) ? GPU::WindingOrder::Clockwise : GPU::WindingOrder::CounterClockwise;
    m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_cull_face(GLenum cull_mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_cull_face, cull_mode);

    RETURN_WITH_ERROR_IF(cull_mode < GL_FRONT || cull_mode > GL_FRONT_AND_BACK, GL_INVALID_ENUM);

    m_culled_sides = cull_mode;

    auto rasterizer_options = m_rasterizer->options();
    rasterizer_options.cull_back = cull_mode == GL_BACK || cull_mode == GL_FRONT_AND_BACK;
    rasterizer_options.cull_front = cull_mode == GL_FRONT || cull_mode == GL_FRONT_AND_BACK;
    m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_flush()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // No-op since GLContext is completely synchronous at the moment
}

void GLContext::gl_finish()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // No-op since GLContext is completely synchronous at the moment
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

    auto map_gl_blend_factor_to_device = [](GLenum factor) constexpr
    {
        switch (factor) {
        case GL_ZERO:
            return GPU::BlendFactor::Zero;
        case GL_ONE:
            return GPU::BlendFactor::One;
        case GL_SRC_ALPHA:
            return GPU::BlendFactor::SrcAlpha;
        case GL_ONE_MINUS_SRC_ALPHA:
            return GPU::BlendFactor::OneMinusSrcAlpha;
        case GL_SRC_COLOR:
            return GPU::BlendFactor::SrcColor;
        case GL_ONE_MINUS_SRC_COLOR:
            return GPU::BlendFactor::OneMinusSrcColor;
        case GL_DST_ALPHA:
            return GPU::BlendFactor::DstAlpha;
        case GL_ONE_MINUS_DST_ALPHA:
            return GPU::BlendFactor::OneMinusDstAlpha;
        case GL_DST_COLOR:
            return GPU::BlendFactor::DstColor;
        case GL_ONE_MINUS_DST_COLOR:
            return GPU::BlendFactor::OneMinusDstColor;
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

void GLContext::gl_alpha_func(GLenum func, GLclampf ref)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_alpha_func, func, ref);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(func < GL_NEVER || func > GL_ALWAYS, GL_INVALID_ENUM);

    m_alpha_test_func = func;
    m_alpha_test_ref_value = ref;

    auto options = m_rasterizer->options();

    switch (func) {
    case GL_NEVER:
        options.alpha_test_func = GPU::AlphaTestFunction::Never;
        break;
    case GL_ALWAYS:
        options.alpha_test_func = GPU::AlphaTestFunction::Always;
        break;
    case GL_LESS:
        options.alpha_test_func = GPU::AlphaTestFunction::Less;
        break;
    case GL_LEQUAL:
        options.alpha_test_func = GPU::AlphaTestFunction::LessOrEqual;
        break;
    case GL_EQUAL:
        options.alpha_test_func = GPU::AlphaTestFunction::Equal;
        break;
    case GL_NOTEQUAL:
        options.alpha_test_func = GPU::AlphaTestFunction::NotEqual;
        break;
    case GL_GEQUAL:
        options.alpha_test_func = GPU::AlphaTestFunction::GreaterOrEqual;
        break;
    case GL_GREATER:
        options.alpha_test_func = GPU::AlphaTestFunction::Greater;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    options.alpha_test_ref_value = m_alpha_test_ref_value;
    m_rasterizer->set_options(options);
}

void GLContext::gl_hint(GLenum target, GLenum mode)
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

void GLContext::gl_read_buffer(GLenum mode)
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

void GLContext::gl_draw_buffer(GLenum buffer)
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

    auto rasterizer_options = m_rasterizer->options();
    // FIXME: We only have a single draw buffer in SoftGPU at the moment,
    // so we simply disable color writes if GL_NONE is selected
    rasterizer_options.enable_color_write = m_current_draw_buffer != GL_NONE;
    m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
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
                float depth = m_rasterizer->get_depthbuffer_value(x + j, y + i);
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
            Gfx::ARGB32 color {};
            if (m_current_read_buffer == GL_FRONT || m_current_read_buffer == GL_LEFT || m_current_read_buffer == GL_FRONT_LEFT) {
                if (y + i >= m_frontbuffer->width() || x + j >= m_frontbuffer->height())
                    color = 0;
                else
                    color = m_frontbuffer->scanline(y + i)[x + j];
            } else {
                color = m_rasterizer->get_color_buffer_pixel(x + j, y + i);
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

void GLContext::gl_depth_mask(GLboolean flag)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_depth_mask, flag);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer->options();
    options.enable_depth_write = (flag != GL_FALSE);
    m_rasterizer->set_options(options);
}

void GLContext::gl_draw_pixels(GLsizei width, GLsizei height, GLenum format, GLenum type, void const* data)
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
                bitmap->set_pixel(x, y, Color::from_argb(*(pixel_data++)));

        m_rasterizer->blit_to_color_buffer_at_raster_position(bitmap);
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

        m_rasterizer->blit_to_depth_buffer_at_raster_position(depth_values, width, height);
    } else {
        VERIFY_NOT_REACHED();
    }
}

void GLContext::gl_depth_range(GLdouble min, GLdouble max)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_depth_range, min, max);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer->options();
    options.depth_min = clamp<float>(min, 0.f, 1.f);
    options.depth_max = clamp<float>(max, 0.f, 1.f);
    m_rasterizer->set_options(options);
}

void GLContext::gl_depth_func(GLenum func)
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

    auto options = m_rasterizer->options();

    switch (func) {
    case GL_NEVER:
        options.depth_func = GPU::DepthTestFunction::Never;
        break;
    case GL_ALWAYS:
        options.depth_func = GPU::DepthTestFunction::Always;
        break;
    case GL_LESS:
        options.depth_func = GPU::DepthTestFunction::Less;
        break;
    case GL_LEQUAL:
        options.depth_func = GPU::DepthTestFunction::LessOrEqual;
        break;
    case GL_EQUAL:
        options.depth_func = GPU::DepthTestFunction::Equal;
        break;
    case GL_NOTEQUAL:
        options.depth_func = GPU::DepthTestFunction::NotEqual;
        break;
    case GL_GEQUAL:
        options.depth_func = GPU::DepthTestFunction::GreaterOrEqual;
        break;
    case GL_GREATER:
        options.depth_func = GPU::DepthTestFunction::Greater;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_rasterizer->set_options(options);
}

void GLContext::gl_color_mask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    auto options = m_rasterizer->options();
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
    m_rasterizer->set_options(options);
}

void GLContext::gl_polygon_mode(GLenum face, GLenum mode)
{
    RETURN_WITH_ERROR_IF(!(face == GL_BACK || face == GL_FRONT || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(mode == GL_POINT || mode == GL_LINE || mode == GL_FILL), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer->options();

    // FIXME: This must support different polygon modes for front- and backside
    if (face == GL_BACK) {
        dbgln_if(GL_DEBUG, "gl_polygon_mode(GL_BACK, {:#x}): unimplemented", mode);
        return;
    }

    auto map_mode = [](GLenum mode) -> GPU::PolygonMode {
        switch (mode) {
        case GL_FILL:
            return GPU::PolygonMode::Fill;
        case GL_LINE:
            return GPU::PolygonMode::Line;
        case GL_POINT:
            return GPU::PolygonMode::Point;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    options.polygon_mode = map_mode(mode);
    m_rasterizer->set_options(options);
}

void GLContext::gl_polygon_offset(GLfloat factor, GLfloat units)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_polygon_offset, factor, units);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer->options();
    rasterizer_options.depth_offset_factor = factor;
    rasterizer_options.depth_offset_constant = units;
    m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_fogfv(GLenum pname, GLfloat* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_fogfv, pname, params);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto options = m_rasterizer->options();

    switch (pname) {
    case GL_FOG_COLOR:
        options.fog_color = { params[0], params[1], params[2], params[3] };
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    m_rasterizer->set_options(options);
}

void GLContext::gl_fogf(GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_fogf, pname, param);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(param < 0.0f, GL_INVALID_VALUE);

    auto options = m_rasterizer->options();

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

    m_rasterizer->set_options(options);
}

void GLContext::gl_fogi(GLenum pname, GLint param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_fogi, pname, param);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(param != GL_LINEAR && param != GL_EXP && param != GL_EXP2, GL_INVALID_ENUM);

    auto options = m_rasterizer->options();

    switch (pname) {
    case GL_FOG_MODE:
        switch (param) {
        case GL_LINEAR:
            options.fog_mode = GPU::FogMode::Linear;
            break;
        case GL_EXP:
            options.fog_mode = GPU::FogMode::Exp;
            break;
        case GL_EXP2:
            options.fog_mode = GPU::FogMode::Exp2;
            break;
        }
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    m_rasterizer->set_options(options);
}

void GLContext::gl_pixel_storei(GLenum pname, GLint param)
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
    }
}

void GLContext::gl_scissor(GLint x, GLint y, GLsizei width, GLsizei height)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_scissor, x, y, width, height);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    auto options = m_rasterizer->options();
    options.scissor_box = { x, y, width, height };
    m_rasterizer->set_options(options);
}

void GLContext::gl_raster_pos(GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_raster_pos, x, y, z, w);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_rasterizer->set_raster_position({ x, y, z, w }, m_model_view_matrix, m_projection_matrix);
}

void GLContext::gl_line_width(GLfloat width)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_line_width, width);

    RETURN_WITH_ERROR_IF(width <= 0, GL_INVALID_VALUE);

    m_line_width = width;
    auto options = m_rasterizer->options();
    options.line_width = width;
    m_rasterizer->set_options(options);
}

void GLContext::gl_push_attrib(GLbitfield mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_push_attrib, mask);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: implement
    dbgln_if(GL_DEBUG, "GLContext FIXME: implement gl_push_attrib({})", mask);
}

void GLContext::gl_pop_attrib()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_pop_attrib);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: implement
    dbgln_if(GL_DEBUG, "GLContext FIXME: implement gl_pop_attrib()");
}

void GLContext::gl_bitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_bitmap, width, height, xorig, yorig, xmove, ymove, bitmap);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (bitmap != nullptr) {
        // FIXME: implement
        dbgln_if(GL_DEBUG, "gl_bitmap({}, {}, {}, {}, {}, {}, {}): unimplemented", width, height, xorig, yorig, xmove, ymove, bitmap);
    }

    auto raster_position = m_rasterizer->raster_position();
    raster_position.window_coordinates += { xmove, ymove, 0.f, 0.f };
    m_rasterizer->set_raster_position(raster_position);
}

void GLContext::gl_rect(GLdouble x1, GLdouble y1, GLdouble x2, GLdouble y2)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_rect, x1, y1, x2, y2);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    gl_begin(GL_POLYGON);
    gl_vertex(x1, y1, 0.0, 1.0);
    gl_vertex(x2, y1, 0.0, 1.0);
    gl_vertex(x2, y2, 0.0, 1.0);
    gl_vertex(x1, y2, 0.0, 1.0);
    gl_end();
}

void GLContext::gl_point_size(GLfloat size)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_point_size, size);
    RETURN_WITH_ERROR_IF(size <= 0.f, GL_INVALID_VALUE);

    m_point_size = size;

    auto rasterizer_options = m_rasterizer->options();
    rasterizer_options.point_size = size;
    m_rasterizer->set_options(rasterizer_options);
}

void GLContext::present()
{
    m_rasterizer->blit_color_buffer_to(*m_frontbuffer);
}

void GLContext::sync_device_config()
{
    sync_device_sampler_config();
    sync_device_texcoord_config();
    sync_light_state();
    sync_stencil_configuration();
    sync_clip_planes();
}

void GLContext::build_extension_string()
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

NonnullOwnPtr<GLContext> create_context(Gfx::Bitmap& bitmap)
{
    // FIXME: Make driver selectable. This is currently hardcoded to LibSoftGPU
    auto driver = MUST(GPU::Driver::try_create("softgpu"));
    auto device = MUST(driver->try_create_device(bitmap.size()));
    auto context = make<GLContext>(driver, move(device), bitmap);
    dbgln_if(GL_DEBUG, "GL::create_context({}) -> {:p}", bitmap.size(), context.ptr());

    if (!g_gl_context)
        make_context_current(context);

    return context;
}

void make_context_current(GLContext* context)
{
    if (g_gl_context == context)
        return;

    dbgln_if(GL_DEBUG, "GL::make_context_current({:p})", context);
    g_gl_context = context;
}

void present_context(GLContext* context)
{
    context->present();
}

}
