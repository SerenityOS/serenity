/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGLSL/Linker.h>

namespace GLSL {

ErrorOr<NonnullOwnPtr<LinkedShader>> Linker::link(Vector<ObjectFile const*> const&)
{
    // FIXME: implement this function
    m_messages = String::empty();
    return adopt_own(*new LinkedShader());
}

}
