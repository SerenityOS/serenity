/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGLSL/LinkedShader.h>
#include <LibGLSL/ObjectFile.h>

namespace GLSL {

class Linker final {
public:
    ErrorOr<NonnullOwnPtr<LinkedShader>> link(Vector<ObjectFile const*> const&);

    String messages() const { return m_messages; }

private:
    String m_messages;
};

}
