/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Format.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/UFixedBigInt.h>
#include <AK/Vector.h>

#ifdef KERNEL
#    include <AK/Error.h>
#    include <Kernel/Library/KString.h>
#else
#    include <AK/String.h>
#endif
#include <AK/IPv4Address.h>
#include <AK/StringBuilder.h>

namespace AK {

namespace Details {

// constexpr version of StringUtils::convert_to_uint_from_hex which cannot be constexpr
// thanks to trimming functionality and a circular dependency on StringView.
constexpr Optional<u16> convert_to_u16_from_hex(StringView string)
{
    if (string.is_empty())
        return {};

    u16 value = 0;
    auto const count = string.length();
    constexpr u16 upper_bound = NumericLimits<u16>::max();

    for (size_t i = 0; i < count; i++) {
        char digit = string[i];
        u8 digit_val;
        if (value > (upper_bound >> 4))
            return {};

        if (digit >= '0' && digit <= '9') {
            digit_val = digit - '0';
        } else if (digit >= 'a' && digit <= 'f') {
            digit_val = 10 + (digit - 'a');
        } else if (digit >= 'A' && digit <= 'F') {
            digit_val = 10 + (digit - 'A');
        } else {
            return {};
        }

        value = (value << 4) + digit_val;
    }
    return value;
}

}

class [[gnu::packed]] IPv6Address {
public:
    using in6_addr_t = u8[16];

    constexpr IPv6Address() = default;

    constexpr IPv6Address(in6_addr_t const& data)
    {
        for (size_t i = 0; i < 16; i++)
            m_data[i] = data[i];
    }

    constexpr IPv6Address(IPv4Address const& ipv4_address)
    {
        // IPv4 mapped IPv6 address
        m_data[10] = 0xff;
        m_data[11] = 0xff;
        m_data[12] = ipv4_address[0];
        m_data[13] = ipv4_address[1];
        m_data[14] = ipv4_address[2];
        m_data[15] = ipv4_address[3];
    }

    constexpr u16 operator[](int i) const { return group(i); }

#ifdef KERNEL
    constexpr ErrorOr<NonnullOwnPtr<Kernel::KString>> to_string() const
#else
    constexpr ErrorOr<String> to_string() const
#endif
    {
        if (is_zero()) {
#ifdef KERNEL
            return Kernel::KString::try_create("::"sv);
#else
            return "::"_string;
#endif
        }

        StringBuilder builder;

        if (is_ipv4_mapped()) {
#ifdef KERNEL
            return Kernel::KString::formatted("::ffff:{}.{}.{}.{}", m_data[12], m_data[13], m_data[14], m_data[15]);
#else
            return String::formatted("::ffff:{}.{}.{}.{}", m_data[12], m_data[13], m_data[14], m_data[15]);
#endif
        }

        // Find the start of the longest span of 0 values
        Optional<int> longest_zero_span_start;
        int zero_span_length = 0;
        for (int i = 0; i < 8;) {
            if (group(i) != 0) {
                i++;
                continue;
            }
            int contiguous_zeros = 1;
            for (int j = i + 1; j < 8; j++) {
                if (group(j) != 0)
                    break;
                contiguous_zeros++;
            }

            if (!longest_zero_span_start.has_value() || longest_zero_span_start.value() < contiguous_zeros) {
                longest_zero_span_start = i;
                zero_span_length = contiguous_zeros;
            }

            i += contiguous_zeros;
        }

        for (int i = 0; i < 8;) {
            if (longest_zero_span_start.has_value() && longest_zero_span_start.value() == i) {
                if (longest_zero_span_start.value() + zero_span_length >= 8)
                    TRY(builder.try_append("::"sv));
                else
                    TRY(builder.try_append(':'));
                i += zero_span_length;
                continue;
            }

            if (i == 0)
                TRY(builder.try_appendff("{:x}", group(i)));
            else
                TRY(builder.try_appendff(":{:x}", group(i)));

            i++;
        }
#ifdef KERNEL
        return Kernel::KString::try_create(builder.string_view());
#else
        return builder.to_string();
#endif
    }

