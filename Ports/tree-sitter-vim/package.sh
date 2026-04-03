#!/usr/bin/env -S bash ../.port_include.sh
port='tree-sitter-vim'
version='0.8.1'
files=(
    "https://github.com/tree-sitter-grammars/tree-sitter-vim/releases/download/v${version}/tree-sitter-vim.tar.gz#1685e18cc25aa182e51d1efa3795c3b7c845096b58a21081629c8134f34bdd1b"
)
workdir='.'

build() {
    run "$CC" -O2 -shared -fPIC -std=c11 -o libtree-sitter-vim.so src/parser.c src/scanner.c
    run "$STRIP" libtree-sitter-vim.so
}

install() {
    run /usr/bin/install -m755 libtree-sitter-vim.so "${SERENITY_INSTALL_ROOT}/usr/local/lib/libtree-sitter-vim.so"
}
