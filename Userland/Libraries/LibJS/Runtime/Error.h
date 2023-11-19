/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/String.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/SourceRange.h>

namespace JS {

struct TracebackFrame {
    DeprecatedFlyString function_name;
    [[nodiscard]] SourceRange const& source_range() const;

    mutable Variant<SourceRange, UnrealizedSourceRange> source_range_storage;
};

enum CompactTraceback {
    No,
    Yes,
};

class Error : public Object {
    JS_OBJECT(Error, Object);
    JS_DECLARE_ALLOCATOR(Error);

public:
    static NonnullGCPtr<Error> create(Realm&);
    static NonnullGCPtr<Error> create(Realm&, String message);
    static NonnullGCPtr<Error> create(Realm&, StringView message);

    virtual ~Error() override = default;

    [[nodiscard]] String stack_string(CompactTraceback compact = CompactTraceback::No) const;

    ThrowCompletionOr<void> install_error_cause(Value options);

    Vector<TracebackFrame, 32> const& traceback() const { return m_traceback; }

protected:
    explicit Error(Object& prototype);

private:
    void populate_stack();
    Vector<TracebackFrame, 32> m_traceback;
};

// NOTE: Making these inherit from Error is not required by the spec but
//       our way of implementing the [[ErrorData]] internal slot, which is
//       used in Object.prototype.toString().
#define DECLARE_NATIVE_ERROR(ClassName, snake_name, PrototypeName, ConstructorName) \
    class ClassName final : public Error {                                          \
        JS_OBJECT(ClassName, Error);                                                \
        JS_DECLARE_ALLOCATOR(ClassName);                                            \
                                                                                    \
    public:                                                                         \
        static NonnullGCPtr<ClassName> create(Realm&);                              \
        static NonnullGCPtr<ClassName> create(Realm&, String message);              \
        static NonnullGCPtr<ClassName> create(Realm&, StringView message);          \
                                                                                    \
        explicit ClassName(Object& prototype);                                      \
        virtual ~ClassName() override = default;                                    \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    DECLARE_NATIVE_ERROR(ClassName, snake_name, PrototypeName, ConstructorName)
JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE
}
