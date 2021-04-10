/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "u128.h"
#include "u256.h"

#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <serenity.h>
#include <stdio.h>

void AK::Formatter<u128>::format(AK::FormatBuilder& builder, u128 value)
{
    if (value.high() == 0) {
        AK::Formatter<u64> formatter { *this };
        return formatter.format(builder, value.low());
    }

    if (m_precision.has_value())
        VERIFY_NOT_REACHED();

    if (m_mode == Mode::Pointer) {
        // this is way to big for a pointer
        VERIFY_NOT_REACHED();
    }

    u8 base = 0;
    bool upper_case = false;
    if (m_mode == Mode::Binary) {
        base = 2;
    } else if (m_mode == Mode::BinaryUppercase) {
        base = 2;
        upper_case = true;
    } else if (m_mode == Mode::Octal) {
        base = 8;
    } else if (m_mode == Mode::Decimal || m_mode == Mode::Default) {
        // FIXME: implement this
        TODO();
    } else if (m_mode == Mode::Hexadecimal) {
        base = 16;
    } else if (m_mode == Mode::HexadecimalUppercase) {
        base = 16;
        upper_case = true;
    } else {
        VERIFY_NOT_REACHED();
    }

    u16 lower_length = sizeof(u64) * 0xFF / base;
    if (m_width.value() > lower_length) {
        builder.put_u64(value.high(), base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value() - lower_length, m_fill, m_sign_mode);
        builder.put_u64(value.low(), base, false, upper_case, m_zero_pad, m_align, m_width.value(), m_fill, m_sign_mode);
    } else {
        builder.put_u64(value.low(), base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value(), m_fill, m_sign_mode);
    }
}

void AK::Formatter<u256>::format(AK::FormatBuilder& builder, u256 value)
{
    if (value.high() == 0) {
        AK::Formatter<u128> formatter { *this };
        return formatter.format(builder, value.low());
    }

    if (m_precision.has_value())
        VERIFY_NOT_REACHED();

    if (m_mode == Mode::Pointer) {
        // this is way to big for a pointer
        VERIFY_NOT_REACHED();
    }

    u8 base = 0;
    bool upper_case = false;
    if (m_mode == Mode::Binary) {
        base = 2;
    } else if (m_mode == Mode::BinaryUppercase) {
        base = 2;
        upper_case = true;
    } else if (m_mode == Mode::Octal) {
        base = 8;
    } else if (m_mode == Mode::Decimal || m_mode == Mode::Default) {
        // FIXME: implement this
        TODO();
    } else if (m_mode == Mode::Hexadecimal) {
        base = 16;
    } else if (m_mode == Mode::HexadecimalUppercase) {
        base = 16;
        upper_case = true;
    } else {
        VERIFY_NOT_REACHED();
    }

    u16 part_length = sizeof(u128) * 0xFF / base;
    if (m_width.value() > part_length * 3) {
        builder.put_u64(value.high().high(), base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value() - part_length * 3, m_fill, m_sign_mode);
        builder.put_u64(value.high().low(), base, false, upper_case, m_zero_pad, m_align, part_length, m_fill, m_sign_mode);
        builder.put_u64(value.low().high(), base, false, upper_case, m_zero_pad, m_align, part_length, m_fill, m_sign_mode);
        builder.put_u64(value.low().low(), base, false, upper_case, m_zero_pad, m_align, part_length, m_fill, m_sign_mode);
    } else if (m_width.value() > part_length * 2) {
        builder.put_u64(value.high().low(), base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value() - part_length * 2, m_fill, m_sign_mode);
        builder.put_u64(value.low().high(), base, false, upper_case, m_zero_pad, m_align, part_length, m_fill, m_sign_mode);
        builder.put_u64(value.low().low(), base, false, upper_case, m_zero_pad, m_align, part_length, m_fill, m_sign_mode);
    } else if (m_width.value() > part_length) {
        builder.put_u64(value.low().high(), base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value() - part_length, m_fill, m_sign_mode);
        builder.put_u64(value.low().low(), base, false, upper_case, m_zero_pad, m_align, part_length, m_fill, m_sign_mode);
    } else {
        builder.put_u64(value.low().low(), base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value(), m_fill, m_sign_mode);
    }
}
