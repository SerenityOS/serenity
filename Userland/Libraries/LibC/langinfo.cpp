/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <langinfo.h>

// Values taken from glibc's en_US locale files.
static const char* __nl_langinfo(nl_item item)
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
        return "Sunday";
    case DAY_2:
        return "Monday";
    case DAY_3:
        return "Tuesday";
    case DAY_4:
        return "Wednesday";
    case DAY_5:
        return "Thursday";
    case DAY_6:
        return "Friday";
    case DAY_7:
        return "Saturday";
    case ABDAY_1:
        return "Sun";
    case ABDAY_2:
        return "Mon";
    case ABDAY_3:
        return "Tue";
    case ABDAY_4:
        return "Wed";
    case ABDAY_5:
        return "Thu";
    case ABDAY_6:
        return "Fri";
    case ABDAY_7:
        return "Sat";
    case MON_1:
        return "January";
    case MON_2:
        return "February";
    case MON_3:
        return "March";
    case MON_4:
        return "April";
    case MON_5:
        return "May";
    case MON_6:
        return "June";
    case MON_7:
        return "July";
    case MON_8:
        return "August";
    case MON_9:
        return "September";
    case MON_10:
        return "October";
    case MON_11:
        return "November";
    case MON_12:
        return "December";
    case ABMON_1:
        return "Jan";
    case ABMON_2:
        return "Feb";
    case ABMON_3:
        return "Mar";
    case ABMON_4:
        return "Apr";
    case ABMON_5:
        return "May";
    case ABMON_6:
        return "Jun";
    case ABMON_7:
        return "Jul";
    case ABMON_8:
        return "Aug";
    case ABMON_9:
        return "Sep";
    case ABMON_10:
        return "Oct";
    case ABMON_11:
        return "Nov";
    case ABMON_12:
        return "Dec";
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
