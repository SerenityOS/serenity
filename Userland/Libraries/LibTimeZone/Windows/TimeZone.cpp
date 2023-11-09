/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <windows.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Globalization.h>

#define AK_DONT_REPLACE_STD
#include <AK/String.h>
#include <LibTimeZone/Windows/TimeZone.h>
#undef AK_DONT_REPLACE_STD

namespace TimeZone {

AK::ErrorOr<AK::String> get_windows_time_zone()
{
    winrt::init_apartment();
    winrt::Windows::Globalization::Calendar calendar;
    winrt::hstring timeZone = calendar.GetTimeZone();
    return TRY(AK::String::from_deprecated_string(winrt::to_string(timeZone).c_str()));
}

}
