

extern "C" {

// This file defines the dynamic TLS implementation
// It should only be included in dynamically linked libraries.
// Statically linked libraries can use the static TLS currently (2020/01/18) defined in the Kernel

typedef struct
{
    unsigned long ti_module;
    unsigned long ti_offset;
} tls_index;

// Page 12 of drepper/tls.pdf
// This is using the GNU model for TLS, rather than the one created by Sun
// The TLS index is passed in via %eax, rather than on the the stack
// If we end up using the Sun Model somehow, we'd know because the symbol __tls_get_addr would be missing  (2 _)
extern void* ___tls_get_addr (tls_index* ti) __attribute__ ((__regparm__ (1)));

// This method is defined in the dynamic loader
extern unsigned char* allocate_tls_block(unsigned long module);

// Time for some shenanigans
// The 'thread pointer' is stored in %gs
// We should be able to call into the dynamic loader via the magic of weak symbols for this?
// Where the dynamic loader sets up the 'dynamic thread vector'
// FIXME: Do this properly, per Drepper's document here: https://akkadia.org/drepper/tls.pdf
void* ___tls_get_addr (tls_index*)
{
    unsigned int thread_ptr;
    asm volatile("movl %%gs:0, %%eax"
                 : "=a"(thread_ptr));
    return (void*)thread_ptr;
}

}
