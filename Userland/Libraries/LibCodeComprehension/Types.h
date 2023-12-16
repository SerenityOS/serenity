/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>

namespace CodeComprehension {

enum class Language {
    Unspecified,
    Cpp,
};

struct AutocompleteResultEntry {
    ByteString completion;
    size_t partial_input_length { 0 };
    // TODO: Actually assign the value of this field in more places (when applicable).
    Language language { Language::Unspecified };
    ByteString display_text {};

    enum class HideAutocompleteAfterApplying {
        No,
        Yes,
    };
    HideAutocompleteAfterApplying hide_autocomplete_after_applying { HideAutocompleteAfterApplying::Yes };
};

struct ProjectLocation {
    ByteString file;
    size_t line { 0 };
    size_t column { 0 };

    bool operator==(ProjectLocation const& other) const
    {
        return file == other.file && line == other.line && column == other.column;
    }
};

enum class DeclarationType {
    Function,
    Struct,
    Class,
    Variable,
    PreprocessorDefinition,
    Namespace,
    Member,
};

struct Declaration {
    ByteString name;
    ProjectLocation position;
    DeclarationType type;
    ByteString scope;

    bool operator==(Declaration const& other) const
    {
        return name == other.name && position == other.position && type == other.type && scope == other.scope;
    }
};

#define FOR_EACH_SEMANTIC_TYPE        \
    __SEMANTIC(Unknown)               \
    __SEMANTIC(Regular)               \
    __SEMANTIC(Keyword)               \
    __SEMANTIC(Type)                  \
    __SEMANTIC(Identifier)            \
    __SEMANTIC(String)                \
    __SEMANTIC(Number)                \
    __SEMANTIC(IncludePath)           \
    __SEMANTIC(PreprocessorStatement) \
    __SEMANTIC(Comment)               \
    __SEMANTIC(Whitespace)            \
    __SEMANTIC(Function)              \
    __SEMANTIC(Variable)              \
    __SEMANTIC(CustomType)            \
    __SEMANTIC(Namespace)             \
    __SEMANTIC(Member)                \
    __SEMANTIC(Parameter)             \
    __SEMANTIC(PreprocessorMacro)

struct TokenInfo {

    enum class SemanticType : u32 {
#define __SEMANTIC(x) x,
        FOR_EACH_SEMANTIC_TYPE
#undef __SEMANTIC

    } type { SemanticType::Unknown };
    size_t start_line { 0 };
    size_t start_column { 0 };
    size_t end_line { 0 };
    size_t end_column { 0 };

    static constexpr char const* type_to_string(SemanticType t)
    {
        switch (t) {
#define __SEMANTIC(x)     \
    case SemanticType::x: \
        return #x;
            FOR_EACH_SEMANTIC_TYPE
#undef __SEMANTIC
        }
        VERIFY_NOT_REACHED();
    }
};

struct TodoEntry {
    ByteString content;
    ByteString filename;
    size_t line { 0 };
    size_t column { 0 };
};

}
