#pragma once

#include <AK/AKString.h>

class GClipboard {
public:
    static String data();
    static void set_data(const String&);
};
