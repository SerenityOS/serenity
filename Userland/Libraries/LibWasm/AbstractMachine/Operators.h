/*
 * Copyright (c) 2021-2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/BuiltinWrappers.h>
#include <AK/Result.h>
#include <AK/SIMD.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <limits.h>
#include <math.h>

namespace Wasm::Operators {

using namespace AK::SIMD;

#define DEFINE_BINARY_OPERATOR(Name, operation) \
    struct Name {                               \
        template<typename Lhs, typename Rhs>    \
        auto operator()(Lhs lhs, Rhs rhs) const \
        {                                       \
            return lhs operation rhs;           \
        }                                       \
                                                \
        static StringView name()                \
        {                                       \
            return #operation##sv;              \
        }                                       \
    }

DEFINE_BINARY_OPERATOR(Equals, ==);
DEFINE_BINARY_OPERATOR(NotEquals, !=);
DEFINE_BINARY_OPERATOR(GreaterThan, >);
DEFINE_BINARY_OPERATOR(LessThan, <);
DEFINE_BINARY_OPERATOR(LessThanOrEquals, <=);
DEFINE_BINARY_OPERATOR(GreaterThanOrEquals, >=);
DEFINE_BINARY_OPERATOR(Add, +);
DEFINE_BINARY_OPERATOR(Subtract, -);
DEFINE_BINARY_OPERATOR(Multiply, *);
DEFINE_BINARY_OPERATOR(BitAnd, &);
DEFINE_BINARY_OPERATOR(BitOr, |);
DEFINE_BINARY_OPERATOR(BitXor, ^);

#undef DEFINE_BINARY_OPERATOR

struct Divide {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        if constexpr (IsFloatingPoint<Lhs>) {
            return lhs / rhs;
        } else {
            Checked value(lhs);
            value /= rhs;
            if (value.has_overflow())
                return AK::Result<Lhs, StringView>("Integer division overflow"sv);
            return AK::Result<Lhs, StringView>(value.value());
        }
    }

    static StringView name() { return "/"sv; }
};

struct Modulo {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        if (rhs == 0)
            return AK::Result<Lhs, StringView>("Integer division overflow"sv);
        if constexpr (IsSigned<Lhs>) {
            if (rhs == -1)
                return AK::Result<Lhs, StringView>(0); // Spec weirdness right here, signed division overflow is ignored.
        }
        return AK::Result<Lhs, StringView>(lhs % rhs);
    }

    static StringView name() { return "%"sv; }
};

struct BitShiftLeft {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const { return lhs << (rhs % (sizeof(lhs) * 8)); }

    static StringView name() { return "<<"sv; }
};

struct BitShiftRight {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const { return lhs >> (rhs % (sizeof(lhs) * 8)); }

    static StringView name() { return ">>"sv; }
};

struct BitRotateLeft {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        // generates a single 'rol' instruction if shift is positive
        // otherwise generate a `ror`
        auto const mask = CHAR_BIT * sizeof(Lhs) - 1;
        rhs &= mask;
        return (lhs << rhs) | (lhs >> ((-rhs) & mask));
    }

    static StringView name() { return "rotate_left"sv; }
};

struct BitRotateRight {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        // generates a single 'ror' instruction if shift is positive
        // otherwise generate a `rol`
        auto const mask = CHAR_BIT * sizeof(Lhs) - 1;
        rhs &= mask;
        return (lhs >> rhs) | (lhs << ((-rhs) & mask));
    }

    static StringView name() { return "rotate_right"sv; }
};

template<size_t VectorSize>
struct VectorShiftLeft {
    auto operator()(u128 lhs, i32 rhs) const
    {
        auto shift_value = rhs % (sizeof(lhs) * 8 / VectorSize);
        return bit_cast<u128>(bit_cast<Native128ByteVectorOf<NativeIntegralType<128 / VectorSize>, MakeUnsigned>>(lhs) << shift_value);
    }
    static StringView name()
    {
        switch (VectorSize) {
        case 16:
            return "vec(8x16)<<"sv;
        case 8:
            return "vec(16x8)<<"sv;
        case 4:
            return "vec(32x4)<<"sv;
        case 2:
            return "vec(64x2)<<"sv;
        default:
            VERIFY_NOT_REACHED();
        }
    }
};

template<size_t VectorSize, template<typename> typename SetSign>
struct VectorShiftRight {
    auto operator()(u128 lhs, i32 rhs) const
    {
        auto shift_value = rhs % (sizeof(lhs) * 8 / VectorSize);
        return bit_cast<u128>(bit_cast<Native128ByteVectorOf<NativeIntegralType<128 / VectorSize>, SetSign>>(lhs) >> shift_value);
    }
    static StringView name()
    {
        switch (VectorSize) {
        case 16:
            return "vec(8x16)>>"sv;
        case 8:
            return "vec(16x8)>>"sv;
        case 4:
            return "vec(32x4)>>"sv;
        case 2:
            return "vec(64x2)>>"sv;
        default:
            VERIFY_NOT_REACHED();
        }
    }
};

struct Minimum {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        if constexpr (IsFloatingPoint<Lhs> || IsFloatingPoint<Rhs>) {
            if (isnan(lhs))
                return lhs;
            if (isnan(rhs))
                return rhs;
            if (isinf(lhs))
                return lhs > 0 ? rhs : lhs;
            if (isinf(rhs))
                return rhs > 0 ? lhs : rhs;
        }
        return min(lhs, rhs);
    }

    static StringView name() { return "minimum"sv; }
};

struct Maximum {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        if constexpr (IsFloatingPoint<Lhs> || IsFloatingPoint<Rhs>) {
            if (isnan(lhs))
                return lhs;
            if (isnan(rhs))
                return rhs;
            if (isinf(lhs))
                return lhs > 0 ? lhs : rhs;
            if (isinf(rhs))
                return rhs > 0 ? rhs : lhs;
        }
        return max(lhs, rhs);
    }

    static StringView name() { return "maximum"sv; }
};

struct CopySign {
    template<typename Lhs, typename Rhs>
    auto operator()(Lhs lhs, Rhs rhs) const
    {
        if constexpr (IsSame<Lhs, float>)
            return copysignf(lhs, rhs);
        else if constexpr (IsSame<Lhs, double>)
            return copysign(lhs, rhs);
        else
            static_assert(DependentFalse<Lhs, Rhs>, "Invalid types to CopySign");
    }

    static StringView name() { return "copysign"sv; }
};

// Unary

struct EqualsZero {
    template<typename Lhs>
    auto operator()(Lhs lhs) const { return lhs == 0; }

    static StringView name() { return "== 0"sv; }
};

struct CountLeadingZeros {
    template<typename Lhs>
    i32 operator()(Lhs lhs) const
    {
        if (lhs == 0)
            return sizeof(Lhs) * CHAR_BIT;

        if constexpr (sizeof(Lhs) == 4 || sizeof(Lhs) == 8)
            return count_leading_zeroes(MakeUnsigned<Lhs>(lhs));
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "clz"sv; }
};

struct CountTrailingZeros {
    template<typename Lhs>
    i32 operator()(Lhs lhs) const
    {
        if (lhs == 0)
            return sizeof(Lhs) * CHAR_BIT;

        if constexpr (sizeof(Lhs) == 4 || sizeof(Lhs) == 8)
            return count_trailing_zeroes(MakeUnsigned<Lhs>(lhs));
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "ctz"sv; }
};

struct PopCount {
    template<typename Lhs>
    auto operator()(Lhs lhs) const
    {
        if constexpr (sizeof(Lhs) == 4 || sizeof(Lhs) == 8)
            return popcount(MakeUnsigned<Lhs>(lhs));
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "popcnt"sv; }
};

struct Absolute {
    template<typename Lhs>
    auto operator()(Lhs lhs) const { return AK::abs(lhs); }

    static StringView name() { return "abs"sv; }
};

struct Negate {
    template<typename Lhs>
    auto operator()(Lhs lhs) const { return -lhs; }

    static StringView name() { return "== 0"sv; }
};

struct Ceil {
    template<typename Lhs>
    auto operator()(Lhs lhs) const
    {
        if constexpr (IsSame<Lhs, float>)
            return ceilf(lhs);
        else if constexpr (IsSame<Lhs, double>)
            return ceil(lhs);
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "ceil"sv; }
};

struct Floor {
    template<typename Lhs>
    auto operator()(Lhs lhs) const
    {
        if constexpr (IsSame<Lhs, float>)
            return floorf(lhs);
        else if constexpr (IsSame<Lhs, double>)
            return floor(lhs);
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "floor"sv; }
};

struct Truncate {
    template<typename Lhs>
    AK::Result<Lhs, StringView> operator()(Lhs lhs) const
    {
        if constexpr (IsSame<Lhs, float>)
            return truncf(lhs);
        else if constexpr (IsSame<Lhs, double>)
            return trunc(lhs);
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "truncate"sv; }
};

struct NearbyIntegral {
    template<typename Lhs>
    auto operator()(Lhs lhs) const
    {
        if constexpr (IsSame<Lhs, float>)
            return nearbyintf(lhs);
        else if constexpr (IsSame<Lhs, double>)
            return nearbyint(lhs);
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "round"sv; }
};

struct SquareRoot {
    template<typename Lhs>
    auto operator()(Lhs lhs) const
    {
        if constexpr (IsSame<Lhs, float>)
            return sqrtf(lhs);
        else if constexpr (IsSame<Lhs, double>)
            return sqrt(lhs);
        else
            VERIFY_NOT_REACHED();
    }

    static StringView name() { return "sqrt"sv; }
};

template<typename Result>
struct Wrap {
    template<typename Lhs>
    Result operator()(Lhs lhs) const
    {
        return static_cast<MakeUnsigned<Result>>(bit_cast<MakeUnsigned<Lhs>>(lhs));
    }

    static StringView name() { return "wrap"sv; }
};

template<typename ResultT>
struct CheckedTruncate {
    template<typename Lhs>
    AK::Result<ResultT, StringView> operator()(Lhs lhs) const
    {
        if (isnan(lhs) || isinf(lhs)) // "undefined", let's just trap.
            return "Truncation undefined behavior"sv;

        Lhs truncated;
        if constexpr (IsSame<float, Lhs>)
            truncated = truncf(lhs);
        else if constexpr (IsSame<double, Lhs>)
            truncated = trunc(lhs);
        else
            VERIFY_NOT_REACHED();

        // FIXME: This function assumes that all values of ResultT are representable in Lhs
        //        the assumption comes from the fact that this was used exclusively by LibJS,
        //        which only considers values that are all representable in 'double'.
        if (!AK::is_within_range<ResultT>(truncated))
            return "Truncation out of range"sv;

        return static_cast<ResultT>(truncated);
    }

    static StringView name() { return "truncate.checked"sv; }
};

template<typename ResultT>
struct Extend {
    template<typename Lhs>
    ResultT operator()(Lhs lhs) const
    {
        return lhs;
    }

    static StringView name() { return "extend"sv; }
};

template<typename ResultT>
struct Convert {
    template<typename Lhs>
    ResultT operator()(Lhs lhs) const
    {
        auto signed_interpretation = bit_cast<MakeSigned<Lhs>>(lhs);
        return static_cast<ResultT>(signed_interpretation);
    }

    static StringView name() { return "convert"sv; }
};

template<typename ResultT>
struct Reinterpret {
    template<typename Lhs>
    ResultT operator()(Lhs lhs) const
    {
        return bit_cast<ResultT>(lhs);
    }

    static StringView name() { return "reinterpret"sv; }
};

struct Promote {
    double operator()(float lhs) const
    {
        if (isnan(lhs))
            return nan(""); // FIXME: Ensure canonical NaN remains canonical
        return static_cast<double>(lhs);
    }

    static StringView name() { return "promote"sv; }
};

struct Demote {
    float operator()(double lhs) const
    {
        if (isnan(lhs))
            return nanf(""); // FIXME: Ensure canonical NaN remains canonical

        if (isinf(lhs))
            return __builtin_huge_valf();

        return static_cast<float>(lhs);
    }

    static StringView name() { return "demote"sv; }
};

template<typename InitialType>
struct SignExtend {
    template<typename Lhs>
    Lhs operator()(Lhs lhs) const
    {
        auto unsigned_representation = bit_cast<MakeUnsigned<Lhs>>(lhs);
        auto truncated_unsigned_representation = static_cast<MakeUnsigned<InitialType>>(unsigned_representation);
        auto initial_value = bit_cast<InitialType>(truncated_unsigned_representation);
        return static_cast<Lhs>(initial_value);
    }

    static StringView name() { return "extend"sv; }
};

template<typename ResultT>
struct SaturatingTruncate {
    template<typename Lhs>
    ResultT operator()(Lhs lhs) const
    {
        if (isnan(lhs))
            return 0;

        if (isinf(lhs)) {
            if (lhs < 0)
                return NumericLimits<ResultT>::min();
            return NumericLimits<ResultT>::max();
        }

        // FIXME: This assumes that all values in ResultT are representable in 'double'.
        //        that assumption is not correct, which makes this function yield incorrect values
        //        for 'edge' values of type i64.
        constexpr auto convert = []<typename ConvertT>(ConvertT truncated_value) {
            if (truncated_value < NumericLimits<ResultT>::min())
                return NumericLimits<ResultT>::min();
            if constexpr (IsSame<ConvertT, float>) {
                if (truncated_value >= static_cast<ConvertT>(NumericLimits<ResultT>::max()))
                    return NumericLimits<ResultT>::max();
            } else {
                if (static_cast<double>(truncated_value) >= static_cast<double>(NumericLimits<ResultT>::max()))
                    return NumericLimits<ResultT>::max();
            }
            return static_cast<ResultT>(truncated_value);
        };

        if constexpr (IsSame<Lhs, float>)
            return convert(truncf(lhs));
        else
            return convert(trunc(lhs));
    }

    static StringView name() { return "truncate.saturating"sv; }
};

}
