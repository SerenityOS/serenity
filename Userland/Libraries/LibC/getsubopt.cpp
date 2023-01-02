/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <string.h>
#include <unistd.h>

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getsubopt.html
int getsubopt(char** option_array, char* const* tokens, char** option_value)
{
    if (**option_array == '\0')
        return -1;

    auto const* option_ptr = *option_array;
    StringView option_string { option_ptr, strlen(option_ptr) };

    auto possible_comma_location = option_string.find(',');
    char* option_end = const_cast<char*>(option_string.characters_without_null_termination()) + possible_comma_location.value_or(option_string.length());

    auto possible_equals_char_location = option_string.find('=');
    char* value_start = option_end;
    if (possible_equals_char_location.has_value()) {
        value_start = const_cast<char*>(option_string.characters_without_null_termination()) + possible_equals_char_location.value();
    }

    ScopeGuard ensure_end_array_contains_null_char([&]() {
        if (*option_end != '\0')
            *option_end++ = '\0';
        *option_array = option_end;
    });

    for (int count = 0; tokens[count] != NULL; ++count) {
        auto const* token = tokens[count];
        StringView token_stringview { token, strlen(token) };
        if (!option_string.starts_with(token_stringview))
            continue;
        if (tokens[count][value_start - *option_array] != '\0')
            continue;

        *option_value = value_start != option_end ? value_start + 1 : nullptr;
        return count;
    }

    // Note: The current sub-option does not match any option, so prepare to tell this
    // to the application.
    *option_value = *option_array;
    return -1;
}
