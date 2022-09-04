/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::DOM {

#define TRY_OR_RETURN_OOM(global_object, expression)                             \
    ({                                                                           \
        auto _temporary_result = (expression);                                   \
        if (_temporary_result.is_error()) {                                      \
            VERIFY(_temporary_result.error().code() == ENOMEM);                  \
            return DOM::UnknownError::create(global_object, "Out of memory."sv); \
        }                                                                        \
        _temporary_result.release_value();                                       \
    })

// The following have a legacy code value but *don't* produce it as
// DOMException.code value when used as name (and are therefore omitted here):
// - DOMStringSizeError (DOMSTRING_SIZE_ERR = 2)
// - NoDataAllowedError (NO_DATA_ALLOWED_ERR = 6)
// - ValidationError (VALIDATION_ERR = 16)
#define ENUMERATE_DOM_EXCEPTION_LEGACY_CODES   \
    __ENUMERATE(IndexSizeError, 1)             \
    __ENUMERATE(HierarchyRequestError, 3)      \
    __ENUMERATE(WrongDocumentError, 4)         \
    __ENUMERATE(InvalidCharacterError, 5)      \
    __ENUMERATE(NoModificationAllowedError, 7) \
    __ENUMERATE(NotFoundError, 8)              \
    __ENUMERATE(NotSupportedError, 9)          \
    __ENUMERATE(InUseAttributeError, 10)       \
    __ENUMERATE(InvalidStateError, 11)         \
    __ENUMERATE(SyntaxError, 12)               \
    __ENUMERATE(InvalidModificationError, 13)  \
    __ENUMERATE(NamespaceError, 14)            \
    __ENUMERATE(InvalidAccessError, 15)        \
    __ENUMERATE(TypeMismatchError, 17)         \
    __ENUMERATE(SecurityError, 18)             \
    __ENUMERATE(NetworkError, 19)              \
    __ENUMERATE(AbortError, 20)                \
    __ENUMERATE(URLMismatchError, 21)          \
    __ENUMERATE(QuotaExceededError, 22)        \
    __ENUMERATE(TimeoutError, 23)              \
    __ENUMERATE(InvalidNodeTypeError, 24)      \
    __ENUMERATE(DataCloneError, 25)

// https://webidl.spec.whatwg.org/#idl-DOMException-error-names
// Same order as in the spec document, also matches the legacy codes order above.
#define ENUMERATE_DOM_EXCEPTION_ERROR_NAMES          \
    __ENUMERATE(IndexSizeError) /* Deprecated */     \
    __ENUMERATE(HierarchyRequestError)               \
    __ENUMERATE(WrongDocumentError)                  \
    __ENUMERATE(InvalidCharacterError)               \
    __ENUMERATE(NoModificationAllowedError)          \
    __ENUMERATE(NotFoundError)                       \
    __ENUMERATE(NotSupportedError)                   \
    __ENUMERATE(InUseAttributeError)                 \
    __ENUMERATE(InvalidStateError)                   \
    __ENUMERATE(SyntaxError)                         \
    __ENUMERATE(InvalidModificationError)            \
    __ENUMERATE(NamespaceError)                      \
    __ENUMERATE(InvalidAccessError) /* Deprecated */ \
    __ENUMERATE(TypeMismatchError)  /* Deprecated */ \
    __ENUMERATE(SecurityError)                       \
    __ENUMERATE(NetworkError)                        \
    __ENUMERATE(AbortError)                          \
    __ENUMERATE(URLMismatchError)                    \
    __ENUMERATE(QuotaExceededError)                  \
    __ENUMERATE(TimeoutError)                        \
    __ENUMERATE(InvalidNodeTypeError)                \
    __ENUMERATE(DataCloneError)                      \
    __ENUMERATE(EncodingError)                       \
    __ENUMERATE(NotReadableError)                    \
    __ENUMERATE(UnknownError)                        \
    __ENUMERATE(ConstraintError)                     \
    __ENUMERATE(DataError)                           \
    __ENUMERATE(TransactionInactiveError)            \
    __ENUMERATE(ReadOnlyError)                       \
    __ENUMERATE(VersionError)                        \
    __ENUMERATE(OperationError)                      \
    __ENUMERATE(NotAllowedError)

static u16 get_legacy_code_for_name(FlyString const& name)
{
#define __ENUMERATE(ErrorName, code) \
    if (name == #ErrorName)          \
        return code;
    ENUMERATE_DOM_EXCEPTION_LEGACY_CODES
#undef __ENUMERATE
    return 0;
}

// https://webidl.spec.whatwg.org/#idl-DOMException
class DOMException final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMException, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<DOMException> create(JS::Object& global_object, FlyString const& name, FlyString const& message);

    // JS constructor has message first, name second
    // FIXME: This is a completely pointless footgun, let's use the same order for both factories.
    static JS::NonnullGCPtr<DOMException> create_with_global_object(JS::Object& global_object, FlyString const& message, FlyString const& name);

    static JS::NonnullGCPtr<DOMException> create(JS::Realm& realm, FlyString const& message);

    virtual ~DOMException() override;

    FlyString const& name() const { return m_name; }
    FlyString const& message() const { return m_message; }
    u16 code() const { return get_legacy_code_for_name(m_name); }

protected:
    DOMException(HTML::Window&, FlyString const& name, FlyString const& message);

private:
    FlyString m_name;
    FlyString m_message;
};

#define __ENUMERATE(ErrorName)                                                                            \
    class ErrorName final {                                                                               \
    public:                                                                                               \
        static JS::NonnullGCPtr<DOMException> create(JS::Object& global_object, FlyString const& message) \
        {                                                                                                 \
            return DOMException::create(global_object, #ErrorName, message);                              \
        }                                                                                                 \
    };
ENUMERATE_DOM_EXCEPTION_ERROR_NAMES
#undef __ENUMERATE

}

WRAPPER_HACK(DOMException, Web::DOM)

namespace Web {

inline JS::Completion throw_completion(JS::NonnullGCPtr<DOM::DOMException> exception)
{
    return JS::throw_completion(JS::Value(static_cast<JS::Object*>(exception.ptr())));
}

}
