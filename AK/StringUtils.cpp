/*
 * Copyright (c) 2018-2020, Andreas Kling <awesomekling@gmail.com>
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>

namespace AK {

namespace StringUtils {

    bool matches(const StringView& str, const StringView& mask, CaseSensitivity case_sensitivity)
    {
        if (str.is_null() || mask.is_null())
            return str.is_null() && mask.is_null();

        if (case_sensitivity == CaseSensitivity::CaseInsensitive) {
            const String str_lower = String(str).to_lowercase();
            const String mask_lower = String(mask).to_lowercase();
            return matches(str_lower, mask_lower, CaseSensitivity::CaseSensitive);
        }

        const char* string_ptr = str.characters_without_null_termination();
        const char* string_end = string_ptr + str.length();
        const char* mask_ptr = mask.characters_without_null_termination();
        const char* mask_end = mask_ptr + mask.length();

        // Match string against mask directly unless we hit a *
        while ((string_ptr < string_end) && (mask_ptr < mask_end) && (*mask_ptr != '*')) {
            if ((*mask_ptr != *string_ptr) && (*mask_ptr != '?'))
                return false;
            mask_ptr++;
            string_ptr++;
        }

        const char* cp = nullptr;
        const char* mp = nullptr;

        while (string_ptr < string_end) {
            if ((mask_ptr < mask_end) && (*mask_ptr == '*')) {
                // If we have only a * left, there is no way to not match.
                if (++mask_ptr == mask_end)
                    return true;
                mp = mask_ptr;
                cp = string_ptr + 1;
            } else if ((mask_ptr < mask_end) && ((*mask_ptr == *string_ptr) || (*mask_ptr == '?'))) {
                mask_ptr++;
                string_ptr++;
            } else if ((cp != nullptr) && (mp != nullptr)) {
                mask_ptr = mp;
                string_ptr = cp++;
            } else {
                break;
            }
        }

        // Handle any trailing mask
        while ((mask_ptr < mask_end) && (*mask_ptr == '*'))
            mask_ptr++;

        // If we 'ate' all of the mask and the string then we match.
        return (mask_ptr == mask_end) && string_ptr == string_end;
    }

    int convert_to_int(const StringView& str, bool& ok)
    {
        if (str.is_empty()) {
            ok = false;
            return 0;
        }

        bool negative = false;
        size_t i = 0;
        const auto characters = str.characters_without_null_termination();

        if (characters[0] == '-' || characters[0] == '+') {
            if (str.length() == 1) {
                ok = false;
                return 0;
            }
            i++;
            negative = (characters[0] == '-');
        }

        int value = 0;
        for (; i < str.length(); i++) {
            if (characters[i] < '0' || characters[i] > '9') {
                ok = false;
                return 0;
            }
            value = value * 10;
            value += characters[i] - '0';
        }
        ok = true;

        return negative ? -value : value;
    }

    unsigned convert_to_uint(const StringView& str, bool& ok)
    {
        if (str.is_empty()) {
            ok = false;
            return 0;
        }

        unsigned value = 0;
        const auto characters = str.characters_without_null_termination();

        for (size_t i = 0; i < str.length(); i++) {
            if (characters[i] < '0' || characters[i] > '9') {
                ok = false;
                return 0;
            }
            value = value * 10;
            value += characters[i] - '0';
        }
        ok = true;

        return value;
    }

}

}
