/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

namespace Detail {
template<auto condition, typename T>
struct ConditionallyResultType;

template<typename T>
struct ConditionallyResultType<true, T> {
    using Type = typename T::ResultType;
};

template<typename T>
struct ConditionallyResultType<false, T> {
    using Type = T;
};
}

template<auto condition, typename T>
using ConditionallyResultType = typename Detail::ConditionallyResultType<condition, T>::Type;

// NOTE: If you're here because of an internal compiler error in GCC 10.3.0+,
//       it's because of the following bug:
//
//       https://gcc.gnu.org/bugzilla/show_bug.cgi?id=96745
//
//       Make sure you didn't accidentally make your destructor private before
//       you start bug hunting. :^)

template<typename>
class Optional;

struct OptionalNone {
    explicit OptionalNone() = default;
};

template<typename T>
requires(!IsLvalueReference<T>) class [[nodiscard]] Optional<T> {
    template<typename U>
    friend class Optional;

    static_assert(!IsLvalueReference<T> && !IsRvalueReference<T>);

public:
    using ValueType = T;

    ALWAYS_INLINE Optional() = default;

    template<SameAs<OptionalNone> V>
    Optional(V) { }

    template<SameAs<OptionalNone> V>
    Optional& operator=(V)
    {
        clear();
        return *this;
    }

    Optional(Optional const& other)
    requires(!IsCopyConstructible<T>)
    = delete;
    Optional(Optional const& other) = default;

    Optional(Optional&& other)
    requires(!IsMoveConstructible<T>)
    = delete;

    Optional& operator=(Optional const&)
    requires(!IsCopyConstructible<T> || !IsDestructible<T>)
    = delete;
    Optional& operator=(Optional const&) = default;

    Optional& operator=(Optional&& other)
    requires(!IsMoveConstructible<T> || !IsDestructible<T>)
    = delete;

    ~Optional()
    requires(!IsDestructible<T>)
    = delete;
    ~Optional() = default;

    ALWAYS_INLINE Optional(Optional const& other)
    requires(!IsTriviallyCopyConstructible<T>)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.value());
    }

    ALWAYS_INLINE Optional(Optional&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.release_value());
    }

    template<typename U>
    requires(IsConstructible<T, U const&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional>) ALWAYS_INLINE explicit Optional(Optional<U> const& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.value());
    }

    template<typename U>
    requires(IsConstructible<T, U &&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional>) ALWAYS_INLINE explicit Optional(Optional<U>&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.release_value());
    }

    template<typename U = T>
    requires(!IsSame<OptionalNone, RemoveCVReference<U>>)
    ALWAYS_INLINE explicit(!IsConvertible<U&&, T>) Optional(U&& value)
    requires(!IsSame<RemoveCVReference<U>, Optional<T>> && IsConstructible<T, U &&>)
        : m_has_value(true)
    {
        new (&m_storage) T(forward<U>(value));
    }

    ALWAYS_INLINE Optional& operator=(Optional const& other)
    requires(!IsTriviallyCopyConstructible<T> || !IsTriviallyDestructible<T>)
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (other.has_value()) {
                new (&m_storage) T(other.value());
            }
        }
        return *this;
    }

    ALWAYS_INLINE Optional& operator=(Optional&& other)
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (other.has_value()) {
                new (&m_storage) T(other.release_value());
            }
        }
        return *this;
    }

    template<typename O>
    ALWAYS_INLINE bool operator==(Optional<O> const& other) const
    {
        return has_value() == other.has_value() && (!has_value() || value() == other.value());
    }

    template<typename O>
    ALWAYS_INLINE bool operator==(O const& other) const
    {
        return has_value() && value() == other;
    }

    ALWAYS_INLINE ~Optional()
    requires(!IsTriviallyDestructible<T>)
    {
        clear();
    }

    ALWAYS_INLINE void clear()
    {
        if (m_has_value) {
            value().~T();
            m_has_value = false;
        }
    }

    template<typename... Parameters>
    ALWAYS_INLINE void emplace(Parameters&&... parameters)
    {
        clear();
        m_has_value = true;
        new (&m_storage) T(forward<Parameters>(parameters)...);
    }

    template<typename Callable>
    ALWAYS_INLINE void lazy_emplace(Callable callable)
    {
        clear();
        m_has_value = true;
        new (&m_storage) T { callable() };
    }

    [[nodiscard]] ALWAYS_INLINE bool has_value() const { return m_has_value; }

    [[nodiscard]] ALWAYS_INLINE T& value() &
    {
        VERIFY(m_has_value);
        return *__builtin_launder(reinterpret_cast<T*>(&m_storage));
    }

    [[nodiscard]] ALWAYS_INLINE T const& value() const&
    {
        VERIFY(m_has_value);
        return *__builtin_launder(reinterpret_cast<T const*>(&m_storage));
    }

    [[nodiscard]] ALWAYS_INLINE T value() &&
    {
        return release_value();
    }

    [[nodiscard]] ALWAYS_INLINE T release_value()
    {
        VERIFY(m_has_value);
        T released_value = move(value());
        value().~T();
        m_has_value = false;
        return released_value;
    }

    [[nodiscard]] ALWAYS_INLINE T value_or(T const& fallback) const&
    {
        if (m_has_value)
            return value();
        return fallback;
    }

    [[nodiscard]] ALWAYS_INLINE T value_or(T&& fallback) &&
    {
        if (m_has_value)
            return move(value());
        return move(fallback);
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE T value_or_lazy_evaluated(Callback callback) const
    {
        if (m_has_value)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE Optional<T> value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_has_value)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE ErrorOr<T> try_value_or_lazy_evaluated(Callback callback) const
    {
        if (m_has_value)
            return value();
        return TRY(callback());
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE ErrorOr<Optional<T>> try_value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_has_value)
            return value();
        return TRY(callback());
    }

    ALWAYS_INLINE T const& operator*() const { return value(); }
    ALWAYS_INLINE T& operator*() { return value(); }

    ALWAYS_INLINE T const* operator->() const { return &value(); }
    ALWAYS_INLINE T* operator->() { return &value(); }

    template<typename F, typename MappedType = decltype(declval<F>()(declval<T&>())), auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>, typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>>
    ALWAYS_INLINE Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper)
    {
        if constexpr (IsErrorOr) {
            if (m_has_value)
                return OptionalType { TRY(mapper(value())) };
            return OptionalType {};
        } else {
            if (m_has_value)
                return OptionalType { mapper(value()) };

            return OptionalType {};
        }
    }

    template<typename F, typename MappedType = decltype(declval<F>()(declval<T&>())), auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>, typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>>
    ALWAYS_INLINE Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper) const
    {
        if constexpr (IsErrorOr) {
            if (m_has_value)
                return OptionalType { TRY(mapper(value())) };
            return OptionalType {};
        } else {
            if (m_has_value)
                return OptionalType { mapper(value()) };

            return OptionalType {};
        }
    }

