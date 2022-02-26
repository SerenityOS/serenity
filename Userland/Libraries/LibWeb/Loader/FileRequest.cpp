/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileRequest.h"

namespace Web {

FileRequest::FileRequest(String path)
    : m_path(move(path))
{
}

String FileRequest::path() const
{
    return m_path;
}

}
