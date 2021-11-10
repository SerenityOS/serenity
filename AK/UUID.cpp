/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/Hex.h>
#include <AK/StringBuilder.h>
#include <AK/UUID.h>

namespace AK {

UUID::UUID(Array<u8, 16> uuid_buffer)
{
    uuid_buffer.span().copy_to(m_uuid_buffer);
}

void UUID::convert_string_view_to_uuid(StringView uuid_string_view)
{
    VERIFY(uuid_string_view.length() == 36);
    auto first_unit = decode_hex(uuid_string_view.substring_view(0, 8));
    auto second_unit = decode_hex(uuid_string_view.substring_view(9, 4));
    auto third_unit = decode_hex(uuid_string_view.substring_view(14, 4));
    auto fourth_unit = decode_hex(uuid_string_view.substring_view(19, 4));
    auto fifth_unit = decode_hex(uuid_string_view.substring_view(24, 12));

    VERIFY(first_unit.value().size() == 4 && second_unit.value().size() == 2
        && third_unit.value().size() == 2 && fourth_unit.value().size() == 2
        && fifth_unit.value().size() == 6);

    m_uuid_buffer.span().overwrite(0, first_unit.value().data(), first_unit.value().size());
    m_uuid_buffer.span().overwrite(4, second_unit.value().data(), second_unit.value().size());
    m_uuid_buffer.span().overwrite(6, third_unit.value().data(), third_unit.value().size());
    m_uuid_buffer.span().overwrite(8, fourth_unit.value().data(), fourth_unit.value().size());
    m_uuid_buffer.span().overwrite(10, fifth_unit.value().data(), fifth_unit.value().size());
}

UUID::UUID(StringView uuid_string_view)
{
    convert_string_view_to_uuid(uuid_string_view);
}

String UUID::to_string() const
{
    StringBuilder builder(36);
    builder.append(encode_hex(m_uuid_buffer.span().trim(4)).view());
    builder.append('-');
    builder.append(encode_hex(m_uuid_buffer.span().slice(4).trim(2)).view());
    builder.append('-');
    builder.append(encode_hex(m_uuid_buffer.span().slice(6).trim(2)).view());
    builder.append('-');
    builder.append(encode_hex(m_uuid_buffer.span().slice(8).trim(2)).view());
    builder.append('-');
    builder.append(encode_hex(m_uuid_buffer.span().slice(10).trim(6)).view());
    return builder.to_string();
}

bool UUID::operator==(const UUID& other) const
{
    for (size_t index = 0; index < 16; index++) {
        if (m_uuid_buffer[index] != other.m_uuid_buffer[index])
            return false;
    }
    return true;
}

bool UUID::is_zero() const
{
    return all_of(m_uuid_buffer, [](const auto octet) { return octet == 0; });
}

}
