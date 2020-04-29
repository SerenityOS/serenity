/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/JsonObject.h>
#include <AK/StreamJsonBuilder.h>

namespace AK {

void StreamJsonBuilder::document_started()
{
}

void StreamJsonBuilder::document_parsed()
{
}

void StreamJsonBuilder::object_started()
{
    m_object_stack.append(JsonObject {});
}

void StreamJsonBuilder::object_parsed()
{
    auto object = m_object_stack.take_last();

    auto discard { false };

    if (apply_streams(object) == VisitDecision::Discard)
        discard = true;

    if (m_object_stack.size() == 0)
        return;

    handle_insertion(move(object), discard);
}

void StreamJsonBuilder::array_started()
{
    m_object_stack.append(JsonArray {});
    m_path.append(JsonPathElement { 0 });
}

void StreamJsonBuilder::array_parsed()
{
    auto array = m_object_stack.take_last();
    m_path.take_last();

    auto discard { false };

    if (apply_streams(array) == VisitDecision::Discard)
        discard = true;

    if (m_object_stack.size() == 0) {
        return;
    }

    handle_insertion(move(array), discard);
}

void StreamJsonBuilder::key_parsed(String key)
{
    m_path.append(JsonPathElement { key });
}

void StreamJsonBuilder::value_parsed(JsonValue&& value)
{
    ASSERT(m_object_stack.size());
    ASSERT(m_path.size());

    auto discard { false };

    if (apply_streams(value) == VisitDecision::Discard)
        discard = true;

    handle_insertion(move(value), discard);
}

void StreamJsonBuilder::handle_insertion(JsonValue&& value, bool discard)
{
    auto& parent = m_object_stack.last();
    auto last_path_segment = m_path.take_last();
    if (last_path_segment.kind() == JsonPathElement::Kind::Key) {
        ASSERT(parent.is_object());
        if (!discard)
            parent.as_object().set(last_path_segment.key(), move(value));
    } else {
        if (!parent.is_array())
            dbg() << parent.to_string();
        ASSERT(parent.is_array());
        if (!discard)
            parent.as_array().append(move(value));
        m_path.append(JsonPathElement { last_path_segment.index() + 1 });
    }
}

StreamJsonBuilder::VisitDecision StreamJsonBuilder::apply_streams(const JsonValue& value)
{
    auto final_decision = VisitDecision::LeaveAlone;

    for (auto& callback : m_callbacks) {
        if (m_path == callback.path) {
            auto decision = callback.function(value);
            if (decision == VisitDecision::Stop) {
                decision = VisitDecision::LeaveAlone;
                m_stop = true;
            }
            // store if any of the callbacks ask for it
            if (decision == VisitDecision::Store)
                final_decision = decision;

            if (final_decision != VisitDecision::Store) {
                final_decision = decision;
            }
        }
    }

    return final_decision;
}

}
