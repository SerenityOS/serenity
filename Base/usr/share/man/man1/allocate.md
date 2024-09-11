## Name

allocate - allocate memory

## Synopsis

```**sh
$ allocate [--unit B/KiB/MiB/GiB] [--sleep-time N] [number]
```

## Description

`allocate` allocates a specific amount of virtual memory. If nothing is specified
then it will allocate 100 bytes of memory.
If `number` is specified without `unit`, it will default to `number` of bytes.
It also writes to each allocated page and then sleeps for N seconds (by default 10).
It is primarily used to test the kernel's memory management capabilities.

## Options

-   `-u`, `--size-unit`: Allocation's Size Unit (Base 2 units - B, KiB, MiB or GiB)
-   `-n`, `--sleep-time`: Number of seconds to sleep before freeing memory

## Examples

```sh
$ allocate 500
allocating memory (500 bytes)...
done in 0ms
writing one byte to each page of allocated memory...
done in 0ms
sleeping for 10 seconds...
0
1
2
3
4
5
6
7
8
9
done.
freeing memory...
done in 0ms

$ allocate 500 -u KiB
allocating memory (512000 bytes)...
done in 0ms
writing one byte to each page of allocated memory...
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
done in 4ms
sleeping for 10 seconds...
0
1
2
3
4
5
6
7
8
9
done.
freeing memory...
done in 0ms

$ allocate -u KiB -n 2 500
allocating memory (512000 bytes)...
done in 0ms
writing one byte to each page of allocated memory...
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
step took 1ms (46.875MiB/s)
done in 0ms
sleeping for 2 seconds...
0
1
done.
freeing memory...
done in 0ms

```
