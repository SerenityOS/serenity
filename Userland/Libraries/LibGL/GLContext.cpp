/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022-2024, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGL/GLContext.h>
#include <LibGL/Image.h>
#include <LibGPU/Device.h>
#include <LibGPU/Enums.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGPU/ImageFormat.h>
#include <LibGfx/Bitmap.h>

__attribute__((visibility("hidden"))) GL::GLContext* g_gl_context;

namespace GL {

GLContext::GLContext(RefPtr<GPU::Driver> driver, NonnullOwnPtr<GPU::Device> device, Gfx::Bitmap& frontbuffer)
    : m_driver { driver }
    , m_rasterizer { move(device) }
    , m_device_info { m_rasterizer->info() }
    , m_viewport { frontbuffer.rect() }
    , m_frontbuffer { frontbuffer }
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
        texture_coordinate_generation[0].object_plane_coefficients = { 1.f, 0.f, 0.f, 0.f };
        texture_coordinate_generation[0].eye_plane_coefficients = { 1.f, 0.f, 0.f, 0.f };
        texture_coordinate_generation[1].object_plane_coefficients = { 0.f, 1.f, 0.f, 0.f };
        texture_coordinate_generation[1].eye_plane_coefficients = { 0.f, 1.f, 0.f, 0.f };
        texture_coordinate_generation[2].object_plane_coefficients = { 0.f, 0.f, 0.f, 0.f };
        texture_coordinate_generation[2].eye_plane_coefficients = { 0.f, 0.f, 0.f, 0.f };
        texture_coordinate_generation[3].object_plane_coefficients = { 0.f, 0.f, 0.f, 0.f };
        texture_coordinate_generation[3].eye_plane_coefficients = { 0.f, 0.f, 0.f, 0.f };
    }

    m_extensions = build_extension_string().release_value_but_fixme_should_propagate_errors();
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

void GLContext::gl_clear_depth(GLfloat depth)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_depth, depth);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_depth = clamp(depth, 0.f, 1.f);
}

void GLContext::gl_end()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_end);

    // Make sure we had a `glBegin` before this call...
    RETURN_WITH_ERROR_IF(!m_in_draw_state, GL_INVALID_OPERATION);
    m_in_draw_state = false;

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

    m_rasterizer->draw_primitives(primitive_type, m_vertex_list);
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

