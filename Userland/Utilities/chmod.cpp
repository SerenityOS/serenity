/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

/* the new mode will be computed using the boolean function(for each bit):

    |current mode|removal mask|applying mask|result |
    |      0     |      0     |      0      |   0   |
    |      0     |      0     |      1      |   1   |
    |      0     |      1     |      0      |   0   |
    |      0     |      1     |      1      |   1   | ---> find the CNF --> find the minimal CNF
    |      1     |      0     |      0      |   1   |
    |      1     |      0     |      1      |   1   |
    |      1     |      1     |      0      |   0   |
    |      1     |      1     |      1      |   1   |
*/

class Mask {
private:
    mode_t removal_mask;  // the bits that will be removed
    mode_t applying_mask; // the bits that will be set

public:
    Mask()
        : removal_mask(0)
        , applying_mask(0)
    {
    }

    Mask& operator|=(const Mask& other)
    {
        removal_mask |= other.removal_mask;
        applying_mask |= other.applying_mask;

        return *this;
    }

    mode_t& get_removal_mask() { return removal_mask; }
    mode_t& get_applying_mask() { return applying_mask; }
};

Optional<Mask> string_to_mode(char access_scope, StringView access_string);
Optional<Mask> apply_permission(char access_scope, char permission, char operation);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath fattr", nullptr));

    if (arguments.strings.size() < 3) {
        warnln("usage: chmod <octal-mode> <path...>");
        warnln("       chmod [[ugoa][+-=][rwx...],...] <path...>");
        return 1;
    }

    Mask mask;

    /* compute a mask */

    if (arguments.strings[1][0] >= '0' && arguments.strings[1][0] <= '7') {
        if (sscanf(arguments.strings[1].to_string().characters(), "%ho", &mask.get_applying_mask()) != 1) {
            perror("sscanf");
            return 1;
        }
        mask.get_removal_mask() = ~mask.get_applying_mask();
    } else {
        auto access_strings = arguments.strings[1].split_view(',');
        for (auto access_string : access_strings) {
            Optional<Mask> tmp_mask;
            switch (access_string[0]) {
            case 'u':
                tmp_mask = string_to_mode('u', access_string);
                break;
            case 'g':
                tmp_mask = string_to_mode('g', access_string);
                break;
            case 'o':
                tmp_mask = string_to_mode('o', access_string);
                break;
            case 'a':
                tmp_mask = string_to_mode('a', access_string);
                break;
            case '=':
            case '+':
            case '-':
                tmp_mask = string_to_mode('a', access_string);
                break;
            }
            if (!tmp_mask.has_value()) {
                warnln("chmod: invalid mode: {}", arguments.strings[1]);
                return 1;
            }
            mask |= tmp_mask.value();
        }
    }

    /* set the mask for each file's permissions */
    size_t i = 2;
    while (i < arguments.strings.size()) {
        auto current_access = TRY(Core::System::stat(arguments.strings[i]));
        /* found the minimal CNF by The Quine–McCluskey algorithm and use it */
        mode_t mode = mask.get_applying_mask()
            | (current_access.st_mode & ~mask.get_removal_mask());
        TRY(Core::System::chmod(arguments.strings[i++], mode));
    }

    return 0;
}

Optional<Mask> string_to_mode(char access_scope, StringView access_string)
{
    auto get_operation = [](StringView s) {
        for (auto c : s) {
            if (c == '+' || c == '-' || c == '=')
                return c;
        }
        return ' ';
    };

    auto operation = get_operation(access_string);
    if (operation == ' ') {
        return {};
    }

    Mask mask;
    if (operation == '=') {
        switch (access_scope) {
        case 'u':
            mask.get_removal_mask() = (S_IRUSR | S_IWUSR | S_IXUSR);
            break;
        case 'g':
            mask.get_removal_mask() = (S_IRGRP | S_IWGRP | S_IXGRP);
            break;
        case 'o':
            mask.get_removal_mask() = (S_IROTH | S_IWOTH | S_IXOTH);
            break;
        case 'a':
            mask.get_removal_mask() = (S_IRUSR | S_IWUSR | S_IXUSR
                | S_IRGRP | S_IWGRP | S_IXGRP
                | S_IROTH | S_IWOTH | S_IXOTH);
            break;
        }
        operation = '+';
    }

    for (size_t i = 1; i < access_string.length(); i++) {
        char permission = access_string[i];
        if (permission == '+' || permission == '-' || permission == '=')
            continue;

        Optional<Mask> tmp_mask;
        tmp_mask = apply_permission(access_scope, permission, operation);
        if (!tmp_mask.has_value()) {
            return {};
        }
        mask |= tmp_mask.value();
    }

    return mask;
}

Optional<Mask> apply_permission(char access_scope, char permission, char operation)
{
    if (permission != 'r' && permission != 'w' && permission != 'x') {
        return {};
    }

    Mask mask;
    mode_t tmp_mask = 0;
    switch (access_scope) {
    case 'u':
        switch (permission) {
        case 'r':
            tmp_mask = S_IRUSR;
            break;
        case 'w':
            tmp_mask = S_IWUSR;
            break;
        case 'x':
            tmp_mask = S_IXUSR;
            break;
        }
        break;
    case 'g':
        switch (permission) {
        case 'r':
            tmp_mask = S_IRGRP;
            break;
        case 'w':
            tmp_mask = S_IWGRP;
            break;
        case 'x':
            tmp_mask = S_IXGRP;
            break;
        }
        break;
    case 'o':
        switch (permission) {
        case 'r':
            tmp_mask = S_IROTH;
            break;
        case 'w':
            tmp_mask = S_IWOTH;
            break;
        case 'x':
            tmp_mask = S_IXOTH;
            break;
        }
        break;
    case 'a':
        mask |= apply_permission('u', permission, operation).value();
        mask |= apply_permission('g', permission, operation).value();
        mask |= apply_permission('o', permission, operation).value();
        break;
    }

    if (operation == '+') {
        mask.get_applying_mask() |= tmp_mask;
    } else {
        mask.get_removal_mask() |= tmp_mask;
    }

    return mask;
}
