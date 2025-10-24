# Patches for bdwgc on SerenityOS

## `0001-test-Set-NTHREADS-to-0.patch`

test: Set NTHREADS to 0

It crashes otherwise:

```
0x000000078c79c017: [/usr/lib/libsystem.so] syscall1 +0x7 (syscall.cpp:20 => syscall.cpp:19)
0x00000012967e283d: [/usr/lib/libc.so] pthread_mutex_lock.localalias +0x6d (serenity.h:43 => pthread_integration.cpp:158)
0x0000000143bf0344: [/home/anon/gctest] GC_thread_is_registered +0x74 (pthread_support.c:911)
0x0000000143bdb46f: [/home/anon/gctest] run_one_test +0x1f (test.c:1576)
0x0000000143bd8e5d: [/home/anon/gctest] main +0x1ed (test.c:2731)
0x0000000143bd9854: [/home/anon/gctest] _entry +0x24 (crt0.cpp:47)
```

## `0002-Add-explicit-integer-to-pointer-cast.patch`

Add explicit integer to pointer cast

```
[bdwgc/build] In file included from extra/gc.c:63:
[bdwgc/build] extra/../os_dep.c: In function 'GC_get_stack_base':
[bdwgc/build] extra/../os_dep.c:878:16: error: assignment to 'void *' from 'long unsigned int' makes pointer from integer without a cast [-Wint-conversion]
[bdwgc/build]   878 |   sb->mem_base = base + size;
[bdwgc/build]       |
```

