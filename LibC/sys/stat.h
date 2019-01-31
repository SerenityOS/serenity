#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

mode_t umask(mode_t);
int chmod(const char* pathname, mode_t);
int mkdir(const char* pathname, mode_t);

inline dev_t makedev(unsigned int major, unsigned int minor) { return (minor & 0xffu) | (major << 8u) | ((minor & ~0xffu) << 12u); }
inline unsigned int major(dev_t dev) { return (dev & 0xfff00u) >> 8u; }
inline unsigned int minor(dev_t dev) { return (dev & 0xffu) | ((dev >> 12u) & 0xfff00u); }

__END_DECLS
