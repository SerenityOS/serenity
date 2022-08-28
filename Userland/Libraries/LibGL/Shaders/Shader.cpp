/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/Shaders/Shader.h>

namespace GL {

NonnullRefPtr<Shader> Shader::create(GLenum shader_type)
{
    return adopt_ref(*new Shader(shader_type));
}

ErrorOr<void> Shader::compile()
{
    TODO();
    return {};
}

}
