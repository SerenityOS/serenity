/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace Kernel::SCSI {

// SAM 4: 5.3.1 Status codes
enum class StatusCode : u8 {
    Good = 0x00,
    CheckCondition = 0x02,
    ConditionMet = 0x04,
    Busy = 0x08,
    // Obsolete = 0x10,
    // Obsolete = 0x14,
    ReservationConflict = 0x18,
    // Obsolete = 0x22, was Command Terminated
    TaskSetFull = 0x28,
    ACAActive = 0x30,
    TaskAborted = 0x40,
};
constexpr StringView to_string(StatusCode status)
{
    using enum StatusCode;
    switch (status) {
    case Good:
        return "Good"sv;
    case CheckCondition:
        return "Check Condition"sv;
    case ConditionMet:
        return "Condition Met"sv;
    case Busy:
        return "Busy"sv;
    case ReservationConflict:
        return "Reservation Conflict"sv;
    case TaskSetFull:
        return "Task Set Full"sv;
    case ACAActive:
        return "ACA Active"sv;
    case TaskAborted:
        return "Task Aborted"sv;
    default:
        return "Unknown"sv;
    }
}

}

namespace AK {

template<>
struct Formatter<Kernel::SCSI::StatusCode> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::SCSI::StatusCode value)
    {
        return Formatter<FormatString>::format(builder, "{:02x}({})"sv, to_underlying(value), Kernel::SCSI::to_string(value));
    }
};

}
