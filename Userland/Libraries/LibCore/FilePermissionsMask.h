/*
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <sys/stat.h>

namespace Core {

class FilePermissionsMask {
public:
    static ErrorOr<FilePermissionsMask> parse(StringView string);
    static ErrorOr<FilePermissionsMask> from_numeric_notation(StringView string);
    static ErrorOr<FilePermissionsMask> from_symbolic_notation(StringView string);

    FilePermissionsMask()
        : m_clear_mask(0)
        , m_write_mask(0)
    {
    }

    FilePermissionsMask& assign_permissions(mode_t mode);
    FilePermissionsMask& add_permissions(mode_t mode);
    FilePermissionsMask& remove_permissions(mode_t mode);

    mode_t apply(mode_t mode) const { return m_write_mask | (mode & ~m_clear_mask); }
    mode_t clear_mask() const { return m_clear_mask; }
    mode_t write_mask() const { return m_write_mask; }

private:
    mode_t m_clear_mask; // the bits that will be cleared
    mode_t m_write_mask; // the bits that will be set
};

}
