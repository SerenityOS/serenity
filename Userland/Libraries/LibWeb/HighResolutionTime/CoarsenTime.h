/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::HighResolutionTime {

// https://w3c.github.io/hr-time/#dfn-coarsen-time
double coarsen_time(double timestamp, bool cross_origin_isolated_capability = false);

}
