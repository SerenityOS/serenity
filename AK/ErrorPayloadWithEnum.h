/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>

namespace AK {

template<Enum T>
class ErrorPayloadWithEnum : public ErrorPayload {
public:
    ErrorPayloadWithEnum(T value)
        : m_value(value)
    {
    }

    virtual ErrorOr<void> format(Formatter<FormatString>& formatter, FormatBuilder& builder) const override
    {
        return formatter.format(builder, "{}"sv, m_value);
    }

#ifdef AK_ERROR_SUPPORTS_DYNAMIC
    virtual bool operator==(ErrorPayload const& other) const override
    {
        auto const* other_as_specific_type = dynamic_cast<ErrorPayloadWithEnum<T> const*>(&other);

        // If it isn't the same type, it can't be equal.
        if (!other_as_specific_type)
            return false;

        return m_value == other_as_specific_type->m_value;
    }
#endif

    bool operator==(T value) const
    {
        return m_value == value;
    }

private:
    T m_value;
};

template<Enum T>
struct Formatter<ErrorPayloadWithEnum<T>> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, ErrorPayloadWithEnum<T> const& error)
    {
        return Formatter<FormatString>::format(builder, "{}"sv, error);
    }
};

}

#if USING_AK_GLOBALLY
using AK::ErrorPayloadWithEnum;
#endif
