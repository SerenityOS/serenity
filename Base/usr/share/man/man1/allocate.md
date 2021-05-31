## Name

allocate - allocate memory

## Synopsis

```**sh
$ allocate [number [unit (B/KiB/MiB)]]
```

## Description

`allocate` allocates a specific amount of virtual memory (specified in `number` and `unit`, by default 50 MiB), It also writes to each allocated page and then sleeps for 10 seconds. It is primarily used to test the kernel's memory management capabilities.

## Arguments

* `number`: A number of `units` to allocate; the default is **50**
* `unit`: Data size unit, can be `B` (bytes), `KiB` (kibibytes) or `MiB` (mebibytes); the default is **MiB**

## Examples

```sh
$ allocate 100 MiB
allocating memory (104857600 bytes)...
done in 13ms
writing one byte to each page of allocated memory...
step took 46ms (217.391304MiB/s)
step took 32ms (312.500000MiB/s)
step took 31ms (322.580645MiB/s)
step took 55ms (181.818181MiB/s)
step took 35ms (285.714285MiB/s)
step took 40ms (250.000000MiB/s)
step took 39ms (256.410256MiB/s)
step took 52ms (192.307692MiB/s)
step took 44ms (227.272727MiB/s)
done in 426ms
sleeping for ten seconds...
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
done in 119ms
```
