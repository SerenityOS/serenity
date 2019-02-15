#pragma once

extern "C" {
int dbgprintf(const char *fmt, ...);
int kprintf(const char *fmt, ...);
int ksprintf(char* buf, const char *fmt, ...);
}
