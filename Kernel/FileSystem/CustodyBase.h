/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>

namespace Kernel {

class CustodyBase {
public:
    CustodyBase(int dirfd, StringView path)
        : m_path(path)
        , m_dirfd(dirfd)
    {
    }

    CustodyBase(NonnullRefPtr<Custody> base)
        : m_base(base)
    {
    }

    CustodyBase(Custody& base)
        : m_base(base)
    {
    }

    CustodyBase(Custody const& base)
        : m_base(base)
    {
    }

    ErrorOr<NonnullRefPtr<Custody>> resolve() const;

private:
    RefPtr<Custody> const m_base;
    StringView m_path;
    int m_dirfd { -1 };
};

}
