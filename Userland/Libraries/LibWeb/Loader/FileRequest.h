/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>

namespace Web {

class FileRequest : public RefCounted<FileRequest> {
public:
    explicit FileRequest(DeprecatedString path);

    DeprecatedString path() const;

    Function<void(ErrorOr<i32>)> on_file_request_finish;

private:
    DeprecatedString m_path {};
};

}