GLubyte const* GLContext::gl_get_string(GLenum name)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, nullptr);

    switch (name) {
    case GL_VENDOR:
        return reinterpret_cast<GLubyte const*>(m_device_info.vendor_name.characters());
    case GL_RENDERER:
        return reinterpret_cast<GLubyte const*>(m_device_info.device_name.characters());
    case GL_VERSION:
        return reinterpret_cast<GLubyte const*>("1.5");
    case GL_EXTENSIONS:
        return reinterpret_cast<GLubyte const*>(m_extensions.data());
    case GL_SHADING_LANGUAGE_VERSION:
        return reinterpret_cast<GLubyte const*>("0.0");
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

    RETURN_WITH_ERROR_IF(cull_mode != GL_FRONT && cull_mode != GL_BACK && cull_mode != GL_FRONT_AND_BACK, GL_INVALID_ENUM);

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

    RETURN_WITH_ERROR_IF(format == GL_NONE || type == GL_NONE, GL_INVALID_ENUM);
    auto pixel_type_or_error = get_validated_pixel_type(GL_NONE, GL_NONE, format, type);
    RETURN_WITH_ERROR_IF(pixel_type_or_error.is_error(), pixel_type_or_error.release_error().code());

    auto pixel_type = pixel_type_or_error.release_value();
    GPU::ImageDataLayout output_layout = {
        .pixel_type = pixel_type,
        .packing = get_packing_specification(PackingType::Pack),
        .dimensions = {
            .width = static_cast<u32>(width),
            .height = static_cast<u32>(height),
            .depth = 1,
        },
        .selection = {
            .width = static_cast<u32>(width),
            .height = static_cast<u32>(height),
            .depth = 1,
        },
    };

    if (pixel_type.format == GPU::PixelFormat::DepthComponent) {
        // FIXME: This check needs to be a bit more sophisticated. Currently the buffers are
        //        hardcoded. Once we add proper structures for them we need to correct this check

        // Error because only back buffer has a depth buffer
        RETURN_WITH_ERROR_IF(m_current_read_buffer == GL_FRONT
                || m_current_read_buffer == GL_FRONT_LEFT
                || m_current_read_buffer == GL_FRONT_RIGHT,
            GL_INVALID_OPERATION);

        m_rasterizer->blit_from_depth_buffer(pixels, { x, y }, output_layout);
    } else if (pixel_type.format == GPU::PixelFormat::StencilIndex) {
        dbgln("gl_read_pixels(): GL_STENCIL_INDEX is not yet supported");
    } else {
        m_rasterizer->blit_from_color_buffer(pixels, { x, y }, output_layout);
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

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    // FIXME: GL_INVALID_OPERATION is generated if format is GL_STENCIL_INDEX and there is no stencil buffer
    // FIXME: GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to the GL_PIXEL_UNPACK_BUFFER
    //        target and the buffer object's data store is currently mapped.
    // FIXME: GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to the GL_PIXEL_UNPACK_BUFFER
    //        target and the data would be unpacked from the buffer object such that the memory reads required would
    //        exceed the data store size.
    // FIXME: GL_INVALID_OPERATION is generated if a non-zero buffer object name is bound to the GL_PIXEL_UNPACK_BUFFER
    //        target and data is not evenly divisible into the number of bytes needed to store in memory a datum
    //        indicated by type.

    RETURN_WITH_ERROR_IF(format == GL_NONE || type == GL_NONE, GL_INVALID_ENUM);
    auto pixel_type_or_error = get_validated_pixel_type(GL_NONE, GL_NONE, format, type);
    RETURN_WITH_ERROR_IF(pixel_type_or_error.is_error(), pixel_type_or_error.release_error().code());

    auto pixel_type = pixel_type_or_error.release_value();
    GPU::ImageDataLayout input_layout = {
        .pixel_type = pixel_type,
        .packing = get_packing_specification(PackingType::Unpack),
        .dimensions = {
            .width = static_cast<u32>(width),
            .height = static_cast<u32>(height),
            .depth = 1,
        },
        .selection = {
            .width = static_cast<u32>(width),
            .height = static_cast<u32>(height),
            .depth = 1,
        },
    };

    if (pixel_type.format == GPU::PixelFormat::DepthComponent) {
        m_rasterizer->blit_to_depth_buffer_at_raster_position(data, input_layout);
    } else if (pixel_type.format == GPU::PixelFormat::StencilIndex) {
        dbgln("gl_draw_pixels(): GL_STENCIL_INDEX is not yet supported");
    } else {
        m_rasterizer->blit_to_color_buffer_at_raster_position(data, input_layout);
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
    options.color_mask = (red == GL_TRUE ? 0xff : 0)
        | (green == GL_TRUE ? 0xff00 : 0)
        | (blue == GL_TRUE ? 0xff0000 : 0)
        | (alpha == GL_TRUE ? 0xff000000 : 0);
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

void GLContext::gl_fogfv(GLenum pname, GLfloat const* params)
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
    auto const is_packing_parameter = (pname >= GL_PACK_SWAP_BYTES && pname <= GL_PACK_ALIGNMENT)
        || pname == GL_PACK_SKIP_IMAGES
        || pname == GL_PACK_IMAGE_HEIGHT;
    auto& pixel_parameters = is_packing_parameter ? m_packing_parameters : m_unpacking_parameters;

    switch (pname) {
    case GL_PACK_ALIGNMENT:
    case GL_UNPACK_ALIGNMENT:
        RETURN_WITH_ERROR_IF(param != 1 && param != 2 && param != 4 && param != 8, GL_INVALID_VALUE);
        pixel_parameters.pack_alignment = param;
        break;
    case GL_PACK_IMAGE_HEIGHT:
    case GL_UNPACK_IMAGE_HEIGHT:
        RETURN_WITH_ERROR_IF(param < 0, GL_INVALID_VALUE);
        pixel_parameters.image_height = param;
        break;
    case GL_PACK_LSB_FIRST:
    case GL_UNPACK_LSB_FIRST:
        pixel_parameters.least_significant_bit_first = (param != 0);
        break;
    case GL_PACK_ROW_LENGTH:
    case GL_UNPACK_ROW_LENGTH:
        RETURN_WITH_ERROR_IF(param < 0, GL_INVALID_VALUE);
        pixel_parameters.row_length = param;
        break;
    case GL_PACK_SKIP_IMAGES:
    case GL_UNPACK_SKIP_IMAGES:
        RETURN_WITH_ERROR_IF(param < 0, GL_INVALID_VALUE);
        pixel_parameters.skip_images = param;
        break;
    case GL_PACK_SKIP_PIXELS:
    case GL_UNPACK_SKIP_PIXELS:
        RETURN_WITH_ERROR_IF(param < 0, GL_INVALID_VALUE);
        pixel_parameters.skip_pixels = param;
        break;
    case GL_PACK_SKIP_ROWS:
    case GL_UNPACK_SKIP_ROWS:
        RETURN_WITH_ERROR_IF(param < 0, GL_INVALID_VALUE);
        pixel_parameters.skip_rows = param;
        break;
    case GL_PACK_SWAP_BYTES:
    case GL_UNPACK_SWAP_BYTES:
        pixel_parameters.swap_bytes = (param != 0);
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

    sync_matrices();
    m_rasterizer->set_raster_position({ x, y, z, w });
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
    m_rasterizer->blit_from_color_buffer(*m_frontbuffer);
}

void GLContext::sync_device_config()
{
    sync_clip_planes();
    sync_device_sampler_config();
    sync_device_texture_units();
    sync_light_state();
    sync_matrices();
    sync_stencil_configuration();
}

ErrorOr<ByteBuffer> GLContext::build_extension_string()
{
    Vector<StringView, 6> extensions;

    // FIXME: npot texture support became a required core feature starting with OpenGL 2.0 (https://www.khronos.org/opengl/wiki/NPOT_Texture)
    // Ideally we would verify if the selected device adheres to the requested OpenGL context version before context creation
    // and refuse to create a context if it doesn't.
    if (m_device_info.supports_npot_textures)
        TRY(extensions.try_append("GL_ARB_texture_non_power_of_two"sv));

    if (m_device_info.num_texture_units > 1)
        TRY(extensions.try_append("GL_ARB_multitexture"sv));

    if (m_device_info.supports_texture_clamp_to_edge)
        TRY(extensions.try_append("GL_EXT_texture_edge_clamp"sv));

    if (m_device_info.supports_texture_env_add) {
        TRY(extensions.try_append("GL_ARB_texture_env_add"sv));
        TRY(extensions.try_append("GL_EXT_texture_env_add"sv));
    }

    if (m_device_info.max_texture_lod_bias > 0.f)
        TRY(extensions.try_append("GL_EXT_texture_lod_bias"sv));

    StringBuilder string_builder {};
    TRY(string_builder.try_join(' ', extensions));

    // Create null-terminated string
    auto extensions_bytes = TRY(string_builder.to_byte_buffer());
    TRY(extensions_bytes.try_append(0));
    return extensions_bytes;
}

ErrorOr<NonnullOwnPtr<GLContext>> create_context(Gfx::Bitmap& bitmap)
{
    // FIXME: Make driver selectable. This is currently hardcoded to LibSoftGPU
    auto driver = TRY(GPU::Driver::try_create("softgpu"sv));
    auto device = TRY(driver->try_create_device(bitmap.size()));
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

}