private:
    alignas(T) u8 m_storage[sizeof(T)];
    bool m_has_value { false };
};

template<typename T>
requires(IsLvalueReference<T>) class [[nodiscard]] Optional<T> {
    template<typename>
    friend class Optional;

    template<typename U>
    constexpr static bool CanBePlacedInOptional = IsSame<RemoveReference<T>, RemoveReference<AddConstToReferencedType<U>>> && (IsBaseOf<RemoveCVReference<T>, RemoveCVReference<U>> || IsSame<RemoveCVReference<T>, RemoveCVReference<U>>);

public:
    using ValueType = T;

    ALWAYS_INLINE Optional() = default;

    template<SameAs<OptionalNone> V>
    Optional(V) { }

    template<SameAs<OptionalNone> V>
    Optional& operator=(V)
    {
        clear();
        return *this;
    }

    template<typename U = T>
    ALWAYS_INLINE Optional(U& value)
    requires(CanBePlacedInOptional<U&>)
        : m_pointer(&value)
    {
    }

    ALWAYS_INLINE Optional(RemoveReference<T>& value)
        : m_pointer(&value)
    {
    }

    ALWAYS_INLINE Optional(Optional const& other)
        : m_pointer(other.m_pointer)
    {
    }

    ALWAYS_INLINE Optional(Optional&& other)
        : m_pointer(other.m_pointer)
    {
        other.m_pointer = nullptr;
    }

    template<typename U>
    ALWAYS_INLINE Optional(Optional<U> const& other)
    requires(CanBePlacedInOptional<U>)
        : m_pointer(other.m_pointer)
    {
    }

    template<typename U>
    ALWAYS_INLINE Optional(Optional<U>&& other)
    requires(CanBePlacedInOptional<U>)
        : m_pointer(other.m_pointer)
    {
        other.m_pointer = nullptr;
    }

    ALWAYS_INLINE Optional& operator=(Optional const& other)
    {
        m_pointer = other.m_pointer;
        return *this;
    }

    ALWAYS_INLINE Optional& operator=(Optional&& other)
    {
        m_pointer = other.m_pointer;
        other.m_pointer = nullptr;
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE Optional& operator=(Optional<U> const& other)
    requires(CanBePlacedInOptional<U>)
    {
        m_pointer = other.m_pointer;
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE Optional& operator=(Optional<U>&& other)
    requires(CanBePlacedInOptional<U>)
    {
        m_pointer = other.m_pointer;
        other.m_pointer = nullptr;
        return *this;
    }

    // Note: Disallows assignment from a temporary as this does not do any lifetime extension.
    template<typename U>
    requires(!IsSame<OptionalNone, RemoveCVReference<U>>)
    ALWAYS_INLINE Optional& operator=(U&& value)
    requires(CanBePlacedInOptional<U> && IsLvalueReference<U>)
    {
        m_pointer = &value;
        return *this;
    }

    ALWAYS_INLINE void clear()
    {
        m_pointer = nullptr;
    }

    [[nodiscard]] ALWAYS_INLINE bool has_value() const { return m_pointer != nullptr; }

    [[nodiscard]] ALWAYS_INLINE T value()
    {
        VERIFY(m_pointer);
        return *m_pointer;
    }

    [[nodiscard]] ALWAYS_INLINE AddConstToReferencedType<T> value() const
    {
        VERIFY(m_pointer);
        return *m_pointer;
    }

    template<typename U>
    requires(IsBaseOf<RemoveCVReference<T>, U>) [[nodiscard]] ALWAYS_INLINE AddConstToReferencedType<T> value_or(U& fallback) const
    {
        if (m_pointer)
            return value();
        return fallback;
    }

    // Note that this ends up copying the value.
    [[nodiscard]] ALWAYS_INLINE RemoveCVReference<T> value_or(RemoveCVReference<T> fallback) const
    {
        if (m_pointer)
            return value();
        return fallback;
    }

    [[nodiscard]] ALWAYS_INLINE T release_value()
    {
        return *exchange(m_pointer, nullptr);
    }

    template<typename U>
    ALWAYS_INLINE bool operator==(Optional<U> const& other) const
    {
        return has_value() == other.has_value() && (!has_value() || value() == other.value());
    }

    template<typename U>
    ALWAYS_INLINE bool operator==(U const& other) const
    {
        return has_value() && value() == other;
    }

    ALWAYS_INLINE AddConstToReferencedType<T> operator*() const { return value(); }
    ALWAYS_INLINE T operator*() { return value(); }

    ALWAYS_INLINE RawPtr<AddConst<RemoveReference<T>>> operator->() const { return &value(); }
    ALWAYS_INLINE RawPtr<RemoveReference<T>> operator->() { return &value(); }

    // Conversion operators from Optional<T&> -> Optional<T>
    ALWAYS_INLINE operator Optional<RemoveCVReference<T>>() const
    {
        if (has_value())
            return Optional<RemoveCVReference<T>>(value());
        return {};
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE T value_or_lazy_evaluated(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE Optional<T> value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE ErrorOr<T> try_value_or_lazy_evaluated(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return TRY(callback());
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE ErrorOr<Optional<T>> try_value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return TRY(callback());
    }

    template<typename F, typename MappedType = decltype(declval<F>()(declval<T&>())), auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>, typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>>
    ALWAYS_INLINE Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper)
    {
        if constexpr (IsErrorOr) {
            if (m_pointer != nullptr)
                return OptionalType { TRY(mapper(value())) };
            return OptionalType {};
        } else {
            if (m_pointer != nullptr)
                return OptionalType { mapper(value()) };

            return OptionalType {};
        }
    }

    template<typename F, typename MappedType = decltype(declval<F>()(declval<T&>())), auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>, typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>>
    ALWAYS_INLINE Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper) const
    {
        if constexpr (IsErrorOr) {
            if (m_pointer != nullptr)
                return OptionalType { TRY(mapper(value())) };
            return OptionalType {};
        } else {
            if (m_pointer != nullptr)
                return OptionalType { mapper(value()) };

            return OptionalType {};
        }
    }

private:
    RemoveReference<T>* m_pointer { nullptr };
};

}

#if USING_AK_GLOBALLY
using AK::Optional;
using AK::OptionalNone;
#endif
