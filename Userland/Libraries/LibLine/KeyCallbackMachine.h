/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Line {

class Editor;

struct Key {
    enum Modifier : int {
        None = 0,
        Alt = 1,
    };

    int modifiers { None };
    unsigned key { 0 };

    Key(unsigned c)
        : modifiers(None)
        , key(c) {};

    Key(unsigned c, int modifiers)
        : modifiers(modifiers)
        , key(c)
    {
    }

    bool operator==(const Key& other) const
    {
        return other.key == key && other.modifiers == modifiers;
    }

    bool operator!=(const Key& other) const
    {
        return !(*this == other);
    }
};

struct KeyCallback {
    KeyCallback(Function<bool(Editor&)> cb)
        : callback(move(cb))
    {
    }
    Function<bool(Editor&)> callback;
};

class KeyCallbackMachine {
public:
    void register_key_input_callback(Vector<Key>, Function<bool(Editor&)> callback);
    void key_pressed(Editor&, Key);
    void interrupted(Editor&);
    bool should_process_last_pressed_key() const { return m_should_process_this_key; }

private:
    HashMap<Vector<Key>, NonnullOwnPtr<KeyCallback>> m_key_callbacks;
    Vector<Vector<Key>> m_current_matching_keys;
    size_t m_sequence_length { 0 };
    bool m_should_process_this_key { true };
};

}

namespace AK {

template<>
struct Traits<Line::Key> : public GenericTraits<Line::Key> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(Line::Key k) { return pair_int_hash(k.key, k.modifiers); }
};

template<>
struct Traits<Vector<Line::Key>> : public GenericTraits<Vector<Line::Key>> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(const Vector<Line::Key>& ks)
    {
        unsigned h = 0;
        for (auto& k : ks)
            h ^= Traits<Line::Key>::hash(k);
        return h;
    }
};

}
