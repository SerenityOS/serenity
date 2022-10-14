/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileRequest.h"

namespace Web {

FileRequest::FileRequest(DeprecatedString path)
    : m_path(move(path))
{
}

DeprecatedString FileRequest::path() const
{
    return m_path;
}

}
