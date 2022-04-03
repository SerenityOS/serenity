/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DateConstants.h>
#include <langinfo.h>

// Values taken from glibc's en_US locale files.
static char const* __nl_langinfo(nl_item item)
{
    switch (item) {
    case CODESET:
        return "UTF-8";
    case D_T_FMT:
        return "%a %d %b %Y %r %Z";
    case D_FMT:
        return "%m/%d/%Y";
    case T_FMT:
        return "%r";
    case T_FMT_AMPM:
        return "%I:%M:%S %p";
    case AM_STR:
        return "AM";
    case PM_STR:
        return "PM";
    case DAY_1:
    case DAY_2:
    case DAY_3:
    case DAY_4:
    case DAY_5:
    case DAY_6:
    case DAY_7:
        return long_day_names[item - DAY_1].characters_without_null_termination();
    case ABDAY_1:
    case ABDAY_2:
    case ABDAY_3:
    case ABDAY_4:
    case ABDAY_5:
    case ABDAY_6:
    case ABDAY_7:
        return short_day_names[item - ABDAY_1].characters_without_null_termination();
    case MON_1:
    case MON_2:
    case MON_3:
    case MON_4:
    case MON_5:
    case MON_6:
    case MON_7:
    case MON_8:
    case MON_9:
    case MON_10:
    case MON_11:
    case MON_12:
        return long_month_names[item - MON_1].characters_without_null_termination();
    case ABMON_1:
    case ABMON_2:
    case ABMON_3:
    case ABMON_4:
    case ABMON_5:
    case ABMON_6:
    case ABMON_7:
    case ABMON_8:
    case ABMON_9:
    case ABMON_10:
    case ABMON_11:
    case ABMON_12:
        return short_month_names[item - ABMON_1].characters_without_null_termination();
    case RADIXCHAR:
        return ".";
    case THOUSEP:
        return ",";
    case YESEXPR:
        return "^[+1yY]";
    case NOEXPR:
        return "^[-0nN]";
    // en_US does not have ERA.
    case ERA:
    case ERA_D_FMT:
    case ERA_D_T_FMT:
    case ERA_T_FMT:
    // en_US also doesn't have special digit symbols.
    case ALT_DIGITS:
    // Invalid values also return an empty string.
    default:
        return "";
    }
}

extern "C" {

char* nl_langinfo(nl_item item)
{
    // POSIX states that returned strings should not be modified,
    // so this cast is probably fine.
    return const_cast<char*>(__nl_langinfo(item));
}
}
