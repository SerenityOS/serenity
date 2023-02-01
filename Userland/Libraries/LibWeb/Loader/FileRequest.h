/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/Function.h>

namespace Web {

class FileRequest {
public:
    FileRequest(DeprecatedString path, Function<void(ErrorOr<i32>)> on_file_request_finish);

    DeprecatedString path() const;

    Function<void(ErrorOr<i32>)> on_file_request_finish;

private:
    DeprecatedString m_path {};
};

}
