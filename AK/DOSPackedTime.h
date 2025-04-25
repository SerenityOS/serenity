/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <AK/Types.h>

namespace AK {

union DOSPackedTime {
    u16 value;
    struct {
        u16 second : 5;
        u16 minute : 6;
        u16 hour : 5;
    };
};
static_assert(sizeof(DOSPackedTime) == 2);

union DOSPackedDate {
    u16 value;
    struct {
        u16 day : 5;
        u16 month : 4;
        u16 year : 7;
    };
};
static_assert(sizeof(DOSPackedDate) == 2);

inline constexpr u16 first_dos_year = 1980;
inline constexpr u32 first_dos_representable_unix_timestamp = UnixDateTime::from_unix_time_parts(1980, 1, 1, 0, 0, 0, 0).seconds_since_epoch();
inline constexpr u64 last_dos_representable_unix_timestamp = UnixDateTime::from_unix_time_parts(2107, 12, 31, 23, 59, 59, 0).seconds_since_epoch();

UnixDateTime time_from_packed_dos(DOSPackedDate, DOSPackedTime);
DOSPackedDate to_packed_dos_date(unsigned year, unsigned month, unsigned day);
DOSPackedTime to_packed_dos_time(unsigned hour, unsigned minute, unsigned second);
ErrorOr<DOSPackedDate> to_packed_dos_date(UnixDateTime const&);
ErrorOr<DOSPackedTime> to_packed_dos_time(UnixDateTime const&);

}

#if USING_AK_GLOBALLY
using AK::DOSPackedDate;
using AK::DOSPackedTime;
using AK::time_from_packed_dos;
using AK::to_packed_dos_date;
using AK::to_packed_dos_time;
#endif
