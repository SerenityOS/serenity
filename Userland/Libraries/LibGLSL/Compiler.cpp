/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGLSL/Compiler.h>

namespace GLSL {

ErrorOr<NonnullOwnPtr<ObjectFile>> Compiler::compile(Vector<String> const&)
{
    // FIXME: implement this function
    m_messages = {};
    return try_make<ObjectFile>();
}

}
