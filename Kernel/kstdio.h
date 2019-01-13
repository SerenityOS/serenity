#pragma once

#include "kprintf.h"

#ifndef USERLAND
#define printf dbgprintf
#endif
