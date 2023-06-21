/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GLContext.h>

namespace GL {

template<typename T>
void GLContext::get_light_param(GLenum light, GLenum pname, T* params)
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

template<typename T>
void GLContext::get_material_param(Face face, GLenum pname, T* params)
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

void GLContext::gl_color_material(GLenum face, GLenum mode)
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

void GLContext::gl_get_light(GLenum light, GLenum pname, void* params, GLenum type)
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

void GLContext::gl_get_material(GLenum face, GLenum pname, void* params, GLenum type)
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

void GLContext::gl_light_model(GLenum pname, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_light_model, pname, x, y, z, w);

    RETURN_WITH_ERROR_IF(pname != GL_LIGHT_MODEL_AMBIENT
            && pname != GL_LIGHT_MODEL_COLOR_CONTROL
            && pname != GL_LIGHT_MODEL_LOCAL_VIEWER
            && pname != GL_LIGHT_MODEL_TWO_SIDE,
        GL_INVALID_ENUM);

    auto lighting_params = m_rasterizer->light_model();

    switch (pname) {
    case GL_LIGHT_MODEL_AMBIENT:
        lighting_params.scene_ambient_color = { x, y, z, w };
        break;
    case GL_LIGHT_MODEL_COLOR_CONTROL: {
        auto color_control = static_cast<GLenum>(x);
        RETURN_WITH_ERROR_IF(color_control != GL_SINGLE_COLOR && color_control != GL_SEPARATE_SPECULAR_COLOR, GL_INVALID_ENUM);
        lighting_params.color_control = (color_control == GL_SINGLE_COLOR) ? GPU::ColorControl::SingleColor : GPU::ColorControl::SeparateSpecularColor;
        break;
    }
    case GL_LIGHT_MODEL_LOCAL_VIEWER:
        // 0 means the viewer is at infinity
        // 1 means they're in local (eye) space
        lighting_params.viewer_at_infinity = (x == 0.f);
        break;
    case GL_LIGHT_MODEL_TWO_SIDE:
        lighting_params.two_sided_lighting = (x != 0.f);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_rasterizer->set_light_model_params(lighting_params);
}

void GLContext::gl_light_modelv(GLenum pname, void const* params, GLenum type)
{
    VERIFY(type == GL_FLOAT || type == GL_INT);

    auto parameters_to_vector = [&]<typename T>(T const* params) -> FloatVector4 {
        return (pname == GL_LIGHT_MODEL_AMBIENT)
            ? Vector4<T> { params[0], params[1], params[2], params[3] }.template to_type<float>()
            : Vector4<T> { params[0], 0, 0, 0 }.template to_type<float>();
    };

    auto light_model_parameters = (type == GL_FLOAT)
        ? parameters_to_vector(reinterpret_cast<GLfloat const*>(params))
        : parameters_to_vector(reinterpret_cast<GLint const*>(params));

    // Normalize integers to -1..1
    if (pname == GL_LIGHT_MODEL_AMBIENT && type == GL_INT)
        light_model_parameters = (light_model_parameters + 2147483648.f) / 2147483647.5f - 1.f;

    gl_light_model(pname, light_model_parameters[0], light_model_parameters[1], light_model_parameters[2], light_model_parameters[3]);
}

void GLContext::gl_lightf(GLenum light, GLenum pname, GLfloat param)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_lightf, light, pname, param);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(light < GL_LIGHT0 || light >= (GL_LIGHT0 + m_device_info.num_lights), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_CONSTANT_ATTENUATION || pname == GL_LINEAR_ATTENUATION || pname == GL_QUADRATIC_ATTENUATION || pname != GL_SPOT_EXPONENT || pname != GL_SPOT_CUTOFF), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(param < 0.f, GL_INVALID_VALUE);

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
        RETURN_WITH_ERROR_IF(param > 128.f, GL_INVALID_VALUE);
        light_state.spotlight_exponent = param;
        break;
    case GL_SPOT_CUTOFF:
        RETURN_WITH_ERROR_IF(param > 90.f && param != 180.f, GL_INVALID_VALUE);
        light_state.spotlight_cutoff_angle = param;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void GLContext::gl_lightfv(GLenum light, GLenum pname, GLfloat const* params)
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
        light_state.position = model_view_matrix() * light_state.position;
        break;
    case GL_CONSTANT_ATTENUATION:
        RETURN_WITH_ERROR_IF(params[0] < 0.f, GL_INVALID_VALUE);
        light_state.constant_attenuation = params[0];
        break;
    case GL_LINEAR_ATTENUATION:
        RETURN_WITH_ERROR_IF(params[0] < 0.f, GL_INVALID_VALUE);
        light_state.linear_attenuation = params[0];
        break;
    case GL_QUADRATIC_ATTENUATION:
        RETURN_WITH_ERROR_IF(params[0] < 0.f, GL_INVALID_VALUE);
        light_state.quadratic_attenuation = params[0];
        break;
    case GL_SPOT_EXPONENT: {
        auto exponent = params[0];
        RETURN_WITH_ERROR_IF(exponent < 0.f || exponent > 128.f, GL_INVALID_VALUE);
        light_state.spotlight_exponent = exponent;
        break;
    }
    case GL_SPOT_CUTOFF: {
        auto cutoff = params[0];
        RETURN_WITH_ERROR_IF((cutoff < 0.f || cutoff > 90.f) && cutoff != 180.f, GL_INVALID_VALUE);
        light_state.spotlight_cutoff_angle = cutoff;
        break;
    }
    case GL_SPOT_DIRECTION: {
        FloatVector4 direction_vector = { params[0], params[1], params[2], 0.f };
        direction_vector = model_view_matrix() * direction_vector;
        light_state.spotlight_direction = direction_vector.xyz();
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void GLContext::gl_lightiv(GLenum light, GLenum pname, GLint const* params)
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
        light_state.position = model_view_matrix() * light_state.position;
        break;
    case GL_CONSTANT_ATTENUATION:
        RETURN_WITH_ERROR_IF(params[0] < 0, GL_INVALID_VALUE);
        light_state.constant_attenuation = static_cast<float>(params[0]);
        break;
    case GL_LINEAR_ATTENUATION:
        RETURN_WITH_ERROR_IF(params[0] < 0, GL_INVALID_VALUE);
        light_state.linear_attenuation = static_cast<float>(params[0]);
        break;
    case GL_QUADRATIC_ATTENUATION:
        RETURN_WITH_ERROR_IF(params[0] < 0, GL_INVALID_VALUE);
        light_state.quadratic_attenuation = static_cast<float>(params[0]);
        break;
    case GL_SPOT_EXPONENT: {
        auto exponent = static_cast<float>(params[0]);
        RETURN_WITH_ERROR_IF(exponent < 0.f || exponent > 128.f, GL_INVALID_VALUE);
        light_state.spotlight_exponent = exponent;
        break;
    }
    case GL_SPOT_CUTOFF: {
        auto cutoff = static_cast<float>(params[0]);
        RETURN_WITH_ERROR_IF((cutoff < 0.f || cutoff > 90.f) && cutoff != 180.f, GL_INVALID_VALUE);
        light_state.spotlight_cutoff_angle = cutoff;
        break;
    }
    case GL_SPOT_DIRECTION: {
        auto direction_vector = to_float_vector(params[0], params[1], params[2], 0.0f);
        direction_vector = model_view_matrix() * direction_vector;
        light_state.spotlight_direction = direction_vector.xyz();
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    m_light_state_is_dirty = true;
}

void GLContext::gl_materialf(GLenum face, GLenum pname, GLfloat param)
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

void GLContext::gl_materialfv(GLenum face, GLenum pname, GLfloat const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_materialfv, face, pname, params);
    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_EMISSION || pname == GL_SHININESS || pname == GL_AMBIENT_AND_DIFFUSE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF((pname == GL_SHININESS && *params > 128.0f), GL_INVALID_VALUE);

    auto update_material = [](GPU::Material& material, GLenum pname, GLfloat const* params) {
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
            material.shininess = params[0];
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

void GLContext::gl_materialiv(GLenum face, GLenum pname, GLint const* params)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_materialiv, face, pname, params);
    RETURN_WITH_ERROR_IF(!(face == GL_FRONT || face == GL_BACK || face == GL_FRONT_AND_BACK), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!(pname == GL_AMBIENT || pname == GL_DIFFUSE || pname == GL_SPECULAR || pname == GL_EMISSION || pname == GL_SHININESS || pname == GL_AMBIENT_AND_DIFFUSE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF((pname == GL_SHININESS && *params > 128), GL_INVALID_VALUE);

    auto update_material = [](GPU::Material& material, GLenum pname, GLint const* params) {
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

void GLContext::gl_shade_model(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_shade_model, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode != GL_FLAT && mode != GL_SMOOTH, GL_INVALID_ENUM);

    auto options = m_rasterizer->options();
    options.shade_smooth = (mode == GL_SMOOTH);
    m_rasterizer->set_options(options);
}

void GLContext::sync_light_state()
{
    if (!m_light_state_is_dirty)
        return;

    m_light_state_is_dirty = false;

    auto options = m_rasterizer->options();
    options.color_material_enabled = m_color_material_enabled;
    switch (m_color_material_face) {
    case GL_BACK:
        options.color_material_face = GPU::ColorMaterialFace::Back;
        break;
    case GL_FRONT:
        options.color_material_face = GPU::ColorMaterialFace::Front;
        break;
    case GL_FRONT_AND_BACK:
        options.color_material_face = GPU::ColorMaterialFace::FrontAndBack;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    switch (m_color_material_mode) {
    case GL_AMBIENT:
        options.color_material_mode = GPU::ColorMaterialMode::Ambient;
        break;
    case GL_AMBIENT_AND_DIFFUSE:
        options.color_material_mode = GPU::ColorMaterialMode::AmbientAndDiffuse;
        break;
    case GL_DIFFUSE:
        options.color_material_mode = GPU::ColorMaterialMode::Diffuse;
        break;
    case GL_EMISSION:
        options.color_material_mode = GPU::ColorMaterialMode::Emissive;
        break;
    case GL_SPECULAR:
        options.color_material_mode = GPU::ColorMaterialMode::Specular;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    m_rasterizer->set_options(options);

    for (auto light_id = 0u; light_id < m_device_info.num_lights; light_id++) {
        auto const& current_light_state = m_light_states.at(light_id);
        m_rasterizer->set_light_state(light_id, current_light_state);
    }

    m_rasterizer->set_material_state(GPU::Face::Front, m_material_states[Face::Front]);
    m_rasterizer->set_material_state(GPU::Face::Back, m_material_states[Face::Back]);
}

}
