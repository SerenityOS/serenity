/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/String.h>
#include <AK/Traits.h>
#include <Kernel/KeyCode.h>

class GShortcut {
public:
    GShortcut() {}
    GShortcut(u8 modifiers, KeyCode key)
        : m_modifiers(modifiers)
        , m_key(key)
    {
    }

    bool is_valid() const { return m_key != KeyCode::Key_Invalid; }
    u8 modifiers() const { return m_modifiers; }
    KeyCode key() const { return m_key; }
    String to_string() const;

    bool operator==(const GShortcut& other) const
    {
        return m_modifiers == other.m_modifiers
            && m_key == other.m_key;
    }

private:
    u8 m_modifiers { 0 };
    KeyCode m_key { KeyCode::Key_Invalid };
};

namespace AK {

template<>
struct Traits<GShortcut> : public GenericTraits<GShortcut> {
    static unsigned hash(const GShortcut& shortcut)
    {
        return pair_int_hash(shortcut.modifiers(), shortcut.key());
    }
};

}
