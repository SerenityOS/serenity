# Patches for Python 3.9 on SerenityOS

## `define-have-sigset-t.patch`

Ensures `HAVE_SIGSET_T` is defined, as we *do* have `sigset_t` but it's not detected properly due to some related functions being missing.

## `define-py-force-utf8-locale.patch`

Enforce UTF-8 as encoding by defining `_Py_FORCE_UTF8_LOCALE`.

## `disable-setrlimit.patch`

Disables check for `RLIMIT_CORE` and subsequent `setrlimit()` call. Would be enabled otherwise as we *do* have `<sys/resource.h>` and therefore `HAVE_SYS_RESOURCE_H`.

## `fix-autoconf.patch`

As usual, make the `configure` script recognize Serenity.

## `fix-hidden-symbol-referenced-by-dso.patch`

Fix a weird build issue of `python` and other provided binaries by marking the `main()` functions `Py_EXPORTED_SYMBOL`.

```text
hidden symbol `main' in Programs/python.o is referenced by DSO
```

Not sure what the proper fix for this is, but it works fine.

## `remove-setlocale-from-preconfig.patch`

Our stub implementation of `setlocale()` always returns `nullptr`, which the interpreter considers critical enough to exit right away.

## `tweak-unsupported-printf-format-specifiers.patch`

Replace uses of `%.Ns` with `%s` as the former is not supported by our `printf` implementation yet and would result in empty strings. It uses `snprintf` already, so this is safe.

## `use-rtld-lazy-for-dlopenflags.patch`

We have `RTLD_NOW` defined but don't actually support it, so use the provided `RTLD_LAZY` fallback. Doesn't help the dynamic library module import assertion though.
