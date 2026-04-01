#!/usr/bin/env -S bash ../.port_include.sh
port='LPeg'
version='1.1.0-2'
workdir='.'
files=(
    "https://luarocks.org/manifests/gvvaughan/lpeg-${version}.rockspec#442986da58c9804396929dd1b26b72df7ffb6b8aaa2dfa5cd0884e1cbab60b3d"
)

build() {
    :
}

install() {
    # For LuaJIT
    luarocks --tree "${SERENITY_INSTALL_ROOT}/usr/local" --lua-version 5.1 install --deps-mode none --no-manifest -- "${files[0]%#*}"

    # For Lua 5.4
    luarocks --tree "${SERENITY_INSTALL_ROOT}/usr/local" --lua-version 5.4 install --deps-mode none --no-manifest -- "${files[0]%#*}"
}
