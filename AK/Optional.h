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
#include <AK/Noncopyable.h>
#include <AK/Platform.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
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

template<typename Self, typename T>
struct AddConstIfNeeded {
    using Type = Conditional<IsConst<RemoveReference<Self>> && !IsConst<T>, AddConst<T>, T>;
};

}

template<auto condition, typename T>
using ConditionallyResultType = typename Detail::ConditionallyResultType<condition, T>::Type;
template<typename Self, typename T>
using AddConstIfNeeded = typename Detail::AddConstIfNeeded<Self, T>::Type;

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
requires(!IsLvalueReference<T>)
class [[nodiscard]] OptionalBase {
public:
    using ValueType = T;

    template<typename Self, SameAs<OptionalNone> V>
    ALWAYS_INLINE constexpr Self& operator=(this Self& self, V)
    {
        self.clear();
        return self;
    }

    template<typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr AddConstIfNeeded<Self, T>* ptr(this Self& self)
    {
        return self.has_value() ? &self.value() : nullptr;
    }

    template<typename O = T, typename Fallback = O, typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr O value_or(this Self&& self, Fallback&& fallback)
    {
        if (self.has_value())
            return forward<Self>(self).value();
        return forward<Fallback>(fallback);
    }

    template<typename Callback, typename O = T, typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr O value_or_lazy_evaluated(this Self&& self, Callback callback)
    {
        if (self.has_value())
            return forward<Self>(self).value();
        return callback();
    }

    template<typename Callback, typename O = T, typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr Optional<O> value_or_lazy_evaluated_optional(this Self&& self, Callback callback)
    {
        if (self.has_value())
            return forward<Self>(self);
        return callback();
    }

    template<typename Callback, typename O = T, typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr ErrorOr<O> try_value_or_lazy_evaluated(this Self&& self, Callback callback)
    {
        if (self.has_value())
            return forward<Self>(self).value();
        return TRY(callback());
    }

    template<typename Callback, typename O = T, typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr ErrorOr<Optional<O>> try_value_or_lazy_evaluated_optional(this Self&& self, Callback callback)
    {
        if (self.has_value())
            return forward<Self>(self);
        return TRY(callback());
    }

    template<typename Self>
    [[nodiscard]] ALWAYS_INLINE constexpr AddConstIfNeeded<Self, T>& operator*(this Self&& self) { return self.value(); }
    template<typename Self>
    ALWAYS_INLINE constexpr AddConstIfNeeded<Self, T>* operator->(this Self&& self) { return &self.value(); }

    template<typename F,
        typename MappedType = decltype(declval<F>()(declval<T&>())),
        auto IsErrorOr = IsSpecializationOf<MappedType, ErrorOr>,
        typename OptionalType = Optional<ConditionallyResultType<IsErrorOr, MappedType>>,
        typename Self>
    ALWAYS_INLINE constexpr Conditional<IsErrorOr, ErrorOr<OptionalType>, OptionalType> map(this Self&& self, F&& mapper)
    {
        if constexpr (IsErrorOr) {
            if (self.has_value())
                return OptionalType { TRY(mapper(forward<Self>(self).value())) };
            return OptionalType {};
        } else {
            if (self.has_value())
                return OptionalType { mapper(forward<Self>(self).value()) };

            return OptionalType {};
        }
    }
};

template<typename T>
requires(!IsLvalueReference<T> && !requires { Traits<T>::special_optional_empty_value; })
class [[nodiscard]] Optional<T> : public OptionalBase<T> {
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

    AK_MAKE_CONDITIONALLY_COPYABLE(Optional, <T>);
    AK_MAKE_CONDITIONALLY_MOVABLE(Optional, <T>);
    AK_MAKE_CONDITIONALLY_DESTRUCTIBLE(Optional, <T>);

