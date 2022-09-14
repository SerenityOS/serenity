/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>

namespace GPU {

class Shader : public RefCounted<Shader> {
public:
    Shader(void const* ownership_token)
        : m_ownership_token { ownership_token }
    {
    }

    virtual ~Shader() = default;

    void const* ownership_token() const { return m_ownership_token; }
    bool has_same_ownership_token(Shader const& other) const { return other.ownership_token() == ownership_token(); }

private:
    void const* const m_ownership_token { nullptr };
};

}
