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

}

}
