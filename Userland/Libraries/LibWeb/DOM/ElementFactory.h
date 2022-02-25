/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>

namespace Web::DOM {

NonnullRefPtr<Element> create_element(Document&, FlyString tag_name, FlyString namespace_);

}
