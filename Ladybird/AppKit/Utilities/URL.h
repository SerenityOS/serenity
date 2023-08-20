/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/URL.h>

#import <System/Cocoa.h>

namespace Ladybird {

URL sanitize_url(NSString*);
URL sanitize_url(StringView);

URL rebase_url_on_serenity_resource_root(StringView);

}
