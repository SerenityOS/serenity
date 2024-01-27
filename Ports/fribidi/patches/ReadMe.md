# SerenityOS patches for fribidi

## patches/0001-meson-remove-ansi-for-serenity.patch

The following lines were removed from `meson.build` [(GitHub)](https://github.com/fribidi/fribidi/blob/5b9a242cbbb0cf27d20da9941667abfc63808c19/meson.build#L24-L26):

```
-if cc.get_id() == 'gcc' and cc.has_argument('-ansi')
-  add_project_arguments('-ansi', language: 'c')
-endif
```

The inline keyword (C99) is too new for ANSI/C89.

Without this patch, the compiler freaks out because fribidi
relies on inline functions.

---

## Additional Resources

- [Does ANSI-C not know the inline keyword? (StackOverflow)](https://stackoverflow.com/questions/12151168/does-ansi-c-not-know-the-inline-keyword)
- [inline function specifier (cppreference.com)](https://en.cppreference.com/w/c/language/inline)
