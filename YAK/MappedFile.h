/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Noncopyable.h>
#include <YAK/NonnullRefPtr.h>
#include <YAK/OSError.h>
#include <YAK/RefCounted.h>
#include <YAK/Result.h>

namespace YAK {

class MappedFile : public RefCounted<MappedFile> {
    YAK_MAKE_NONCOPYABLE(MappedFile);
    YAK_MAKE_NONMOVABLE(MappedFile);

public:
    static Result<NonnullRefPtr<MappedFile>, OSError> map(String const& path);
    static Result<NonnullRefPtr<MappedFile>, OSError> map_from_fd_and_close(int fd, String const& path);
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

using YAK::MappedFile;
