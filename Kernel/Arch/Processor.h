#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/Processor.h>
#elif ARCH(AARCH64)
#   error "Need to implement this file"
#else
#   error "Unknown architecture"
#endif