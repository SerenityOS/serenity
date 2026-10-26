#!/usr/bin/env -S bash ../.port_include.sh
port='neovim'
version='0.12.6'
files=(
    "https://github.com/neovim/neovim/archive/refs/tags/v${version}.tar.gz#8629fdc5eb951daf89a76f68cad12f2f1e03fab0d539150ba3fe6d177d254b7f"
)
useconfigure='true'
launcher_name='Neovim'
launcher_category='&Utilities'
launcher_command='/usr/local/bin/nvim'
launcher_run_in_terminal='true'
icon_file='runtime/nvim.png'

depends=(
    'libiconv'
    'libuv'
    'luv'
    'LPeg'
    'tree-sitter'
    'gettext'
    'unibilium'
    'utf8proc'

    # These tree-sitter parsers are required by the current Nvim version.
    # When updating these, make sure to also add new symlinks in the install step.
    'tree-sitter-c'
    'tree-sitter-lua'
    'tree-sitter-markdown'
    'tree-sitter-query'
    'tree-sitter-vim'
    'tree-sitter-vimdoc'
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
)

# FIXME: The host bootstrapping steps are only necessary if this port is cross-compiled.

pre_configure() {
    # Bootstrap neovim on the host.
    # Cross-compiling neovim needs a prebuilt libnlua0.so on the host and an nvim binary to build help tags.
    host_env

    run cmake -G Ninja -B build-host -S . -DCMAKE_BUILD_TYPE=Release
    run cmake --build build-host --parallel "${MAKEJOBS}"
}

configure() {
    local host_build_dir="${SERENITY_BUILD_DIR}/Ports/${port}/${workdir}/build-host"

    run cmake -G Ninja -B build -S . "${configopts[@]}" \
        -DNVIM_HOST_PRG="${host_build_dir}/bin/nvim" -DNLUA0_HOST_PRG="${host_build_dir}/lib/libnlua0.so"
}

build() {
    run cmake --build build --parallel "${MAKEJOBS}"

    # The nvim binary DT_NEEDED entry for LPeg contains a host path for some reason.
    run patchelf --replace-needed "${SERENITY_INSTALL_ROOT}/usr/local/lib/lua/5.1/lpeg.so" '/usr/local/lib/lua/5.1/lpeg.so' build/bin/nvim
}

install() {
    run cmake --install build

    # Symlink tree-sitter parsers to where neovim expects them.
    local parser_dir="${SERENITY_INSTALL_ROOT}/usr/local/lib/nvim/parser"

    mkdir -p "${parser_dir}"

    for parser in c lua markdown query vim vimdoc; do
        run_nocd ln -sf "/usr/local/lib/libtree-sitter-${parser}.so" "${parser_dir}/${parser}.so"
    done
}

post_install() {
    # Neovim uses LuaJIT, so add a drop-in fstab entry to make sure that we can use anonymous executable memory and bypass W^X.
    mkdir -p "${SERENITY_INSTALL_ROOT}/etc/fstab.d"
    echo '/usr/local/bin/nvim	/usr/local/bin/nvim	bind	bind,wxallowed,axallowed' > "${SERENITY_INSTALL_ROOT}/etc/fstab.d/neovim"
}
