/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Loader/Resource.h>

namespace Web {

ErrorOr<DeprecatedString> load_file_directory_page(LoadRequest const&);
DeprecatedString resource_directory_url();
void set_resource_directory_url(DeprecatedString);
DeprecatedString directory_page_url();
void set_directory_page_url(DeprecatedString);

}
