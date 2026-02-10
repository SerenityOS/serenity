# Patches for bdwgc on SerenityOS

## `0001-test-Set-NTHREADS-to-0.patch`

test: Set NTHREADS to 0

It crashes otherwise:

```
0x00000015e4c9b017: [/usr/lib/libsystem.so] syscall1 +0x7 (syscall.cpp:20 => syscall.cpp:19)
0x00000002018903eb: [/home/anon/gctest] GC_suspend_handler +0xab (pthread_stop_world.c:406 => pthread_stop_world.c:263)
0x0000001eb725200d: ???
0x000000050209d666: [/usr/lib/libc.so] sem_timedwait +0x86 (serenity.h:43 => semaphore.cpp:372)
0x000000020188fb26: [/home/anon/gctest] GC_pthread_create +0x106 (pthread_support.c:2356)
0x0000000201877d2e: [/home/anon/gctest] main +0x12e (test.c:2438)
0x0000000201878064: [/home/anon/gctest] _entry +0x24 (crt0.cpp:47)
```

