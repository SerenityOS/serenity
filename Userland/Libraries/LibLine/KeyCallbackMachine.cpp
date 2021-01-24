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

#include <AK/Debug.h>
#include <LibLine/Editor.h>

namespace {
constexpr u32 ctrl(char c) { return c & 0x3f; }
}

namespace Line {

void KeyCallbackMachine::register_key_input_callback(Vector<Key> keys, Function<bool(Editor&)> callback)
{
    m_key_callbacks.set(keys, make<KeyCallback>(move(callback)));
}

void KeyCallbackMachine::key_pressed(Editor& editor, Key key)
{
#if CALLBACK_MACHINE_DEBUG
    dbgln("Key<{}, {}> pressed, seq_length={}, {} things in the matching vector", key.key, key.modifiers, m_sequence_length, m_current_matching_keys.size());
#endif
    if (m_sequence_length == 0) {
        ASSERT(m_current_matching_keys.is_empty());

        for (auto& it : m_key_callbacks) {
            if (it.key.first() == key)
                m_current_matching_keys.append(it.key);
        }

        if (m_current_matching_keys.is_empty()) {
            m_should_process_this_key = true;
            return;
        }
    }

    ++m_sequence_length;
    Vector<Vector<Key>> old_macthing_keys;
    swap(m_current_matching_keys, old_macthing_keys);

    for (auto& okey : old_macthing_keys) {
        if (okey.size() < m_sequence_length)
            continue;

        if (okey[m_sequence_length - 1] == key)
            m_current_matching_keys.append(okey);
    }

    if (m_current_matching_keys.is_empty()) {
        // Insert any keys that were captured
        if (!old_macthing_keys.is_empty()) {
            auto& keys = old_macthing_keys.first();
            for (size_t i = 0; i < m_sequence_length - 1; ++i)
                editor.insert(keys[i].key);
        }
        m_sequence_length = 0;
        m_should_process_this_key = true;
        return;
    }

#if CALLBACK_MACHINE_DEBUG
    dbgln("seq_length={}, matching vector:", m_sequence_length);
    for (auto& key : m_current_matching_keys) {
        for (auto& k : key)
            dbgln("    {}, {}", k.key, k.modifiers);
        dbgln("");
    }
#endif

    m_should_process_this_key = false;
    for (auto& key : m_current_matching_keys) {
        if (key.size() == m_sequence_length) {
            m_should_process_this_key = m_key_callbacks.get(key).value()->callback(editor);
            m_sequence_length = 0;
            m_current_matching_keys.clear();
            return;
        }
    }
}

void KeyCallbackMachine::interrupted(Editor& editor)
{
    m_sequence_length = 0;
    m_current_matching_keys.clear();
    if (auto callback = m_key_callbacks.get({ ctrl('C') }); callback.has_value())
        m_should_process_this_key = callback.value()->callback(editor);
    else
        m_should_process_this_key = true;
}

}
