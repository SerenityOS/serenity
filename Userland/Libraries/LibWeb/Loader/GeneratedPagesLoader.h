/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/Loader/Resource.h>

namespace Web {

ErrorOr<String> load_error_page(AK::URL const&);

ErrorOr<String> load_file_directory_page(AK::URL const&);

ErrorOr<String> load_about_version_page();

}
