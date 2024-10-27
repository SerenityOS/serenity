# Patches for awk on SerenityOS

## `0001-Make-it-possible-to-override-HOSTCC-and-CC-from-the-.patch`

Make it possible to override HOSTCC and CC from the environment


## `0002-Fix-duplicate-case-value-build-error.patch`

Fix duplicate case value build error

```
[awk/build] main.c: In function 'fpecatch':
[awk/build] main.c:92:9: error: duplicate case value
[awk/build]    92 |         case 0:
[awk/build]       |         ^~~~
[awk/build] main.c:68:9: note: previously used here
[awk/build]    68 |         case FPE_INTDIV:
[awk/build]       |         ^~~~
```

