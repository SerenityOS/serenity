/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGL/GL/glplatform.h>

namespace GL {

class Shader final : public RefCounted<Shader> {
public:
    static NonnullRefPtr<Shader> create(GLenum shader_type);

    void clear_sources() { m_sources.clear(); }
    ErrorOr<void> add_source(StringView source_code);
    ErrorOr<void> compile();
    GLenum type() const { return m_type; }

private:
    explicit Shader(GLenum shader_type)
        : m_type { shader_type }
    {
    }

    Vector<String> m_sources;
    GLenum m_type;
};

}
