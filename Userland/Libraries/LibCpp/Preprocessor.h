/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Cpp {
class Preprocessor {

public:
    explicit Preprocessor(const StringView&);
    const String& process();
    const String& processed_text();
    Vector<StringView> included_paths() const { return m_included_paths; }

    struct DefinedValue {
        Optional<StringView> value;
    };
    using Definitions = HashMap<StringView, DefinedValue>;

    const Definitions& definitions() const { return m_definitions; }

private:
    void handle_preprocessor_line(const StringView&);

    Definitions m_definitions;
    const StringView m_program;
    StringBuilder m_builder;
    Vector<StringView> m_lines;
    size_t m_line_index { 0 };
    size_t m_current_depth { 0 };
    Vector<size_t> m_depths_of_taken_branches;
    Vector<size_t> m_depths_of_not_taken_branches;

    enum class State {
        Normal,
        SkipIfBranch,
        SkipElseBranch
    };
    State m_state { State::Normal };

    Vector<StringView> m_included_paths;
    String m_processed_text;
};
}
