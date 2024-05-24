/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/TypedTransfer.h>
#include <AK/Userspace.h>

#if defined(KERNEL) && !defined(PREKERNEL)
#    include <Kernel/Arch/SafeMem.h>
#    include <Kernel/Arch/SmapDisabler.h>
#    include <Kernel/Memory/MemorySections.h>
#endif

namespace AK {

template<size_t Size>
class FixedStringBuffer {
public:
    [[nodiscard]] static ErrorOr<FixedStringBuffer<Size>> vformatted(StringView fmtstr, AK::TypeErasedFormatParams& params)
    requires(Size < StringBuilder::inline_capacity)
    {
        StringBuilder builder { StringBuilder::UseInlineCapacityOnly::Yes };
        TRY(AK::vformat(builder, fmtstr, params));
        FixedStringBuffer<Size> buffer {};
        buffer.store_characters(builder.string_view());
        return buffer;
    }

    template<typename... Parameters>
    [[nodiscard]] static ErrorOr<FixedStringBuffer<Size>> formatted(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    requires(Size < StringBuilder::inline_capacity)
    {
        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_parameters { parameters... };
        return vformatted(fmtstr.view(), variadic_format_parameters);
    }

    void store_characters(StringView characters)
    {
        // NOTE: Only store the characters up to the first null terminator
        // because we don't care about any further characters.
        // This matches some expected behavior in the Kernel code, because
        // technically userspace programs could send a syscall argument with
        // multiple null terminators - we only care about the *first* chunk up to
        // the first null terminator, if present at all.
        size_t stored_length = 0;
        for (; stored_length < min(Size, characters.length()); stored_length++) {
            if (characters[stored_length] == '\0')
                break;
            m_storage[stored_length] = characters[stored_length];
        }
        m_stored_length = stored_length;
        // NOTE: Fill the rest of the array bytes with zeroes, just to be
        // on the safe side.
        // Technically, it means that a sent StringView could occupy the
        // entire storage without any null terminators and that's OK as well.
        for (size_t index = m_stored_length; index < Size; index++)
            m_storage[index] = '\0';
    }

#if defined(KERNEL) && !defined(PREKERNEL)
    ErrorOr<void> copy_characters_from_user(Userspace<char const*> user_str, size_t user_str_size)
    {
        if (user_str_size > Size)
            return EFAULT;
        bool is_user = Kernel::Memory::is_user_range(user_str.vaddr(), user_str_size);
        if (!is_user)
            return EFAULT;
        Kernel::SmapDisabler disabler;
        void* fault_at;
        ssize_t length = Kernel::safe_strnlen(user_str.unsafe_userspace_ptr(), user_str_size, fault_at);
        if (length < 0) {
            dbgln("FixedStringBuffer::copy_characters_into_storage({:p}, {}) failed at {} (strnlen)", static_cast<void const*>(user_str.unsafe_userspace_ptr()), user_str_size, VirtualAddress { fault_at });
            return EFAULT;
        }
        if (!Kernel::safe_memcpy(m_storage.data(), user_str.unsafe_userspace_ptr(), (size_t)length, fault_at)) {
            dbgln("FixedStringBuffer::copy_characters_into_storage({:p}, {}) failed at {} (memcpy)", static_cast<void const*>(user_str.unsafe_userspace_ptr()), user_str_size, VirtualAddress { fault_at });
            return EFAULT;
        }
        m_stored_length = (size_t)length;
        for (size_t index = m_stored_length; index < Size; index++)
            m_storage[index] = '\0';
        return {};
    }
#endif

    Span<u8> storage()
    {
        return m_storage.span();
    }
    StringView representable_view() const { return StringView(m_storage.data(), m_stored_length); }
    Span<u8 const> span_view_ensuring_ending_null_char()
    {
        VERIFY(m_stored_length + 1 <= Size);
        m_storage[m_stored_length] = '\0';
        return Span<u8 const>(m_storage.data(), m_stored_length + 1);
    }

    size_t stored_length() const { return m_stored_length; }

    FixedStringBuffer()
    {
        m_storage.fill(0);
    }

private:
    Array<u8, Size> m_storage;
    size_t m_stored_length { 0 };
};

}

#if USING_AK_GLOBALLY
using AK::FixedStringBuffer;
#endif
