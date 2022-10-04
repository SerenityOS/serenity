/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HighResolutionTime/DOMHighResTimeStamp.h>

namespace Web::HighResolutionTime {

DOMHighResTimeStamp coarsen_time(DOMHighResTimeStamp timestamp, bool cross_origin_isolated_capability = false);
DOMHighResTimeStamp coarsened_shared_current_time(bool cross_origin_isolated_capability = false);
DOMHighResTimeStamp unsafe_shared_current_time();

}