    static constexpr Optional<IPv6Address> from_string(StringView string)
    {
        if (string.is_null())
            return {};

        // Manual split_at(':', KeepEmpty) implementation, since the StringView one is not constexpr.
        Array<StringView, 9> parts_creator;
        size_t current_part = 0;
        size_t current_part_start = 0;
        for (size_t index = 0; index < string.length(); ++index) {
            auto const character = string[index];
            if (character == ':') {
                // terminate current part
                parts_creator[current_part] = string.substring_view(current_part_start, index - current_part_start);
                ++current_part;
                current_part_start = index + 1;
                if (current_part >= parts_creator.size()) {
                    // We may have 9 parts if the address is compressed
                    // at the beginning or end, e.g. by substituting the
                    // leading or trailing 0 with a : character. Otherwise,
                    // the maximum number of parts is 8, which we validate
                    // when expanding the compression.
                    return {};
                }
            }
        }
        if (current_part_start != string.length())
            parts_creator[current_part] = string.substring_view(current_part_start, string.length() - current_part_start);

        auto const parts = parts_creator.span().trim(current_part + 1);

        if (parts.is_empty())
            return {};
        if (parts.size() >= 4 && parts.last().contains('.')) {
            // Check if this may be an ipv4 mapped address
            auto is_ipv4_prefix = [&]() {
                auto separator_part = parts[parts.size() - 2];
                if (separator_part.is_empty())
                    return false;
                auto separator_value = Details::convert_to_u16_from_hex(separator_part);
                if (!separator_value.has_value() || separator_value.value() != 0xffff)
                    return false;
                // TODO: this allows multiple compression tags "::" in the prefix, which is technically not legal
                for (size_t i = 0; i < parts.size() - 2; i++) {
                    auto part = parts[i];
                    if (part.is_empty())
                        continue;
                    auto value = Details::convert_to_u16_from_hex(part);
                    if (!value.has_value() || value.value() != 0)
                        return false;
                }
                return true;
            };

            if (is_ipv4_prefix()) {
                auto ipv4_address = IPv4Address::from_string(parts[parts.size() - 1]);
                if (ipv4_address.has_value())
                    return IPv6Address(ipv4_address.value());
                return {};
            }
        }

        in6_addr_t addr {};
        int group = 0;
        int have_groups = 0;
        bool found_compressed = false;
        for (size_t i = 0; i < parts.size();) {
            auto trimmed_part = parts[i];
            if (trimmed_part.is_empty()) {
                if (found_compressed)
                    return {};
                int empty_parts = 1;
                bool is_leading = (i == 0);
                bool is_trailing = false;
                for (size_t j = i + 1; j < parts.size(); j++) {
                    if (!parts[j].is_empty())
                        break;
                    empty_parts++;
                    if (j == parts.size() - 1)
                        is_trailing = true;
                }
                if (is_leading && is_trailing) {
                    if (empty_parts > 3)
                        return {};
                    return IPv6Address();
                }
                if (is_leading || is_trailing) {
                    if (empty_parts > 2)
                        return {};
                } else if (empty_parts > 1) {
                    return {};
                }

                int remaining_parts = parts.size() - empty_parts - have_groups;
                found_compressed = true;
                group = 8 - remaining_parts;
                VERIFY(group >= 0);
                i += empty_parts;
                continue;
            } else {
                i++;
            }
            auto part = Details::convert_to_u16_from_hex(trimmed_part);
            if (!part.has_value())
                return {};

            if (++have_groups > 8)
                return {};

            VERIFY(group < 8);
            addr[group * sizeof(u16)] = (u8)(part.value() >> 8);
            addr[group * sizeof(u16) + 1] = (u8)part.value();
            group++;
        }

        return IPv6Address(addr);
    }

