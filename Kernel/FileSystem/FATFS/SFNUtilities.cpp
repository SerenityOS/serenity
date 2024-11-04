/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <Kernel/FileSystem/FATFS/SFNUtilities.h>

#ifdef KERNEL
#    include <Kernel/Library/KString.h>
#else
#    include <AK/ByteString.h>
#endif

namespace Kernel::SFNUtils {

ErrorOr<NonnullRefPtr<SFN>> SFN::try_create(ByteBuffer name, ByteBuffer extension, size_t unique)
{
    VERIFY(name.size() > 0);
    auto new_name = TRY(name.slice(0, min(name.size(), 6)));
    auto new_extension = TRY(extension.slice(0, min(extension.size(), 3)));
    return adopt_nonnull_ref_or_enomem(new (nothrow) SFN(move(new_name), move(new_extension), unique));
}

SFN::SFN(ByteBuffer name, ByteBuffer extension, size_t unique)
    : m_name(move(name))
    , m_extension(move(extension))
    , m_unique(unique)
{
}

size_t SFN::digits() const
{
    size_t digits = 0;
    size_t w = m_unique;
    while (w /= 10)
        ++digits;
    return digits;
}

ErrorOr<ByteBuffer> SFN::serialize_name() const
{
    auto name = TRY(ByteBuffer::copy(m_name.data(), m_name.size() - digits()));
    TRY(name.try_ensure_capacity(8));
#ifdef KERNEL
    auto suffix = TRY(KString::formatted("~{}", m_unique));
    name.append(suffix->bytes());
#else
    auto suffix = ByteString::formatted("~{}", m_unique);
    name.append(suffix.bytes());
#endif

    while (name.size() < 8)
        name.append(' ');

    return name;
}

ErrorOr<ByteBuffer> SFN::serialize_extension() const
{
    auto extension = TRY(ByteBuffer::copy(m_extension));
    TRY(extension.try_ensure_capacity(3));

    while (extension.size() < 3)
        extension.append(' ');

    return extension;
}

static constexpr auto valid_misc_sfn_chars = to_array<char>({ '$', '%', '\'', '-', '_', '@', ' ', '~', '`', '!', '(', ')' });

static bool is_valid_sfn_char(char c)
{
    if (c >= 'A' && c <= 'Z')
        return true;
    if (c >= '0' && c <= '9')
        return true;

    return valid_misc_sfn_chars.contains_slow(c);
}

bool is_valid_sfn(StringView sfn)
{
    StringView name = {};
    StringView extension = {};

    auto dot = sfn.find('.');
    if (!dot.has_value()) {
        name = sfn;
    } else {
        if (*dot + 1 >= sfn.length())
            return false;

        name = sfn.substring_view(0, *dot);
        extension = sfn.substring_view(*dot + 1, sfn.length() - *dot - 1);
    }

    if (name.length() > 8 || extension.length() > 3)
        return false;

    for (char c : name) {
        if (!is_valid_sfn_char(c))
            return false;
    }

    for (char c : extension) {
        if (!is_valid_sfn_char(c))
            return false;
    }

    if (name.length() == 0 || name[0] == ' ')
        return false;

    return true;
}

// http://www.osdever.net/documents/LongFileName.pdf
ErrorOr<NonnullRefPtr<SFN>> create_sfn_from_lfn(StringView lfn)
{
    ByteBuffer out;

    // 1. Remove all spaces.
    // 2. Initial periods, trailing periods, and extra periods prior to the last embedded period are removed.
    lfn = lfn.trim("."sv);
    auto last_dot_index = lfn.find_last('.');
    for (size_t i = 0; i < lfn.length(); ++i) {
        if (lfn[i] == ' ')
            continue;
        if (lfn[i] == '.' && i != last_dot_index.value())
            continue;

        TRY(out.try_append(to_ascii_uppercase(lfn[i])));
    }

    // 3. Translate all illegal 8.3 characters into "_".
    for (size_t i = 0; i < out.size(); ++i) {
        if (!is_valid_sfn_char(out[i]) && out[i] != '.')
            out[i] = '_';
    }

    // 4. If the name does not contain an extension then truncate it to 6 characters.
    // If the names does contain an extension, then truncate the first part to 6 characters and the extension to 3 characters.
    auto last_period = StringView(out).find_last('.');
    if (!last_period.has_value()) {
        auto name = TRY(out.slice(0, min(out.size(), 6)));
        return SFNUtils::SFN::try_create(move(name), {}, 1);
    } else {
        auto name = TRY(out.slice(0, min(last_period.value(), 6)));
        size_t extension_length = min(out.size() - last_period.value() - 1, 3);
        auto extension = TRY(out.slice(last_period.value() + 1, extension_length));
        return SFNUtils::SFN::try_create(move(name), move(extension), 1);
    }
}

}
