/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    int power_state_switch_node = open("/sys/firmware/power_state", O_WRONLY);
    if (power_state_switch_node < 0) {
        perror("open");
        return 1;
    }
    const char* value = "2";
    if (write(power_state_switch_node, value, 1) < 0) {
        perror("write");
        return 1;
    }
    return 0;
}
