/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/Shaders/Program.h>

namespace GL {

NonnullRefPtr<Program> Program::create()
{
    return adopt_ref(*new Program());
}

ErrorOr<void> Program::attach_shader(Shader&)
{
    TODO();
    return {};
}

ErrorOr<void> Program::link()
{
    TODO();
    return {};
}

}
