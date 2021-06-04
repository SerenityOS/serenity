#!/usr/bin/env bash
set -eu

SCRIPT="$(dirname "${0}")"
export SERENITY_ARCH="${SERENITY_ARCH:-i686}"

HOST_CC="${CC:=cc}"
HOST_CXX="${CXX:=c++}"
HOST_AR="${AR:=ar}"
HOST_RANLIB="${RANLIB:=ranlib}"
HOST_PATH="${PATH:=}"
HOST_PKG_CONFIG_DIR="${PKG_CONFIG_DIR:=}"
HOST_PKG_CONFIG_SYSROOT_DIR="${PKG_CONFIG_SYSROOT_DIR:=}"
HOST_PKG_CONFIG_LIBDIR="${PKG_CONFIG_LIBDIR:=}"

DESTDIR="/"

maybe_source() {
    if [ -f "$1" ]; then
        . "$1"
    fi
}

enable_ccache() {
    if command -v ccache &>/dev/null; then
        ccache_tooldir="${SERENITY_BUILD_DIR}/ccache"
        mkdir -p "$ccache_tooldir"
        for tool in gcc g++ c++; do
            ln -sf "$(command -v ccache)" "${ccache_tooldir}/${SERENITY_ARCH}-pc-serenity-${tool}"
        done
        export PATH="${ccache_tooldir}:$PATH"
    fi
}

target_env() {
    maybe_source "${SCRIPT}/.hosted_defs.sh"
}

target_env

host_env() {
    export CC="${HOST_CC}"
    export CXX="${HOST_CXX}"
    export AR="${HOST_AR}"
    export RANLIB="${HOST_RANLIB}"
    export PATH="${HOST_PATH}"
    export PKG_CONFIG_DIR="${HOST_PKG_CONFIG_DIR}"
    export PKG_CONFIG_SYSROOT_DIR="${HOST_PKG_CONFIG_SYSROOT_DIR}"
    export PKG_CONFIG_LIBDIR="${HOST_PKG_CONFIG_LIBDIR}"
    enable_ccache
}

packagesdb="${DESTDIR}/usr/Ports/packages.db"

. "$@"
shift

: "${makeopts:=-j$(nproc)}"
: "${installopts:=}"
: "${workdir:=$port-$version}"
: "${configscript:=configure}"
: "${configopts:=}"
: "${useconfigure:=false}"
: "${depends:=}"
: "${patchlevel:=1}"
: "${auth_type:=}"
: "${auth_import_key:=}"
: "${auth_opts:=}"
: "${launcher_name:=}"
: "${launcher_category:=}"
: "${launcher_command:=}"

run_nocd() {
    echo "+ $@ (nocd)"
    ("$@")
}

run() {
    echo "+ $@"
    (cd "$workdir" && "$@")
}

run_replace_in_file() {
    run perl -p -i -e "$1" $2
}

ensure_build() {
    # Sanity check.
    if [ ! -f "${DESTDIR}/usr/lib/libc.so" ]; then
        echo "libc.so could not be found. This likely means that SerenityOS:"
        echo "- has not been built and/or installed yet"
        echo "- has been installed in an unexpected location"
        echo "The currently configured build directory is ${SERENITY_BUILD_DIR}. Resolve this issue and try again."
        exit 1
    fi
}

install_main_launcher() {
    if [ -n "$launcher_name" ] && [ -n "$launcher_category" ] && [ -n "$launcher_command" ]; then
        install_launcher "$launcher_name" "$launcher_category" "$launcher_command"
    fi
}

install_launcher() {
    if [ "$#" -lt 3 ]; then
        echo "Syntax: install_launcher <name> <category> <command>"
        exit 1
    fi
    launcher_name="$1"
    launcher_category="$2"
    launcher_command="$3"
    launcher_filename="${launcher_name,,}"
    launcher_filename="${launcher_filename// /}"
    case "$launcher_command" in
        *\ *)
            mkdir -p $DESTDIR/usr/local/libexec
            launcher_executable="/usr/local/libexec/$launcher_filename"
            cat >"$DESTDIR/$launcher_executable" <<SCRIPT
#!/bin/sh
set -e
exec $(printf '%q ' $launcher_command)
SCRIPT
            chmod +x "$DESTDIR/$launcher_executable"
            ;;
        *)
            launcher_executable="$launcher_command"
            ;;
    esac
    mkdir -p $DESTDIR/res/apps
    cat >$DESTDIR/res/apps/$launcher_filename.af <<CONFIG
