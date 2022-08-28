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
#include <LibGLSL/ObjectFile.h>

namespace GLSL {

class Compiler final {
public:
    ErrorOr<NonnullOwnPtr<ObjectFile>> compile(Vector<String> const& sources);

    String messages() const { return m_messages; }

private:
    String m_messages;
};

}
