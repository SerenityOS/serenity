/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022-2024, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGL/GLContext.h>

namespace GL {

Optional<ContextParameter> GLContext::get_context_parameter(GLenum name)
{
    switch (name) {
    case GL_ACTIVE_TEXTURE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(GL_TEXTURE0 + m_active_texture_unit_index) } };
    case GL_ALPHA_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(u8) * 8 } };
    case GL_ALPHA_TEST:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_alpha_test_enabled } };
    case GL_BLEND:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_blend_enabled } };
    case GL_BLEND_DST:
    case GL_BLEND_DST_ALPHA:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_blend_destination_factor) } };
    case GL_BLEND_EQUATION_ALPHA:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_blend_equation_alpha) } };
    case GL_BLEND_EQUATION_RGB:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_blend_equation_rgb) } };
    case GL_BLEND_SRC:
    case GL_BLEND_SRC_ALPHA:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_blend_source_factor) } };
    case GL_BLUE_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(u8) * 8 } };
    case GL_CLIENT_ACTIVE_TEXTURE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(GL_TEXTURE0 + m_client_active_texture) } };
    case GL_COLOR_CLEAR_VALUE:
        return ContextParameter {
            .type = GL_DOUBLE,
            .count = 4,
            .value = {
                .double_list = {
                    static_cast<GLdouble>(m_clear_color.x()),
                    static_cast<GLdouble>(m_clear_color.y()),
                    static_cast<GLdouble>(m_clear_color.z()),
                    static_cast<GLdouble>(m_clear_color.w()),
                } }
        };
    case GL_COLOR_MATERIAL:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_color_material_enabled } };
    case GL_COLOR_MATERIAL_FACE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_color_material_face) } };
    case GL_COLOR_MATERIAL_MODE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_color_material_mode) } };
    case GL_CURRENT_COLOR:
        return ContextParameter {
            .type = GL_DOUBLE,
            .count = 4,
            .value = {
                .double_list = {
                    static_cast<double>(m_current_vertex_color.x()),
                    static_cast<double>(m_current_vertex_color.y()),
                    static_cast<double>(m_current_vertex_color.z()),
                    static_cast<double>(m_current_vertex_color.w()),
                } }
        };
    case GL_CULL_FACE:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_cull_faces } };
    case GL_DEPTH_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(float) * 8 } };
    case GL_DEPTH_CLEAR_VALUE:
        return ContextParameter { .type = GL_DOUBLE, .value = { .double_value = static_cast<GLdouble>(m_clear_depth) } };
    case GL_DEPTH_TEST:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_depth_test_enabled } };
    case GL_DITHER:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_dither_enabled } };
    case GL_DOUBLEBUFFER:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = true } };
    case GL_FOG: {
        auto fog_enabled = m_rasterizer->options().fog_enabled;
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = fog_enabled } };
    }
    case GL_GREEN_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(u8) * 8 } };
    case GL_LIGHTING:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_lighting_enabled } };
    case GL_LINE_SMOOTH:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_line_smooth } };
    case GL_MAX_CLIP_PLANES:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_device_info.max_clip_planes) } };
    case GL_MAX_LIGHTS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_device_info.num_lights) } };
    case GL_MAX_MODELVIEW_STACK_DEPTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = MODELVIEW_MATRIX_STACK_LIMIT } };
    case GL_MAX_PROJECTION_STACK_DEPTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = PROJECTION_MATRIX_STACK_LIMIT } };
    case GL_MAX_TEXTURE_LOD_BIAS:
        return ContextParameter { .type = GL_DOUBLE, .value = { .double_value = static_cast<GLdouble>(m_device_info.max_texture_lod_bias) } };
    case GL_MAX_TEXTURE_SIZE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_device_info.max_texture_size) } };
    case GL_MAX_TEXTURE_STACK_DEPTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = TEXTURE_MATRIX_STACK_LIMIT } };
    case GL_MAX_TEXTURE_UNITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = static_cast<GLint>(m_texture_units.size()) } };
    case GL_NORMAL_ARRAY_TYPE:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = GL_FLOAT } };
    case GL_NORMALIZE:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_normalize } };
    case GL_PACK_ALIGNMENT:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_packing_parameters.pack_alignment } };
    case GL_PACK_IMAGE_HEIGHT:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_packing_parameters.image_height } };
    case GL_PACK_LSB_FIRST:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_packing_parameters.least_significant_bit_first } };
    case GL_PACK_ROW_LENGTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_packing_parameters.row_length } };
    case GL_PACK_SKIP_IMAGES:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_packing_parameters.skip_images } };
    case GL_PACK_SKIP_PIXELS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_packing_parameters.skip_pixels } };
    case GL_PACK_SKIP_ROWS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_packing_parameters.skip_rows } };
    case GL_PACK_SWAP_BYTES:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_packing_parameters.swap_bytes } };
    case GL_POINT_SMOOTH:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_point_smooth } };
    case GL_POINT_SIZE:
        return ContextParameter { .type = GL_DOUBLE, .value = { .double_value = static_cast<GLdouble>(m_point_size) } };
    case GL_POLYGON_OFFSET_FILL:
        return ContextParameter { .type = GL_BOOL, .is_capability = true, .value = { .boolean_value = m_depth_offset_enabled } };
    case GL_RED_BITS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = sizeof(u8) * 8 } };
    case GL_SAMPLE_BUFFERS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 0 } };
    case GL_SAMPLES:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = 1 } };
    case GL_SCISSOR_BOX: {
        auto scissor_box = m_rasterizer->options().scissor_box;
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
    }
    case GL_SCISSOR_TEST: {
        auto scissor_enabled = m_rasterizer->options().scissor_enabled;
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
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpacking_parameters.pack_alignment } };
    case GL_UNPACK_IMAGE_HEIGHT:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpacking_parameters.image_height } };
    case GL_UNPACK_LSB_FIRST:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_unpacking_parameters.least_significant_bit_first } };
    case GL_UNPACK_ROW_LENGTH:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpacking_parameters.row_length } };
    case GL_UNPACK_SKIP_IMAGES:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpacking_parameters.skip_images } };
    case GL_UNPACK_SKIP_PIXELS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpacking_parameters.skip_pixels } };
    case GL_UNPACK_SKIP_ROWS:
        return ContextParameter { .type = GL_INT, .value = { .integer_value = m_unpacking_parameters.skip_rows } };
    case GL_UNPACK_SWAP_BYTES:
        return ContextParameter { .type = GL_BOOL, .value = { .boolean_value = m_unpacking_parameters.swap_bytes } };
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

