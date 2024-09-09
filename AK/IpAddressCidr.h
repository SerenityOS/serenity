/*
 * Copyright (c) 2024, famfo <famfo@famfo.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IPv4Address.h>
#include <AK/IPv6Address.h>

namespace AK {

class IPv4AddressCidr;
class IPv6AddressCidr;

namespace Details {

template<typename Address>
class AddressTraits;

template<OneOf<IPv4AddressCidr, IPv6AddressCidr> AddressFamily>
class IPAddressCidr {
public:
    enum class IPAddressCidrError {
        CidrTooLong,
        StringParsingFailed,
    };

    using IPAddress = Details::AddressTraits<AddressFamily>::IPAddress;

    static constexpr ErrorOr<AddressFamily, IPAddressCidrError> create(IPAddress address, u8 length)
    {
        if (length > AddressFamily::MAX_LENGTH)
            return IPAddressCidrError::CidrTooLong;

        return AddressFamily(address, length);
    }

    constexpr IPAddress const& ip_address() const& { return m_address; }
    constexpr u32 length() const { return m_length; }

    constexpr void set_ip_address(IPAddress address) { m_address = address; }
    constexpr ErrorOr<void, IPAddressCidrError> set_length(u32 length)
    {
        if (length > AddressFamily::MAX_LENGTH)
            return IPAddressCidrError::CidrTooLong;

        m_length = length;
        return {};
    }

    constexpr static ErrorOr<AddressFamily, IPAddressCidrError> from_string(StringView string)
    {
        Vector<StringView> const parts = string.split_view('/');

        if (parts.size() != 2)
            return IPAddressCidrError::StringParsingFailed;

        auto ip_address = IPAddress::from_string(parts[0]);
        if (!ip_address.has_value())
            return IPAddressCidrError::StringParsingFailed;

        Optional<u8> length = parts[1].to_number<u8>();
        if (!length.has_value())
            return IPAddressCidrError::StringParsingFailed;

        return IPAddressCidr::create(ip_address.value(), length.release_value());
    }

#ifdef KERNEL
    ErrorOr<NonnullOwnPtr<Kernel::KString>> to_string() const
#else
    ErrorOr<String> to_string() const
#endif
    {
        StringBuilder builder;

        auto address_string = TRY(m_address.to_string());

#ifdef KERNEL
        TRY(builder.try_append(address_string->view()));
#else
        TRY(builder.try_append(address_string));
#endif

        TRY(builder.try_append('/'));
        TRY(builder.try_appendff("{}", m_length));

#ifdef KERNEL
        return Kernel::KString::try_create(builder.string_view());
#else
        return builder.to_string();
#endif
    }

    constexpr bool operator==(IPAddressCidr const& other) const = default;
    constexpr bool operator!=(IPAddressCidr const& other) const = default;

protected:
    constexpr IPAddressCidr(IPAddress address, u8 length)
        : m_address(address)
        , m_length(length)
    {
    }

private:
    IPAddress m_address;
    u8 m_length;
};

}

}

#if USING_AK_GLOBALLY
#endif