    ALWAYS_INLINE constexpr Optional(Optional const& other)
    requires(!IsTriviallyCopyConstructible<T>)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.value());
    }

    // Note: The MoveConstructible only versions are to allow for non-move assignable types,
    //       such as types containing a reference.
    //       Move assigning into an Optional still makes sense in these cases,
    //       as to replace its contents or to allow for find-like patterns through iterator helpers.

    ALWAYS_INLINE constexpr Optional(Optional&& other)
    requires(IsMoveConstructible<T> && !IsTriviallyMoveConstructible<T>)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());
    }

    template<typename U>
    requires(IsConstructible<T, U const&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional> && !IsLvalueReference<U>)
    ALWAYS_INLINE constexpr explicit Optional(Optional<U> const& other)
        : m_has_value(other.has_value())
    {
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.value());
    }

    template<typename U>
    requires(IsConstructible<T, U &&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional> && !IsLvalueReference<U>)
    ALWAYS_INLINE constexpr explicit Optional(Optional<U>&& other)
        : m_has_value(other.has_value())
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

    ALWAYS_INLINE constexpr Optional& operator=(Optional&& other)
    requires(IsMoveAssignable<T> && IsMoveConstructible<T> && (!IsTriviallyMoveAssignable<T> || !IsTriviallyMoveConstructible<T> || !IsTriviallyDestructible<T>))
    {
        if (this == &other)
            return *this;

        if (has_value() && other.has_value()) {
            value() = other.release_value();
        } else if (has_value()) {
            value().~T();
            m_has_value = false;
        } else if (other.has_value()) {
            m_has_value = true;
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());
        }
        return *this;
    }

    // Allow for move constructible but non-move assignable types, such as those containing const or reference fields,
    // Note: This overload can also handle move assignable types perfectly fine, but the behaviour would be slightly different.
    ALWAYS_INLINE constexpr Optional& operator=(Optional&& other)
    requires(!IsMoveAssignable<T> && IsMoveConstructible<T> && (!IsTriviallyMoveConstructible<T> || !IsTriviallyDestructible<T>))
    {
        if (this == &other)
            return *this;

        clear();
        m_has_value = other.m_has_value;
        if (other.has_value())
            construct_at<RemoveConst<T>>(&m_storage, other.release_value());
        return *this;
    }

    template<class U = T>
    requires(!IsOneOf<RemoveCVReference<U>, Optional<T>, OptionalNone> && !(IsSame<U, T> && IsScalar<U>))
    // Note: We restrict this to `!IsScalar<U>` to prevent undesired overload resolution for `= {}`.
    ALWAYS_INLINE constexpr Optional<T>& operator=(U&& value)
    requires(IsConstructible<T, U &&>)
    {
        if constexpr (IsAssignable<AddLvalueReference<T>, AddRvalueReference<U>>) {
            if (m_has_value)
                m_storage = forward<U>(value);
            else
                construct_at<RemoveConst<T>>(&m_storage, forward<U>(value));
            m_has_value = true;
        } else {
            emplace(forward<U>(value));
        }
        return *this;
    }

    ALWAYS_INLINE constexpr ~Optional()
    requires(!IsTriviallyDestructible<T> && IsDestructible<T>)
    {
        clear();
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
    // Note: This can't be based on OptionalBase<T>, does not work with T&'s.
    AK_MAKE_DEFAULT_COPYABLE(Optional);
    AK_MAKE_DEFAULT_MOVABLE(Optional);

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

    template<typename U>
    ALWAYS_INLINE constexpr Optional(Optional<U>& other)
    requires(CanBePlacedInOptional<U>)
        : m_pointer(other.ptr())
    {
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional(Optional<U> const& other)
    requires(CanBePlacedInOptional<U const>)
        : m_pointer(other.ptr())
    {
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional(Optional<U>&& other)
    requires(CanBePlacedInOptional<U>)
        : m_pointer(other.ptr())
    {
        other.m_pointer = nullptr;
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional& operator=(Optional<U>& other)
    requires(CanBePlacedInOptional<U>)
    {
        m_pointer = other.ptr();
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional& operator=(Optional<U> const& other)
    requires(CanBePlacedInOptional<U const>)
    {
        m_pointer = other.ptr();
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE constexpr Optional& operator=(Optional<U>&& other)
    requires(CanBePlacedInOptional<U> && IsLvalueReference<U>)
    {
        m_pointer = other.m_pointer;
        other.m_pointer = nullptr;
        return *this;
    }

    template<typename U>
    requires(!IsSame<OptionalNone, RemoveCVReference<U>>)
    ALWAYS_INLINE constexpr Optional& operator=(U& value)
    requires(CanBePlacedInOptional<U>)
    {
        m_pointer = &value;
        return *this;
    }

    // Note: Disallows assignment from a temporary as this does not do any lifetime extension.
    template<typename U>
    requires(!IsSame<OptionalNone, RemoveCVReference<U>>)
    ALWAYS_INLINE consteval Optional& operator=(RemoveReference<U> const&& value)
    requires(CanBePlacedInOptional<U>)
    = delete;

    ALWAYS_INLINE constexpr void clear()
    {
        m_pointer = nullptr;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr bool has_value() const { return m_pointer != nullptr; }

    [[nodiscard]] ALWAYS_INLINE constexpr RemoveReference<T>* ptr()
    {
        return m_pointer;
    }

    [[nodiscard]] ALWAYS_INLINE constexpr RemoveReference<T> const* ptr() const
    {
        return m_pointer;
    }

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
    requires(IsBaseOf<RemoveCVReference<T>, U>)
    [[nodiscard]] ALWAYS_INLINE constexpr AddConstToReferencedType<T> value_or(U& fallback) const
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

    ALWAYS_INLINE constexpr AddConstToReferencedType<T> operator*() const { return value(); }
    ALWAYS_INLINE constexpr T operator*() { return value(); }

    ALWAYS_INLINE constexpr RawPtr<AddConst<RemoveReference<T>>> operator->() const { return &value(); }
    ALWAYS_INLINE constexpr RawPtr<RemoveReference<T>> operator->() { return &value(); }

    // Conversion operators from Optional<T&> -> Optional<T>, implicit when T is trivially copyable.
    ALWAYS_INLINE constexpr explicit(!IsTriviallyCopyable<RemoveCVReference<T>>) operator Optional<RemoveCVReference<T>>() const
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

template<typename T>
requires(requires { Traits<T>::special_optional_empty_value; })
class [[nodiscard]] Optional<T> : public OptionalBase<T> {

    // Note: We need to go through this helper to not materialize a temporary at compile time
    //       which needs to be destroyed at compile time.
    ALWAYS_INLINE constexpr static auto the_empty_value() -> T
    {
        if constexpr (IsSame<decltype(Traits<T>::special_optional_empty_value), T>)
            return Traits<T>::special_optional_empty_value;
        else if constexpr (IsCallableWithArguments<decltype(Traits<T>::special_optional_empty_value), T>)
            return Traits<T>::special_optional_empty_value();
        else if constexpr (IsCallableWithArguments<decltype(Traits<T>::special_optional_empty_value), T, Badge<Optional<T>>>)
            return Traits<T>::special_optional_empty_value(Badge<Optional<T>> {});
    }
    // FIXME: We should investigate if we could guess trivial swappability
    //        based on attributes the T has (see trivially relocatable/replaceable)
    //        This might also be useful for the normal Optional implementation

public:
    ALWAYS_INLINE constexpr Optional() = default;
    ALWAYS_INLINE constexpr Optional(OptionalNone) { }
    template<typename U>
    requires(IsConstructible<T, U> && !IsOneOf<RemoveCVReference<U>, Optional, OptionalNone>)
    ALWAYS_INLINE constexpr Optional(U&& value)
        : m_value(forward<U>(value))
    {
    }

    AK_MAKE_CONDITIONALLY_COPYABLE(Optional, <T>);
    AK_MAKE_CONDITIONALLY_MOVABLE(Optional, <T>);
    AK_MAKE_CONDITIONALLY_DESTRUCTIBLE(Optional, <T>);

    template<typename U>
    requires(
        !IsScalar<U> && IsConstructible<T, U const&>
        && !IsOneOf<RemoveCVReference<U>, Optional, OptionalNone>)
    ALWAYS_INLINE constexpr Optional& operator=(U const& value)
    {
        m_value = value;
        return *this;
    }
    template<typename U>
    requires(
        !IsScalar<U> && IsConstructible<T, U &&>
        && !IsOneOf<RemoveCVReference<U>, Optional, OptionalNone>)
    ALWAYS_INLINE constexpr Optional& operator=(U&& value)
    {
        m_value = forward<U>(value);
        return *this;
    }

    ALWAYS_INLINE constexpr bool has_value() const
    {
        return m_value != the_empty_value();
    }
    ALWAYS_INLINE constexpr bool has_value() const
    requires(requires(T const& t) {{ Traits<T>::optional_has_value(t) } -> SameAs<bool>; })
    {
        return Traits<T>::optional_has_value(m_value);
    }
    ALWAYS_INLINE constexpr void clear()
    {
        if (has_value())
            (void)release_value();
    }

    ALWAYS_INLINE constexpr T& value() &
    {
        VERIFY(has_value());
        return m_value;
    }
    ALWAYS_INLINE constexpr T const& value() const&
    {
        VERIFY(has_value());
        return m_value;
    }

    ALWAYS_INLINE constexpr T value() && { return release_value(); }

    ALWAYS_INLINE constexpr T release_value()
    {
        VERIFY(has_value());
        return exchange(m_value, the_empty_value());
    }

    template<typename... Ts>
    requires(IsConstructible<T, Ts...>)
    ALWAYS_INLINE constexpr void emplace(Ts&&... parameters)
    {
        clear();
        construct_at<T>(&m_value, forward<Ts>(parameters)...);
    }

private:
    T m_value { the_empty_value() };
};

template<typename T1, typename T2>
ALWAYS_INLINE constexpr bool operator==(Optional<T1> const& first, Optional<T2> const& second)
{
    return first.has_value() == second.has_value()
        && (!first.has_value() || first.value() == second.value());
}

template<typename T1, typename T2>
ALWAYS_INLINE constexpr bool operator==(Optional<T1> const& first, T2 const& second)
{
    return first.has_value() && first.value() == second;
}

template<typename T>
ALWAYS_INLINE constexpr bool operator==(Optional<T> const& first, OptionalNone)
{
    return !first.has_value();
}

template<typename T>
struct Traits<Optional<T>> : public DefaultTraits<Optional<T>> {
    static unsigned hash(Optional<T> const& optional)
    {
        // Arbitrary-ish value for an empty optional, but not 0 as that is a common 'hash' for many T's.
        if (!optional.has_value())
            return 13;

        return Traits<T>::hash(optional.value());
    }
};

}

#if USING_AK_GLOBALLY
using AK::Optional;
using AK::OptionalNone;
#endif