[App]
Name=$launcher_name
Executable=$launcher_executable
Category=$launcher_category
CONFIG
    unset launcher_filename
}
# Checks if a function is defined. In this case, if the function is not defined in the port's script, then we will use our defaults. This way, ports don't need to include these functions every time, but they can override our defaults if needed.
func_defined() {
    PATH= command -V "$1"  > /dev/null 2>&1
}

func_defined post_fetch || post_fetch() {
    :
}
fetch() {
    if [ "$auth_type" = "sig" ] && [ ! -z "${auth_import_key}" ]; then
        # import gpg key if not existing locally
        # The default keyserver keys.openpgp.org prints "new key but contains no user ID - skipped"
        # and fails. Use a different key server.
        gpg --list-keys $auth_import_key || gpg --keyserver hkps://keyserver.ubuntu.com --recv-key $auth_import_key
    fi

    tried_download_again=0

    while true; do
        OLDIFS=$IFS
        IFS=$'\n'
        for f in $files; do
            IFS=$OLDIFS
            read url filename auth_sum<<< $(echo "$f")
            echo "Downloading URL: ${url}"

            # FIXME: Serenity's curl port does not support https, even with openssl installed.
            if which curl >/dev/null 2>&1 && ! curl https://example.com -so /dev/null; then
                url=$(echo "$url" | sed "s/^https:\/\//http:\/\//")
            fi

            # download files
            if [ -f "$filename" ]; then
                echo "$filename already exists"
            else
                if which curl; then
                    run_nocd curl ${curlopts:-} "$url" -L -o "$filename"
                else
                    run_nocd pro "$url" > "$filename"
                fi
            fi
        done

        verification_failed=0

        OLDIFS=$IFS
        IFS=$'\n'
        for f in $files; do
            IFS=$OLDIFS
            read url filename auth_sum<<< $(echo "$f")

            # check sha256sum if given
            if [ "$auth_type" = "sha256" ]; then
                echo "Expecting ${auth_type}sum: $auth_sum"
                calc_sum="$(sha256sum $filename | cut -f1 -d' ')"
                echo "${auth_type}sum($filename) = '$calc_sum'"
                if [ "$calc_sum" != "$auth_sum" ]; then
                    # remove downloaded file to re-download on next run
                    rm -f $filename
                    echo "${auth_type}sums mismatching, removed erronous download."
                    if [ $tried_download_again -eq 1 ]; then
                        echo "Please run script again."
                        exit 1
                    fi
                    echo "Trying to download the files again."
                    tried_download_again=1
                    verification_failed=1
                fi
            fi
        done

        # check signature
        if [ "$auth_type" = "sig" ]; then
            if $NO_GPG; then
                echo "WARNING: gpg signature check was disabled by --no-gpg-verification"
            else
                if $(gpg --verify $auth_opts); then
                    echo "- Signature check OK."
                else
                    echo "- Signature check NOT OK"
                    for f in $files; do
                        rm -f $f
                    done
                    rm -rf "$workdir"
                    echo "  Signature mismatching, removed erronous download."
                    if [ $tried_download_again -eq 1 ]; then
                        echo "Please run script again."
                        exit 1
                    fi
                    echo "Trying to download the files again."
                    tried_download_again=1
                    verification_failed=1
                fi
            fi
        fi

        if [ $verification_failed -ne 1 ]; then
            break
        fi
    done

    # extract
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename auth_sum<<< $(echo "$f")

        if [ ! -f "$workdir"/.${filename}_extracted ]; then
            case "$filename" in
                *.tar.gz|*.tgz)
                    run_nocd tar -xzf "$filename"
                    run touch .${filename}_extracted
                    ;;
                *.tar.gz|*.tar.bz|*.tar.bz2|*.tar.xz|*.tar.lz|.tbz*|*.txz|*.tgz)
                    run_nocd tar -xf "$filename"
                    run touch .${filename}_extracted
                    ;;
                *.gz)
                    run_nocd gunzip "$filename"
                    run touch .${filename}_extracted
                    ;;
                *.zip)
                    run_nocd bsdtar xf "$filename" || run_nocd unzip -qo "$filename"
                    run touch .${filename}_extracted
                    ;;
                *.asc)
                    run_nocd gpg --import "$filename" || true
                    ;;
                *)
                    echo "Note: no case for file $filename."
                    ;;
            esac
        fi
    done

    post_fetch
}