    constexpr in6_addr_t const& to_in6_addr_t() const { return m_data; }

    constexpr bool operator==(IPv6Address const& other) const = default;
    constexpr bool operator!=(IPv6Address const& other) const = default;

    constexpr bool is_zero() const
    {
        for (auto& d : m_data) {
            if (d != 0)
                return false;
        }
        return true;
    }

    constexpr bool is_ipv4_mapped() const
    {
        if (m_data[0] || m_data[1] || m_data[2] || m_data[3] || m_data[4] || m_data[5] || m_data[6] || m_data[7] || m_data[8] || m_data[9])
            return false;
        if (m_data[10] != 0xff || m_data[11] != 0xff)
            return false;
        return true;
    }

    constexpr Optional<IPv4Address> ipv4_mapped_address() const
    {
        if (is_ipv4_mapped())
            return IPv4Address(m_data[12], m_data[13], m_data[14], m_data[15]);
        return {};
    }

    // https://datatracker.ietf.org/doc/html/rfc4291#section-2.5.3
    [[nodiscard]] constexpr static IPv6Address loopback()
    {
        return IPv6Address({ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 });
    }

    [[nodiscard]] constexpr bool is_loopback() const
    {
        return *this == loopback();
    }

    [[nodiscard]] constexpr bool is_in_subnet(IPv6Address subnet, u16 network_size) const
    {
        VERIFY(network_size <= 128);
        return this->network(network_size) == subnet;
    }

    [[nodiscard]] constexpr IPv6Address network(u16 network_size) const
    {
        VERIFY(network_size <= 128);
        IPv6Address net;
        for (int i = 0; i < 16; ++i) {
            if (network_size >= 8) {
                net.m_data[i] = m_data[i];
                network_size -= 8;
            } else {
                u8 mask = ((1 << network_size) - 1) << (8 - network_size);
                net.m_data[i] = m_data[i] & mask;
                break;
            }
        }
        return net;
    }

    // https://datatracker.ietf.org/doc/html/rfc4291#section-2.5.6
    [[nodiscard]] constexpr bool is_link_local() const
    {
        return is_in_subnet({ { 0xfe, 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }, 10);
    }

    // https://datatracker.ietf.org/doc/html/rfc4193
    [[nodiscard]] constexpr bool is_unique_local() const
    {
        return is_in_subnet({ { 0xfc, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }, 7);
    }

    // https://datatracker.ietf.org/doc/html/rfc2373#section-2.7
    [[nodiscard]] constexpr bool is_multicast() const
    {
        return is_in_subnet({ { 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } }, 8);
    }
    [[nodiscard]] constexpr bool is_unicast() const { return !is_multicast(); }

private:
    constexpr u16 group(unsigned i) const
    {
        VERIFY(i < 8);
        return ((u16)m_data[i * sizeof(u16)] << 8) | m_data[i * sizeof(u16) + 1];
    }

    in6_addr_t m_data {};
};

static_assert(sizeof(IPv6Address) == 16);

template<>
struct Traits<IPv6Address> : public DefaultTraits<IPv6Address> {
    // SipHash-4-8 is considered conservatively secure, even if not cryptographically secure.
    static unsigned hash(IPv6Address const& address) { return sip_hash_bytes<4, 8>({ &address.to_in6_addr_t(), sizeof(address.to_in6_addr_t()) }); }
};

#ifdef KERNEL
template<>
struct Formatter<IPv6Address> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, IPv6Address const& value)
    {
        return Formatter<StringView>::format(builder, TRY(value.to_string())->view());
    }
};
#else
template<>
struct Formatter<IPv6Address> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, IPv6Address const& value)
    {
        return Formatter<StringView>::format(builder, TRY(value.to_string()));
    }
};
#endif

}

#if USING_AK_GLOBALLY
using AK::IPv6Address;
#endif
