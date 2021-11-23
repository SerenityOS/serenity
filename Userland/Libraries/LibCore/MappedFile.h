/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Result.h>

namespace Core {

class MappedFile : public RefCounted<MappedFile> {
    AK_MAKE_NONCOPYABLE(MappedFile);
    AK_MAKE_NONMOVABLE(MappedFile);

public:
    static ErrorOr<NonnullRefPtr<MappedFile>> map(String const& path);
    static ErrorOr<NonnullRefPtr<MappedFile>> map_from_fd_and_close(int fd, String const& path);
    ~MappedFile();

    void* data() { return m_data; }
    const void* data() const { return m_data; }

    size_t size() const { return m_size; }

    ReadonlyBytes bytes() const { return { m_data, m_size }; }

private:
    explicit MappedFile(void*, size_t);

    void* m_data { nullptr };
    size_t m_size { 0 };
};

}
