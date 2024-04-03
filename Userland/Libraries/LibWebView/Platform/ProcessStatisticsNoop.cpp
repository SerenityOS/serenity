/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/Platform/ProcessStatistics.h>

namespace WebView {

ErrorOr<void> update_process_statistics(ProcessStatistics&)
{
    return {};
}

}
