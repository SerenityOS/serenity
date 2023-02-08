/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/String.h>

namespace AK {

template<typename R>
R Error::format_impl() const
{
    static_assert(IsSame<R, ErrorOr<AK::String>>);

    auto& format_data = m_data.get<FormattedString>();
    alignas(AK::TypeErasedParameter) u8 params_bits[sizeof(AK::TypeErasedParameter) * ((64 - sizeof(StringView)) / 2)];
    auto* params = bit_cast<AK::TypeErasedParameter*>(&params_bits[0]);
    size_t count = 0;
    size_t offset = 0;

    u8 aligned_local_storage[1 * KiB] {};
    size_t aligned_local_storage_offset = 0;
    auto allocate_aligned_on_local_storage = [&]<typename T>(u8 const* p) {
        auto start_offset = align_up_to(aligned_local_storage_offset, alignof(T));
        VERIFY(array_size(aligned_local_storage) >= start_offset + sizeof(T));
        __builtin_memcpy(&aligned_local_storage[start_offset], p, sizeof(T));
        aligned_local_storage_offset = start_offset + sizeof(T);
        return bit_cast<T const*>(&aligned_local_storage[start_offset]);
    };

    for (; format_data.buffer[offset] != 0;) {
        auto type = static_cast<FormattedString::Type>(format_data.buffer[offset]);
        offset += 1;
        switch (type) {
        case FormattedString::Type::Nothing:
            VERIFY_NOT_REACHED();

#define CASE(Name, T)                                                                              \
    case FormattedString::Type::Name:                                                              \
        params[count++] = AK::TypeErasedParameter {                                                \
            allocate_aligned_on_local_storage.template operator()<T>(&format_data.buffer[offset]), \
            AK::TypeErasedParameter::get_type<T>(),                                                \
            AK::__format_value<T>                                                                  \
        };                                                                                         \
        offset += sizeof(T);                                                                       \
        break

            CASE(StringView, StringView);
            CASE(U8, u8);
            CASE(U16, u16);
            CASE(U32, u32);
            CASE(U64, u64);
            CASE(I8, i8);
            CASE(I16, i16);
            CASE(I32, i32);
            CASE(I64, i64);

#undef CASE
        }
    }

    TypeErasedFormatParams format_params;
    format_params.set_parameters({ params, count });
    return AK::String::vformatted(format_data.format_string, format_params);
}

template ErrorOr<String> Error::format_impl<ErrorOr<String>>() const;

}
