/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

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

static u16 get_legacy_code_for_name(const FlyString& name)
{
#define __ENUMERATE(ErrorName, code) \
    if (name == #ErrorName)          \
        return code;
    ENUMERATE_DOM_EXCEPTION_LEGACY_CODES
#undef __ENUMERATE
    return 0;
}

// https://webidl.spec.whatwg.org/#idl-DOMException
class DOMException final
    : public RefCounted<DOMException>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMExceptionWrapper;

    static NonnullRefPtr<DOMException> create(const FlyString& name, const FlyString& message)
    {
        return adopt_ref(*new DOMException(name, message));
    }

    // JS constructor has message first, name second
    static NonnullRefPtr<DOMException> create_with_global_object(Bindings::WindowObject&, const FlyString& message, const FlyString& name)
    {
        return adopt_ref(*new DOMException(name, message));
    }

    const FlyString& name() const { return m_name; }
    const FlyString& message() const { return m_message; }
    u16 code() const { return get_legacy_code_for_name(m_name); }

protected:
    DOMException(const FlyString& name, const FlyString& message)
        : m_name(name)
        , m_message(message)
    {
    }

private:
    FlyString m_name;
    FlyString m_message;
};

#define __ENUMERATE(ErrorName)                                              \
    class ErrorName final {                                                 \
    public:                                                                 \
        static NonnullRefPtr<DOMException> create(const FlyString& message) \
        {                                                                   \
            return DOMException::create(#ErrorName, message);               \
        }                                                                   \
    };
ENUMERATE_DOM_EXCEPTION_ERROR_NAMES
#undef __ENUMERATE

}