void GLContext::gl_disable(GLenum capability)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_disable, capability);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer->options();
    bool update_rasterizer_options = false;

    switch (capability) {
    case GL_CLIP_PLANE0:
    case GL_CLIP_PLANE1:
    case GL_CLIP_PLANE2:
    case GL_CLIP_PLANE3:
    case GL_CLIP_PLANE4:
    case GL_CLIP_PLANE5: {
        auto plane_idx = static_cast<size_t>(capability) - GL_CLIP_PLANE0;
        m_clip_plane_attributes.enabled &= ~(1 << plane_idx);
        m_clip_planes_dirty = true;
        break;
    }
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
    case GL_LINE_SMOOTH:
        m_line_smooth = false;
        rasterizer_options.line_smooth = false;
        update_rasterizer_options = true;
        break;
    case GL_NORMALIZE:
        m_normalize = false;
        rasterizer_options.normalization_enabled = false;
        update_rasterizer_options = true;
        break;
    case GL_POINT_SMOOTH:
        m_point_smooth = false;
        rasterizer_options.point_smooth = false;
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
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_2D:
        m_active_texture_unit->set_texture_2d_enabled(false);
        m_sampler_config_is_dirty = true;
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_3D:
        m_active_texture_unit->set_texture_3d_enabled(false);
        m_sampler_config_is_dirty = true;
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_CUBE_MAP:
        m_active_texture_unit->set_texture_cube_map_enabled(false);
        m_sampler_config_is_dirty = true;
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_GEN_Q:
    case GL_TEXTURE_GEN_R:
    case GL_TEXTURE_GEN_S:
    case GL_TEXTURE_GEN_T:
        texture_coordinate_generation(m_active_texture_unit_index, capability).enabled = false;
        m_texture_units_dirty = true;
        break;
    default:
        dbgln_if(GL_DEBUG, "gl_disable({:#x}): unknown parameter", capability);
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    if (update_rasterizer_options)
        m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_disable_client_state(GLenum cap)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    switch (cap) {
    case GL_COLOR_ARRAY:
        m_client_side_color_array_enabled = false;
        break;
    case GL_NORMAL_ARRAY:
        m_client_side_normal_array_enabled = false;
        break;
    case GL_TEXTURE_COORD_ARRAY:
        m_client_side_texture_coord_array_enabled[m_client_active_texture] = false;
        break;
    case GL_VERTEX_ARRAY:
        m_client_side_vertex_array_enabled = false;
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }
}

