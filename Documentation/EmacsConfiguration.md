# Emacs Project Configuration

Emacs can be configured with `lsp-mode` and `clangd` to work well.

### clangd

The official clangd extension can be used for C++ comprehension. Refer to [ClangdConfiguration](ClangdConfiguration.md) for how to configure clangd.

There are a few different ways to specify which clangd to use:

-   By default, without configuration `lsp-mode` will try to find and use your system `clangd`. This is the easiest solution, but your system clangd might be out of date.
-   You can manually specify any `clangd` binary with `lsp-clangd-binary-path`.
-   You can have `lsp-mode` manage your `clangd` installation with emacs' `lsp-install-server`. This will install a `clangd` binary for you.

### lsp-mode

```lisp
(use-package lsp-mode
  :hook ((c++-mode) . lsp-deferred)
  :commands lsp
  :config
  ;; clangd arguments, refer to ClangdConfiguration.md for what other arguments may be needed.
  (setq lsp-clients-clangd-args '("-j=4" "-background-index" "--log=error" "--clang-tidy" "--enable-config"))
  ;; Optionally, set the location of clangd -- See above for options.
  (setq lsp-clangd-binary-path "/usr/bin/clangd"))
```

### clang-format

There are multiple packages to handle auto formatting with
`clang-format`, within emacs. Choose what works best for your setup:

-   [format-all-mode](https://github.com/lassik/emacs-format-all-the-code)
-   [clang-format-plus](https://github.com/SavchenkoValeriy/emacs-clang-format-plus)

Alternatively, this can be done without additional packages, using `lsp-mode`.
You can use the following `.dir-locals.el` file placed in the project root:

```lisp
((c++-mode
  (eval add-hook 'before-save-hook #'lsp-format-buffer nil t)))
```
