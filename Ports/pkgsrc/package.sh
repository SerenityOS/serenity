#!/usr/bin/env -S bash ../.port_include.sh
port=pkgsrc
version=2021Q2
useconfigure=false
files="https://cdn.netbsd.org/pub/pkgsrc/pkgsrc-$version/pkgsrc-$version.tar.gz pkgsrc-$version.tar.gz d880f2c842c9e50e8513b96d683b1f80d62b98058f2bce6920ccf57147c17863
       https://git.savannah.gnu.org/cgit/config.git/plain/config.guess?id=805517123cbfe33d17c989a18e78c5789fab0437 config.guess af8a1922c9b3c240bf2119d4ec0965a0b5ec36b1016017ba66db44b3b53e9cea
       https://git.savannah.gnu.org/cgit/config.git/plain/config.sub?id=805517123cbfe33d17c989a18e78c5789fab0437 config.sub 325caa596fa7aee6495bc416af6ed7d9e36b23aa2e0f6fba04cbb07838b37a27"
auth_type=sha256
depends=("bash" "binutils" "gcc" "grep" "mawk" "make" "sed")
workdir="pkgsrc"

port_path=$(realpath $(dirname ${BASH_SOURCE[0]}))

post_fetch() {
    cp config.guess "$workdir/config.guess"
    cp config.sub "$workdir/config.sub"
}

build() {
    files_config_guess=$(cd $workdir && find . -mindepth 2 -name config.guess)
    files_config_sub=$(cd $workdir && find . -mindepth 2 -name config.sub)
    for i in $files_config_guess; do
        run cp config.guess "$i"
    done
    for i in $files_config_sub; do
        run cp config.sub "$i"
    done
}

install() {
    pkgsrc_dir="${SERENITY_BUILD_DIR}/Root/home/anon/Source/pkgsrc"
    run rm -rf "$pkgsrc_dir"
    run mkdir -p "$pkgsrc_dir"
    run cp -r . "$pkgsrc_dir"
}

post_install() {
    echo +===================================================================
    echo "| Successfully prepared the sources for pkgsrc-$version!"
    echo "| The bootstrapping itself has to be done inside serenity."
    echo "| To continue, re-image and run the VM, then do the following:"
    echo '| '
    echo '| $ cd Source/pkgsrc/bootstrap'
    echo '| $ ./bootstrap --full --unprivileged'
    echo '| '
    echo +===================================================================
}