func_defined patch_internal || patch_internal() {
    # patch if it was not yet patched (applying patches multiple times doesn't work!)
    if [ -d patches ]; then
        for filepath in patches/*.patch; do
            filename=$(basename $filepath)
            if [ ! -f "$workdir"/.${filename}_applied ]; then
                run patch -p"$patchlevel" < "$filepath"
                run touch .${filename}_applied
            fi
        done
    fi
}
func_defined pre_configure || pre_configure() {
    :
}
func_defined configure || configure() {
    chmod +x "${workdir}"/"$configscript"
    run ./"$configscript" --host="${SERENITY_ARCH}-pc-serenity" $configopts
}
func_defined post_configure || post_configure() {
    :
}
func_defined build || build() {
    run make $makeopts
}
func_defined install || install() {
    run make DESTDIR=$DESTDIR $installopts install
}
func_defined post_install || post_install() {
    echo
}
func_defined clean || clean() {
    rm -rf "$workdir" *.out
}
func_defined clean_dist || clean_dist() {
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename hash <<< $(echo "$f")
        rm -f "$filename"
    done
}
func_defined clean_all || clean_all() {
    rm -rf "$workdir" *.out
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename hash <<< $(echo "$f")
        rm -f "$filename"
    done
}
addtodb() {
    if [ ! -f "$packagesdb" ]; then
        echo "Note: $packagesdb does not exist. Creating."
        mkdir -p "${DESTDIR}/usr/Ports/"
        touch "$packagesdb"
    fi
    if ! grep -E "^(auto|manual) $port $version" "$packagesdb" > /dev/null; then
        echo "Adding $port $version to database of installed ports!"
        if [ "${1:-}" = "--auto" ]; then
            echo "auto $port $version" >> "$packagesdb"
        else
            echo "manual $port $version" >> "$packagesdb"
            if [ ! -z "${dependlist:-}" ]; then
                echo "dependency $port$dependlist" >> "$packagesdb"
            fi
        fi
    else
        >&2 echo "Warning: $port $version already installed. Not adding to database of installed ports!"
    fi
}
installdepends() {
    for depend in $depends; do
        dependlist="${dependlist:-} $depend"
    done
    for depend in $depends; do
        if ! grep "$depend" "$packagesdb" > /dev/null; then
            (cd "../$depend" && ./package.sh --auto)
        fi
    done
}
uninstall() {
    if grep "^manual $port " "$packagesdb" > /dev/null; then
        if [ -f plist ]; then
            for f in `cat plist`; do
                case $f in
                    */)
                        run rmdir "${DESTDIR}/$f" || true
                        ;;
                    *)
                        run rm -rf "${DESTDIR}/$f"
                        ;;
                esac
            done
            # Without || true, mv will not be executed if you are uninstalling your only remaining port.
            grep -v "^manual $port " "$packagesdb" > packages.db.tmp || true
            mv packages.db.tmp "$packagesdb"
        else
            >&2 echo "Error: This port does not have a plist yet. Cannot uninstall."
        fi
    else
        >&2 echo "Error: $port is not installed. Cannot uninstall."
    fi
}
do_installdepends() {
    echo "Installing dependencies of $port!"
    installdepends
}
do_fetch() {
    echo "Fetching $port!"
    fetch
}
do_patch() {
    echo "Patching $port!"
    patch_internal
}
do_configure() {
    ensure_build
    if [ "$useconfigure" = "true" ]; then
        echo "Configuring $port!"
        pre_configure
        configure
        post_configure
    else
        echo "This port does not use a configure script. Skipping configure step."
    fi
}
do_build() {
    ensure_build
    echo "Building $port!"
    build
}
do_install() {
    ensure_build
    echo "Installing $port!"
    install
    install_main_launcher
    post_install
    addtodb "${1:-}"
}
do_clean() {
    echo "Cleaning workdir and .out files in $port!"
    clean
}
do_clean_dist() {
    echo "Cleaning dist in $port!"
    clean_dist
}
do_clean_all() {
    echo "Cleaning all in $port!"
    clean_all
}
do_uninstall() {
    echo "Uninstalling $port!"
    uninstall
}
do_showproperty() {
    if [ -z ${!1+x} ]; then
        echo "Property '$1' is not set." >&2
        exit 1
    fi
    echo ${!1}
}
do_all() {
    do_installdepends
    do_fetch
    do_patch
    do_configure
    do_build
    do_install "${1:-}"
}

NO_GPG=false
parse_arguments() {
    if [ -z "${1:-}" ]; then
        do_all
    else
        case "$1" in
            fetch|patch|configure|build|install|installdepends|clean|clean_dist|clean_all|uninstall|showproperty)
                method=$1
                shift
                do_${method} "$@"
                ;;
            --auto)
                do_all $1
                ;;
            --no-gpg-verification)
                NO_GPG=true
                shift
                parse_arguments $@
                ;;
            *)
                >&2 echo "I don't understand $1! Supported arguments: fetch, patch, configure, build, install, installdepends, clean, clean_dist, clean_all, uninstall, showproperty."
                exit 1
                ;;
        esac
    fi
}

parse_arguments $@
