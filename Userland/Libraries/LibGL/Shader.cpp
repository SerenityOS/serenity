/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <LibGL/GLContext.h>

namespace GL {

GLuint GLContext::gl_create_shader(GLenum shader_type)
{
    // FIXME: Add support for GL_COMPUTE_SHADER, GL_TESS_CONTROL_SHADER, GL_TESS_EVALUATION_SHADER and GL_GEOMETRY_SHADER.
    RETURN_VALUE_WITH_ERROR_IF(shader_type != GL_VERTEX_SHADER
            && shader_type != GL_FRAGMENT_SHADER,
        GL_INVALID_ENUM,
        0);

    GLuint shader_name;
    m_shader_name_allocator.allocate(1, &shader_name);
    auto shader = Shader::create(shader_type);
    m_allocated_shaders.set(shader_name, shader);
    return shader_name;
}

void GLContext::gl_delete_shader(GLuint shader)
{
    // "A value of 0 for shader will be silently ignored." (https://registry.khronos.org/OpenGL-Refpages/gl4/html/glDeleteShader.xhtml)
    if (shader == 0)
        return;

    auto it = m_allocated_shaders.find(shader);
    RETURN_WITH_ERROR_IF(it == m_allocated_shaders.end(), GL_INVALID_VALUE);

    // FIXME: According to the spec, we should only flag the shader for deletion here and delete it once it is detached from all programs.
    m_allocated_shaders.remove(it);
    m_shader_name_allocator.free(shader);
}

void GLContext::gl_shader_source(GLuint shader, GLsizei count, GLchar const** string, GLint const* length)
{
    auto it = m_allocated_shaders.find(shader);
    // FIXME: implement check "GL_INVALID_VALUE is generated if shader is not a value generated by OpenGL."
    RETURN_WITH_ERROR_IF(it == m_allocated_shaders.end(), GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(count < 0, GL_INVALID_VALUE);

    it->value->clear_sources();
    for (int i = 0; i < count; i++) {
        if (!length || length[i] < 0)
            it->value->add_source(StringView(string[i], strlen(string[i])));
        else
            it->value->add_source(StringView(string[i], length[i]));
    }
}

void GLContext::gl_compile_shader(GLuint shader)
{
    auto it = m_allocated_shaders.find(shader);
    // FIXME: implement check "GL_INVALID_VALUE is generated if shader is not a value generated by OpenGL."
    RETURN_WITH_ERROR_IF(it == m_allocated_shaders.end(), GL_INVALID_OPERATION);

    // NOTE: We are ignoring the compilation result here since it is tracked inside the shader object
    (void)it->value->compile();
}

GLuint GLContext::gl_create_program()
{
    GLuint program_name;
    m_program_name_allocator.allocate(1, &program_name);
    auto program = Program::create();
    m_allocated_programs.set(program_name, program);
    return program_name;
}

void GLContext::gl_delete_program(GLuint program)
{
    // "A value of 0 for program will be silently ignored." (https://registry.khronos.org/OpenGL-Refpages/gl4/html/glDeleteProgram.xhtml)
    if (program == 0)
        return;

    auto it = m_allocated_programs.find(program);
    RETURN_WITH_ERROR_IF(it == m_allocated_programs.end(), GL_INVALID_VALUE);

    // FIXME: According to the spec, we should only flag the program for deletion here and delete it once it is not used anymore.
    m_allocated_programs.remove(it);
    m_program_name_allocator.free(program);
}

void GLContext::gl_attach_shader(GLuint program, GLuint shader)
{
    auto program_it = m_allocated_programs.find(program);
    auto shader_it = m_allocated_shaders.find(shader);
    // FIXME: implement check "GL_INVALID_VALUE is generated if either program or shader is not a value generated by OpenGL."
    RETURN_WITH_ERROR_IF(program_it == m_allocated_programs.end(), GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(shader_it == m_allocated_shaders.end(), GL_INVALID_OPERATION);

    // NOTE: attach_result is Error if the shader is already attached to this program
    auto attach_result = program_it->value->attach_shader(*shader_it->value);
    RETURN_WITH_ERROR_IF(attach_result.is_error(), GL_INVALID_OPERATION);
}

void GLContext::gl_link_program(GLuint program)
{
    auto program_it = m_allocated_programs.find(program);
    // FIXME: implement check "GL_INVALID_VALUE is generated if program is not a value generated by OpenGL."
    RETURN_WITH_ERROR_IF(program_it == m_allocated_programs.end(), GL_INVALID_OPERATION);
    // FIXME: implement check "GL_INVALID_OPERATION is generated if program is the currently active program object and transform feedback mode is active."
    
    // NOTE: We are ignoring the link result since this is tracked inside the program object
    (void)program_it->value->link();
}

}
