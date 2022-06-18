# Emacs Project Configuration

Emacs can be configured with `lsp-mode` and `clangd` to work well.

### clangd

The official clangd extension can be used for C++ comprehension. You
can use the following `.clangd` file placed in the project root:

```yaml
CompileFlags:
  CompilationDatabase: Build/i686
  Add:
    - "-D__serenity__"
    - "-UNO_TLS"
    - "-I/path/to/serenity/Toolchain/Local/i686/i686-pc-serenity/include/c++/11.2.0"
    - "-I/path/to/serenity/Toolchain/Local/i686/i686-pc-serenity/include/c++/11.2.0/i686-pc-serenity"
```

You will need to change `/path/to/serenity` and change `11.2.0` to
whatever your GCC toolchain version at the time is.

Run cmake (`Meta/serenity.sh run` or similar) at least once for this
to work, as it will generate the `Build/i686/compile_commands.json`
that is needed by `clangd`.

### lsp-mode

```lisp
(use-package lsp-mode
  :hook ((c++-mode) . lsp-deferred)
  :commands lsp
  :config
  (setq lsp-clients-clangd-args '("-j=4" "-background-index" "--log=error" "--clang-tidy" "--enable-config"))
  ;; Optionally, set the location of clangd -- See notes below for options.
  (setq lsp-clangd-binary-path "/usr/bin/clangd"))

```

### clangd

There are a few different ways to specify which clangd to use:

- By default, without configuration `lsp-mode` will try to find and use your system `clangd`. This is the easiest solution, but your system clangd might be out of date.
- You can manually specify any `clangd` binary with `lsp-clangd-binary-path`, as shown in the use-package example above.
- You can have `lsp-mode` manage your `clangd` installation with emacs' `lsp-install-server`. This will install a `clangd` binary for you.
- You can build the LLVM toolchain, including `clangd`, from Serenity's repository. This is an advanced option that is not currently documented.

### clang-format

There are multiple packages to handle auto formatting with
`clang-format`, within emacs. Choose what works best for your setup:

- [format-all-mode](https://github.com/lassik/emacs-format-all-the-code)
- [clang-format-plus](https://github.com/SavchenkoValeriy/emacs-clang-format-plus)

```
