/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    int power_state_switch_node = TRY(Core::System::open("/sys/firmware/power_state", O_WRONLY));

    char const* value = "1";
    TRY(Core::System::write(power_state_switch_node, ReadonlyBytes(value, 1)));

    return 0;
}
