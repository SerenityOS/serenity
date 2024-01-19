/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/FlyString.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibXML/DOM/Node.h>

namespace JSSpecCompiler {

struct LogicalLocation : RefCounted<LogicalLocation> {
    String section;
    String step;
};

struct Location {
    StringView filename;
    size_t line = 0;
    size_t column = 0;
    RefPtr<LogicalLocation> logical_location;

    static Location global_scope() { return {}; }
};

class DiagnosticEngine {
    AK_MAKE_NONCOPYABLE(DiagnosticEngine);
    AK_MAKE_NONMOVABLE(DiagnosticEngine);

public:
    DiagnosticEngine() = default;

#define DEFINE_DIAGNOSTIC_FUNCTION(name_, level_)                                                                          \
    template<typename... Parameters>                                                                                       \
    void name_(Location const& location, AK::CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters) \
    {                                                                                                                      \
        add_diagnostic({                                                                                                   \
            .location = location,                                                                                          \
            .level = DiagnosticLevel::level_,                                                                              \
            .message = MUST(String::formatted(move(fmtstr), parameters...)),                                               \
        });                                                                                                                \
    }

    DEFINE_DIAGNOSTIC_FUNCTION(note, Note)
    DEFINE_DIAGNOSTIC_FUNCTION(warn, Warning)
    DEFINE_DIAGNOSTIC_FUNCTION(error, Error)
    DEFINE_DIAGNOSTIC_FUNCTION(fatal_error, FatalError)

#undef DEFINE_DIAGNOSTIC_FUNCTION

    bool has_fatal_errors() const;
    void print_diagnostics();

private:
    enum class DiagnosticLevel {
        Note,
        Warning,
        Error,
        FatalError,
    };

    enum class UseColor {
        No,
        Yes,
    };

    struct Diagnostic {
        Location location;
        DiagnosticLevel level;
        String message;

        Vector<Diagnostic> notes;

        bool operator<(Diagnostic const& other) const;

        void format_into(StringBuilder& builder, UseColor) const;
    };

    void add_diagnostic(Diagnostic&& diagnostic);

    Vector<Diagnostic> m_diagnostics;
    bool m_has_fatal_errors = false;
};

}