void GLContext::gl_enable(GLenum capability)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_enable, capability);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer->options();
    bool update_rasterizer_options = false;

    switch (capability) {
    case GL_CLIP_PLANE0:
    case GL_CLIP_PLANE1:
    case GL_CLIP_PLANE2:
    case GL_CLIP_PLANE3:
    case GL_CLIP_PLANE4:
    case GL_CLIP_PLANE5: {
        auto plane_idx = static_cast<size_t>(capability) - GL_CLIP_PLANE0;
        m_clip_plane_attributes.enabled |= (1 << plane_idx);
        m_clip_planes_dirty = true;
        break;
    }
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
    case GL_LINE_SMOOTH:
        m_line_smooth = true;
        rasterizer_options.line_smooth = true;
        update_rasterizer_options = true;
        break;
    case GL_NORMALIZE:
        m_normalize = true;
        rasterizer_options.normalization_enabled = true;
        update_rasterizer_options = true;
        break;
    case GL_POINT_SMOOTH:
        m_point_smooth = true;
        rasterizer_options.point_smooth = true;
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
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_2D:
        m_active_texture_unit->set_texture_2d_enabled(true);
        m_sampler_config_is_dirty = true;
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_3D:
        m_active_texture_unit->set_texture_3d_enabled(true);
        m_sampler_config_is_dirty = true;
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_CUBE_MAP:
        m_active_texture_unit->set_texture_cube_map_enabled(true);
        m_sampler_config_is_dirty = true;
        m_texture_units_dirty = true;
        break;
    case GL_TEXTURE_GEN_Q:
    case GL_TEXTURE_GEN_R:
    case GL_TEXTURE_GEN_S:
    case GL_TEXTURE_GEN_T:
        texture_coordinate_generation(m_active_texture_unit_index, capability).enabled = true;
        m_texture_units_dirty = true;
        break;
    default:
        dbgln_if(GL_DEBUG, "gl_enable({:#x}): unknown parameter", capability);
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    if (update_rasterizer_options)
        m_rasterizer->set_options(rasterizer_options);
}

void GLContext::gl_enable_client_state(GLenum cap)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    switch (cap) {
    case GL_COLOR_ARRAY:
        m_client_side_color_array_enabled = true;
        break;
    case GL_NORMAL_ARRAY:
        m_client_side_normal_array_enabled = true;
        break;
    case GL_TEXTURE_COORD_ARRAY:
        m_client_side_texture_coord_array_enabled[m_client_active_texture] = true;
        break;
    case GL_VERTEX_ARRAY:
        m_client_side_vertex_array_enabled = true;
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }
}

void GLContext::gl_get_booleanv(GLenum pname, GLboolean* data)
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

void GLContext::gl_get_doublev(GLenum pname, GLdouble* params)
{
    get_floating_point(pname, params);
}

template<typename T>
void GLContext::get_floating_point(GLenum pname, T* params)
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
        flatten_and_assign_matrix(model_view_matrix());
        return;
    case GL_PROJECTION_MATRIX:
        flatten_and_assign_matrix(projection_matrix());
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

void GLContext::gl_get_floatv(GLenum pname, GLfloat* params)
{
    get_floating_point(pname, params);
}

void GLContext::gl_get_integerv(GLenum pname, GLint* data)
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

GLboolean GLContext::gl_is_enabled(GLenum capability)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, 0);

    auto optional_parameter = get_context_parameter(capability);
    RETURN_VALUE_WITH_ERROR_IF(!optional_parameter.has_value(), GL_INVALID_ENUM, 0);

    auto parameter = optional_parameter.release_value();
    RETURN_VALUE_WITH_ERROR_IF(!parameter.is_capability, GL_INVALID_ENUM, 0);

    return parameter.value.boolean_value ? GL_TRUE : GL_FALSE;
}

GPU::PackingSpecification GLContext::get_packing_specification(PackingType packing_type)
{
    // FIXME: add support for .least_significant_bit_first, .skip_images, .skip_pixels and .skip_rows
    auto const& pixel_parameters = (packing_type == PackingType::Pack) ? m_packing_parameters : m_unpacking_parameters;
    return {
        .depth_stride = static_cast<u32>(pixel_parameters.image_height),
        .row_stride = static_cast<u32>(pixel_parameters.row_length),
        .byte_alignment = pixel_parameters.pack_alignment,
        .component_bytes_order = pixel_parameters.swap_bytes ? GPU::ComponentBytesOrder::Reversed : GPU::ComponentBytesOrder::Normal,
    };
}

}
