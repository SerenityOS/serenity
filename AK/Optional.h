/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Concepts.h>
#include <AK/Forward.h>
#include <AK/Platform.h>
#include <AK/StdLibExtraDetails.h>
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
    explicit constexpr OptionalNone() = default;
};

template<typename T>
requires(!IsLvalueReference<T>) class [[nodiscard]] Optional<T> {
    template<typename U>
    friend class Optional;

    static_assert(!IsLvalueReference<T> && !IsRvalueReference<T>);

public:
    using ValueType = T;

    // FIXME: Not sure why this constructor is needed,
    //        as it essentially does the same thing as the default constructor,
    //        as the union already has an initialized member,
    //        but for some reason compilers assert having default constructor
    //        on the value type
    ALWAYS_INLINE constexpr Optional()
    requires(!IsConstructible<T>)
    {
    }

    ALWAYS_INLINE constexpr Optional() = default;

    template<SameAs<OptionalNone> V>
    constexpr Optional(V) { }

    template<SameAs<OptionalNone> V>
    constexpr Optional& operator=(V)
    {
        clear();
        return *this;
    }

    Optional(Optional const& other)
    requires(!IsCopyConstructible<T>)
    = delete;
    Optional& operator=(Optional const&)
    requires(!IsCopyConstructible<T> || !IsDestructible<T>)
    = delete;

    ALWAYS_INLINE constexpr Optional(Optional const& other)
    requires(!IsTriviallyCopyConstructible<T>)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.value());
    }

    ALWAYS_INLINE constexpr Optional& operator=(Optional const& other)
    requires(!IsTriviallyCopyAssignable<T> || !IsTriviallyDestructible<T>)
    {
        if (this == &other)
            return *this;

        clear();
        m_has_value = other.m_has_value;
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.value());
        return *this;
    }

    constexpr Optional(Optional const& other) = default;
    constexpr Optional& operator=(Optional const&) = default;

    Optional(Optional&& other)
    requires(!IsMoveConstructible<T>)
    = delete;
    ALWAYS_INLINE constexpr Optional(Optional&& other)
    requires(IsMoveConstructible<T> && !IsTriviallyMoveConstructible<T>)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());
    }

    // Note: The MoveConstructible only versions are to allow for non-move assignable types,
    //       such as types containing a reference.
    //       Move assigning into an Optional still makes sense in these cases,
    //       as to replace its contents or to allow for find-like patterns through iterator helpers.

    Optional& operator=(Optional&& other)
    requires(!IsMoveConstructible<T> || !IsDestructible<T>)
    = delete;
    ALWAYS_INLINE constexpr Optional& operator=(Optional&& other)
    requires(
        IsMoveAssignable<T> && IsMoveConstructible<T>
        && (!IsTriviallyMoveAssignable<T> || !IsTriviallyMoveConstructible<T> || !IsTriviallyDestructible<T>))
    {
        if (this == &other)
            return *this;

        if (m_has_value && other.m_has_value) {
            value() = other.release_value();
        } else if (m_has_value) {
            value().~T();
            m_has_value = false;
        } else if (other.m_has_value) {
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());
            m_has_value = true;
        }
        return *this;
    }

    ALWAYS_INLINE constexpr Optional& operator=(Optional&& other)
    requires(
        IsMoveConstructible<T> && !IsMoveAssignable<T>
        && (!IsTriviallyMoveConstructible<T> || !IsTriviallyDestructible<T>))
    {
        clear();

        m_has_value = other.m_has_value;
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());

        return *this;
    }

    // Note: These versions are not allowing scalar types, as those would mess with the `= {}`
    //       clearing pattern, they still work through an implicit conversion to Optional<T>
    //       and the regular move-assignment operator.
    template<class U = T>
    requires(
        !OneOf<RemoveCVReference<U>, Optional, OptionalNone>
        && !(IsSame<U, T> && IsScalar<U>))
    ALWAYS_INLINE constexpr Optional<T>& operator=(U&& value)
    requires(requires(T& t, U&& u) { t = forward<U>(u); } && IsConstructible<T, U &&>)
    {
        if (m_has_value)
            m_storage = forward<U>(value);
        else
            construct_at<RemoveConst<T>>(&m_storage, forward<U>(value));
        m_has_value = true;
        return *this;
    }
    template<class U = T>
    requires(
        !OneOf<RemoveCVReference<U>, Optional, OptionalNone>
        && !(IsSame<U, T> && IsScalar<U>))
    ALWAYS_INLINE constexpr Optional<T>& operator=(U&& value)
    requires(!(requires(T& t, U&& u) { t = forward<U>(u); })
        && IsConstructible<T, U &&>)
    {
        // Note: This one is needed, as it is a common pattern to assign to an Optional to set or replace it's contents
        clear();
        construct_at<RemoveConst<T>>(&m_storage, forward<U>(value));
        m_has_value = true;
        return *this;
    }

    ALWAYS_INLINE constexpr Optional(Optional&& other) = default;
    ALWAYS_INLINE constexpr Optional& operator=(Optional&& other) = default;

    ~Optional()
    requires(!IsDestructible<T>)
    = delete;
    ALWAYS_INLINE constexpr ~Optional()
    requires(IsDestructible<T> && !IsTriviallyDestructible<T>)
    {
        clear();
    }
    constexpr ~Optional() = default;

    template<typename U>
    requires(IsConstructible<T, U const&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional> && !IsLvalueReference<U>)
    ALWAYS_INLINE constexpr explicit Optional(Optional<U> const& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.value());
    }

    template<typename U>
    requires(IsConstructible<T, U &&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional> && !IsLvalueReference<U>)
    ALWAYS_INLINE constexpr explicit Optional(Optional<U>&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());
    }

    template<typename U = T>
    requires(!IsSame<OptionalNone, RemoveCVReference<U>>)
    ALWAYS_INLINE constexpr explicit(!IsConvertible<U&&, T>) Optional(U&& value)
    requires(!IsSame<RemoveCVReference<U>, Optional<T>> && IsConstructible<T, U &&>)
        : m_has_value(true)
    {
        construct_at<RemoveConst<T>>(&m_storage, forward<U>(value));
    }

    template<typename O>
    ALWAYS_INLINE constexpr bool operator==(Optional<O> const& other) const
    {
        return has_value() == other.has_value() && (!has_value() || value() == other.value());
    }

    template<typename O>
    ALWAYS_INLINE constexpr bool operator==(O const& other) const
    {
        return has_value() && value() == other;
    }

    ALWAYS_INLINE constexpr void clear()
    {
        if (m_has_value) {
            value().~T();
            m_has_value = false;
        }
    }

    template<typename... Parameters>
    ALWAYS_INLINE constexpr void emplace(Parameters&&... parameters)
    {
        clear();
        m_has_value = true;
        construct_at<RemoveConst<T>>(&m_storage, forward<Parameters>(parameters)...);
    }

    template<typename Callable>
    ALWAYS_INLINE constexpr void lazy_emplace(Callable callable)
    {
        clear();
        m_has_value = true;
        construct_at<RemoveConst<T>>(&m_storage, callable());
    }

    [[nodiscard]] ALWAYS_INLINE constexpr bool has_value() const { return m_has_value; }

    [[nodiscard]] ALWAYS_INLINE constexpr T& value() &
    {
        VERIFY(m_has_value);
        return m_storage;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T const& value() const&
    {
        VERIFY(m_has_value);
        return m_storage;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T value() &&
    {
        return release_value();
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T release_value()
    {
        VERIFY(m_has_value);
        T released_value = move(value());
        value().~T();
        m_has_value = false;
        return released_value;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T value_or(T const& fallback) const&
    {
        if (m_has_value)
            return value();
        return fallback;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T value_or(T&& fallback) &&
    {
        if (m_has_value)
            return move(value());
        return move(fallback);
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr T value_or_lazy_evaluated(Callback callback) const
    {
        if (m_has_value)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr Optional<T> value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_has_value)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr ErrorOr<T> try_value_or_lazy_evaluated(Callback callback) const
    {
        if (m_has_value)
            return value();
        return TRY(callback());
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr ErrorOr<Optional<T>> try_value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_has_value)
            return value();
        return TRY(callback());
    }

    ALWAYS_INLINE constexpr T const& operator*() const { return value(); }
    ALWAYS_INLINE constexpr T& operator*() { return value(); }

    ALWAYS_INLINE constexpr T const* operator->() const { return &value(); }
    ALWAYS_INLINE constexpr T* operator->() { return &value(); }

    template<typename F, typename MappedType = decltype(declval<F>()(declval<T&>())), auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>, typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>>
    ALWAYS_INLINE constexpr Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper)
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
    ALWAYS_INLINE constexpr Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper) const
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
    union {
        // FIXME: GCC seems to have an issue with uninitialized unions and non trivial types,
        //        which forces us to have an equally sized trivial null member in the union
        //        to pseudo-initialize the union.
        struct {
            u8 _[sizeof(T)];
        } m_null {};
        RemoveConst<T> m_storage;
    };
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

    ALWAYS_INLINE constexpr Optional() = default;

    template<SameAs<OptionalNone> V>
    ALWAYS_INLINE constexpr Optional(V) { }

    template<SameAs<OptionalNone> V>
    ALWAYS_INLINE constexpr Optional& operator=(V)
    {
        clear();
        return *this;
    }

    template<typename U = T>
    ALWAYS_INLINE constexpr Optional(U& value)
    requires(CanBePlacedInOptional<U&>)
        : m_pointer(&value)
    {
    }

    ALWAYS_INLINE constexpr Optional(RemoveReference<T>& value)
        : m_pointer(&value)
    {
    }

    ALWAYS_INLINE constexpr Optional(Optional const& other) = default;

    ALWAYS_INLINE constexpr Optional(Optional&& other)
        : m_pointer(other.m_pointer)
    {
        other.m_pointer = nullptr;
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional(Optional<U> const& other)
    requires(CanBePlacedInOptional<U>)
        : m_pointer(other.m_pointer)
    {
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional(Optional<U>&& other)
    requires(CanBePlacedInOptional<U>)
        : m_pointer(other.m_pointer)
    {
        other.m_pointer = nullptr;
    }

    ALWAYS_INLINE constexpr Optional& operator=(Optional const& other) = default;
    ALWAYS_INLINE constexpr Optional& operator=(Optional&& other)
    {
        m_pointer = other.m_pointer;
        other.m_pointer = nullptr;
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional& operator=(Optional<U> const& other)
    requires(CanBePlacedInOptional<U>)
    {
        m_pointer = other.m_pointer;
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional& operator=(Optional<U>&& other)
    requires(CanBePlacedInOptional<U>)
    {
        m_pointer = other.m_pointer;
        other.m_pointer = nullptr;
        return *this;
    }

    // Note: Disallows assignment from a temporary as this does not do any lifetime extension.
    template<typename U>
    requires(!IsSame<OptionalNone, RemoveCVReference<U>>)
    ALWAYS_INLINE constexpr Optional& operator=(U&& value)
    requires(CanBePlacedInOptional<U> && IsLvalueReference<U>)
    {
        m_pointer = &value;
        return *this;
    }

    ALWAYS_INLINE constexpr void clear()
    {
        m_pointer = nullptr;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr bool has_value() const { return m_pointer != nullptr; }

    [[nodiscard]] ALWAYS_INLINE constexpr T value()
    {
        VERIFY(m_pointer);
        return *m_pointer;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr AddConstToReferencedType<T> value() const
    {
        VERIFY(m_pointer);
        return *m_pointer;
    }

    template<typename U>
    requires(IsBaseOf<RemoveCVReference<T>, U>) [[nodiscard]]
    ALWAYS_INLINE constexpr AddConstToReferencedType<T> value_or(U& fallback) const
    {
        if (m_pointer)
            return value();
        return fallback;
    }

    // Note that this ends up copying the value.
    [[nodiscard]] ALWAYS_INLINE constexpr RemoveCVReference<T> value_or(RemoveCVReference<T> fallback) const
    {
        if (m_pointer)
            return value();
        return fallback;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T release_value()
    {
        return *exchange(m_pointer, nullptr);
    }

    template<typename U>
    ALWAYS_INLINE constexpr bool operator==(Optional<U> const& other) const
    {
        return has_value() == other.has_value() && (!has_value() || value() == other.value());
    }

    template<typename U>
    ALWAYS_INLINE constexpr bool operator==(U const& other) const
    {
        return has_value() && value() == other;
    }

    ALWAYS_INLINE constexpr AddConstToReferencedType<T> operator*() const { return value(); }
    ALWAYS_INLINE constexpr T operator*() { return value(); }

    ALWAYS_INLINE constexpr RawPtr<AddConst<RemoveReference<T>>> operator->() const { return &value(); }
    ALWAYS_INLINE constexpr RawPtr<RemoveReference<T>> operator->() { return &value(); }

    // Conversion operators from Optional<T&> -> Optional<T>
    ALWAYS_INLINE constexpr explicit operator Optional<RemoveCVReference<T>>() const
    {
        if (has_value())
            return Optional<RemoveCVReference<T>>(value());
        return {};
    }
    ALWAYS_INLINE constexpr Optional<RemoveCVReference<T>> copy() const
    {
        return static_cast<Optional<RemoveCVReference<T>>>(*this);
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr T value_or_lazy_evaluated(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr Optional<T> value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return callback();
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr ErrorOr<T> try_value_or_lazy_evaluated(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return TRY(callback());
    }

    template<typename Callback>
    [[nodiscard]] ALWAYS_INLINE constexpr ErrorOr<Optional<T>> try_value_or_lazy_evaluated_optional(Callback callback) const
    {
        if (m_pointer != nullptr)
            return value();
        return TRY(callback());
    }

    template<typename F, typename MappedType = decltype(declval<F>()(declval<T&>())), auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>, typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>>
    ALWAYS_INLINE constexpr Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper)
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
    ALWAYS_INLINE constexpr Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(F&& mapper) const
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
