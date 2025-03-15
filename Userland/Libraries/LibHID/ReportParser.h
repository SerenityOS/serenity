/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/IterationDecision.h>

namespace HID {

struct ApplicationCollection;
struct ParsedReportDescriptor;
struct Field;

// The maximum field size is 32 bits. Fields can be signed or unsigned.
// Use an i64 for the field value so it can fit all possible field values without having the caller convert it to a signed int.
ErrorOr<void> parse_input_report(ParsedReportDescriptor const&, ApplicationCollection const&, ReadonlyBytes, Function<ErrorOr<IterationDecision>(Field const&, i64)>);

}
