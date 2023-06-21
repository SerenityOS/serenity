/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_call_list(GLuint list)
{
    if (m_gl_call_depth > max_allowed_gl_call_depth)
        return;

    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_call_list, list);

    if (m_listings.size() < list)
        return;

    TemporaryChange change { m_gl_call_depth, m_gl_call_depth + 1 };

    invoke_list(list);
}

void GLContext::gl_call_lists(GLsizei n, GLenum type, void const* lists)
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
        dbgln("GLContext FIXME: unimplemented glCallLists() with type {}", type);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void GLContext::gl_delete_lists(GLuint list, GLsizei range)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(range < 0, GL_INVALID_VALUE);

    if (m_listings.size() < list || m_listings.size() <= list + range)
        return;

    for (auto& entry : m_listings.span().slice(list - 1, range))
        entry.entries.clear_with_capacity();
}

void GLContext::gl_end_list()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!m_current_listing_index.has_value(), GL_INVALID_OPERATION);

    m_listings[m_current_listing_index->index] = move(m_current_listing_index->listing);
    m_current_listing_index.clear();
}

GLuint GLContext::gl_gen_lists(GLsizei range)
{
    RETURN_VALUE_WITH_ERROR_IF(range <= 0, GL_INVALID_VALUE, 0);
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, 0);

    auto initial_entry = m_listings.size();
    m_listings.resize(range + initial_entry);
    return initial_entry + 1;
}

GLboolean GLContext::gl_is_list(GLuint list)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, GL_FALSE);

    return list < m_listings.size() ? GL_TRUE : GL_FALSE;
}

void GLContext::gl_list_base(GLuint base)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_list_base, base);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_list_base = base;
}

void GLContext::gl_new_list(GLuint list, GLenum mode)
{
    RETURN_WITH_ERROR_IF(list == 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(mode != GL_COMPILE && mode != GL_COMPILE_AND_EXECUTE, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(m_current_listing_index.has_value(), GL_INVALID_OPERATION);

    if (m_listings.size() < list)
        return;

    m_current_listing_index = CurrentListing { {}, static_cast<size_t>(list - 1), mode };
}

void GLContext::invoke_list(size_t list_index)
{
    auto& listing = m_listings[list_index - 1];
    for (auto& entry : listing.entries) {
        entry.function.visit([&](auto& function) {
            entry.arguments.visit([&](auto& arguments) {
                auto apply = [&]<typename... Args>(Args&&... args) {
                    if constexpr (requires { (this->*function)(forward<Args>(args)...); })
                        (this->*function)(forward<Args>(args)...);
                };

                arguments.apply_as_args(apply);
            });
        });
    }
}

}
