/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Cpp {
class Preprocessor {

public:
    explicit Preprocessor(const String& filename, const StringView& program);
    const String& process();
    const String& processed_text();
    Vector<StringView> included_paths() const { return m_included_paths; }

    struct DefinedValue {
        Optional<StringView> value;
        FlyString filename;
        size_t line { 0 };
        size_t column { 0 };
    };
    using Definitions = HashMap<StringView, DefinedValue>;

    const Definitions& definitions() const { return m_definitions; }

    void set_ignore_unsupported_keywords(bool ignore) { m_options.ignore_unsupported_keywords = ignore; }
    void set_keep_include_statements(bool keep) { m_options.keep_include_statements = keep; }

private:
    using PreprocessorKeyword = StringView;
    PreprocessorKeyword handle_preprocessor_line(const StringView&);
    void handle_preprocessor_keyword(const StringView& keyword, GenericLexer& line_lexer);

    Definitions m_definitions;
    const String m_filename;
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

    struct Options {
        bool ignore_unsupported_keywords { false };
        bool keep_include_statements { false };
    } m_options;
};
}
