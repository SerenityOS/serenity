/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGL/Shaders/Shader.h>

namespace GL {

NonnullRefPtr<Shader> Shader::create(GLenum shader_type)
{
    return adopt_ref(*new Shader(shader_type));
}

ErrorOr<void> Shader::add_source(StringView source_code)
{
    auto source_code_content = TRY(String::from_utf8(source_code));
    TRY(m_sources.try_append(move(source_code_content)));
    return {};
}

ErrorOr<void> Shader::compile()
{
    // FIXME: Implement actual shader compilation
    m_compile_status = true;
    return {};
}

size_t Shader::info_log_length() const
{
    if (!m_info_log.has_value())
        return 0;

    // Per the spec we return the size including the null terminator
    return m_info_log.value().bytes().size() + 1;
}

size_t Shader::combined_source_length() const
{
    if (m_sources.is_empty())
        return 0;

    size_t combined_size = 0;
    for (auto source : m_sources)
        combined_size += source.bytes().size();

    // Per the spec we return the size including the null terminator
    return combined_size + 1;
}

}
