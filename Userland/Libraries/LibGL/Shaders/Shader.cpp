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

}
