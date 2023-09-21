/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/Loader/Resource.h>

namespace Web {

String resource_directory_url();
void set_resource_directory_url(String);
String error_page_url();
void set_error_page_url(String);
String directory_page_url();
void set_directory_page_url(String);

ErrorOr<String> load_error_page(AK::URL const&);

ErrorOr<String> load_file_directory_page(LoadRequest const&);

}
