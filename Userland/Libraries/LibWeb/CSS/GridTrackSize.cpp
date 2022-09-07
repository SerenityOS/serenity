/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSize.h"
#include <AK/String.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

GridTrackSize::GridTrackSize(Length length)
    : m_type(Type::Length)
    , m_length(length)
{
}

GridTrackSize::GridTrackSize(Percentage percentage)
    : m_type(Type::Percentage)
    , m_percentage(percentage)
{
}

GridTrackSize::GridTrackSize(float flexible_length)
    : m_type(Type::FlexibleLength)
    , m_flexible_length(flexible_length)
{
}

GridTrackSize GridTrackSize::make_auto()
{
    return GridTrackSize(CSS::Length::make_auto());
}

String GridTrackSize::to_string() const
{
    switch (m_type) {
    case Type::Length:
        return m_length.to_string();
    case Type::Percentage:
        return m_percentage.to_string();
    case Type::FlexibleLength:
        return String::formatted("{}fr", m_flexible_length);
    }
    VERIFY_NOT_REACHED();
}

}
