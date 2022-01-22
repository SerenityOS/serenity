/*
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/CharacterTypes.h>
#include <AK/StringUtils.h>

#include <LibCore/FilePermissionsMask.h>

namespace Core {

enum State {
    Classes,
    Mode
};

enum ClassFlag {
    Other = 1,
    Group = 2,
    User = 4,
    All = 7
};

enum Operation {
    Add,
    Remove,
    Assign,
};

ErrorOr<FilePermissionsMask> FilePermissionsMask::parse(StringView string)
{
    return (!string.is_empty() && is_ascii_digit(string[0]))
        ? from_numeric_notation(string)
        : from_symbolic_notation(string);
}

ErrorOr<FilePermissionsMask> FilePermissionsMask::from_numeric_notation(StringView string)
{
    mode_t mode = AK::StringUtils::convert_to_uint_from_octal<u16>(string).value_or(01000);
    if (mode > 0777)
        return Error::from_string_literal("invalid octal representation"sv);
    return FilePermissionsMask().assign_permissions(mode);
}

ErrorOr<FilePermissionsMask> FilePermissionsMask::from_symbolic_notation(StringView string)
{
    auto mask = FilePermissionsMask();

    u8 state = State::Classes;
    u8 classes = 0;
    u8 operation = 0;

    for (auto ch : string) {
        switch (state) {
        case State::Classes: {
            // zero or more [ugoa] terminated by one operator [+-=]
            if (ch == 'u')
                classes |= ClassFlag::User;
            else if (ch == 'g')
                classes |= ClassFlag::Group;
            else if (ch == 'o')
                classes |= ClassFlag::Other;
            else if (ch == 'a')
                classes = ClassFlag::All;
            else {
                if (ch == '+')
                    operation = Operation::Add;
                else if (ch == '-')
                    operation = Operation::Remove;
                else if (ch == '=')
                    operation = Operation::Assign;
                else if (classes == 0)
                    return Error::from_string_literal("invalid class: expected 'u', 'g', 'o' or 'a'"sv);
                else
                    return Error::from_string_literal("invalid operation: expected '+', '-' or '='"sv);

                // if an operation was specified without a class, assume all
                if (classes == 0)
                    classes = ClassFlag::All;

                state = State::Mode;
            }

            break;
        }

        case State::Mode: {
            // one or more [rwx] terminated by a comma

            // End of mode part, expect class next
            if (ch == ',') {
                state = State::Classes;
                classes = operation = 0;
                continue;
            }

            mode_t write_bits = 0;

            if (ch == 'r')
                write_bits = 4;
            else if (ch == 'w')
                write_bits = 2;
            else if (ch == 'x')
                write_bits = 1;
            else
                return Error::from_string_literal("invalid symbolic permission: expected 'r', 'w' or 'x'"sv);

            mode_t clear_bits = operation == Operation::Assign ? 7 : write_bits;

            // Update masks one class at a time in other, group, user order
            for (auto cls = classes; cls != 0; cls >>= 1) {
                if (cls & 1) {
                    if (operation == Operation::Add || operation == Operation::Assign)
                        mask.add_permissions(write_bits);
                    if (operation == Operation::Remove || operation == Operation::Assign)
                        mask.remove_permissions(clear_bits);
                }
                write_bits <<= 3;
                clear_bits <<= 3;
            }

            break;
        }

        default:
            VERIFY_NOT_REACHED();
        }
    }

    return mask;
}

FilePermissionsMask& FilePermissionsMask::assign_permissions(mode_t mode)
{
    m_write_mask = mode;
    m_clear_mask = 0777;
    return *this;
}

FilePermissionsMask& FilePermissionsMask::add_permissions(mode_t mode)
{
    m_write_mask |= mode;
    return *this;
}

FilePermissionsMask& FilePermissionsMask::remove_permissions(mode_t mode)
{
    m_clear_mask |= mode;
    return *this;
}

}
