/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Core {

class LockFile {
public:
    enum class Type {
        Exclusive,
        Shared
    };

    LockFile(LockFile const& other) = delete;
    LockFile(char const* filename, Type type = Type::Exclusive);
    ~LockFile();

    bool is_held() const;
    int error_code() const { return m_errno; }
    void release();

private:
    int m_fd { -1 };
    int m_errno { 0 };
    char const* m_filename { nullptr };
};

}
