#!/bin/bash
set -eu

SCRIPT=`dirname $0`
export SERENITY_ROOT=`realpath $SCRIPT/../`
prefix=$SERENITY_ROOT/Ports

export CC=i686-pc-serenity-gcc
export CXX=i686-pc-serenity-g++
export PATH=$SERENITY_ROOT/Toolchain/Local/bin:$PATH

MD5SUM=md5sum

if [ `uname -s` = "OpenBSD" ]; then
    MD5SUM="md5 -q"
fi

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
: "${auth_type:=md5}"
: "${auth_import_key:=}"
: "${auth_opts:=}"

run_nocd() {
    echo "+ $@ (nocd)"
    ("$@")
}
run() {
    echo "+ $@"
    (cd "$workdir" && "$@")
}
run_replace_in_file(){
    run perl -p -i -e "$1" $2
}
# Checks if a function is defined. In this case, if the function is not defined in the port's script, then we will use our defaults. This way, ports don't need to include these functions every time, but they can override our defaults if needed.
func_defined() {
    PATH= command -V "$1"  > /dev/null 2>&1
}

func_defined post_fetch || post_fetch() {
    :
}
fetch() {
    if [ "$auth_type" == "sig" ] && [ ! -z "${auth_import_key}" ]; then
        # import gpg key if not existing locally
        # The default keyserver keys.openpgp.org prints "new key but contains no user ID - skipped"
        # and fails. Use a different key server.
        gpg --list-keys $auth_import_key || gpg --keyserver hkps://keyserver.ubuntu.com --recv-key $auth_import_key
    fi

    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename auth_sum<<< $(echo "$f")
        echo "URL: ${url}"
        # download files
        if [ -f "$filename" ]; then
            echo "$filename already exists"
        else
            run_nocd curl ${curlopts:-} "$url" -L -o "$filename"
        fi

        # check md5sum if given
        if [ ! -z "$auth_sum" ]; then
            if [ "$auth_type" == "md5" ] || [ "$auth_type" == "sha256" ] || [ "$auth_type" == "sha1" ]; then
                echo "Expecting ${auth_type}sum: $auth_sum"
                if [ "$auth_type" == "md5" ]; then
                    calc_sum="$($MD5SUM $filename | cut -f1 -d' ')"
                elif [ "$auth_type" == "sha256" ]; then
                    calc_sum="$(sha256sum $filename | cut -f1 -d' ')"
                elif [ "$auth_type" == "sha1" ]; then
                    calc_sum="$(sha1sum $filename | cut -f1 -d' ')"
                fi
                echo "${auth_type}sum($filename) = '$calc_sum'"
                if [ "$calc_sum" != "$auth_sum" ]; then
                    # remove downloaded file to re-download on next run
                    rm -f $filename
                    echo "${auth_type}sum's mismatching, removed erronous download. Please run script again."
                    exit 1
                fi
            fi
        fi

        # extract
        if [ ! -f "$workdir"/.${filename}_extracted ]; then
            case "$filename" in
                *.tar.gz|*.tar.bz|*.tar.bz2|*.tar.xz|*.tar.lz|.tbz*|*.txz|*.tgz)
                    run_nocd tar xf "$filename"
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

    # check signature
    if [ "$auth_type" == "sig" ]; then
        if $(gpg --verify $auth_opts); then
            echo "- Signature check OK."
        else
            echo "- Signature check NOT OK"
            for f in $files; do
                rm -f $f
            done
            rm -rf "$workdir"
            echo "  Signature mismatching, removed erronous download. Please run script again."
            exit 1
        fi
    fi

    post_fetch
}

func_defined patch_internal || patch_internal() {
    # patch if it was not yet patched (applying patches multiple times doesn't work!)
    if [ -d patches ]; then
        for filepath in patches/*; do
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
    run ./"$configscript" --host=i686-pc-serenity $configopts
}
func_defined build || build() {
    run make $makeopts
}
func_defined install || install() {
    run make DESTDIR="$SERENITY_ROOT"/Build/Root $installopts install
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
    if [ ! -f "$prefix"/packages.db ]; then
        echo "Note: $prefix/packages.db does not exist. Creating."
        touch "$prefix"/packages.db
    fi
    if ! grep -E "^(auto|manual) $port $version" "$prefix"/packages.db > /dev/null; then
        echo "Adding $port $version to database of installed ports!"
        if [ "${1:-}" = "--auto" ]; then
            echo "auto $port $version" >> "$prefix"/packages.db
        else
            echo "manual $port $version" >> "$prefix"/packages.db
            if [ ! -z "${dependlist:-}" ]; then
                echo "dependency $port$dependlist" >> "$prefix/packages.db"
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
        if ! grep "$depend" "$prefix"/packages.db > /dev/null; then
            (cd "../$depend" && ./package.sh --auto)
        fi
    done
}
uninstall() {
    if grep "^manual $port " "$prefix"/packages.db > /dev/null; then
        if [ -f plist ]; then
            for f in `cat plist`; do
                case $f in
                    */)
                        run rmdir "$SERENITY_ROOT/Build/Root/$f" || true
                        ;;
                    *)
                        run rm -rf "$SERENITY_ROOT/Build/Root/$f"
                        ;;
                esac
            done
            # Without || true, mv will not be executed if you are uninstalling your only remaining port.
            grep -v "^manual $port " "$prefix"/packages.db > packages.dbtmp || true
            mv packages.dbtmp "$prefix"/packages.db
        else
            >&2 echo "Error: This port does not have a plist yet. Cannot uninstall."
        fi
    else
        >&2 echo "Error: $port is not installed. Cannot uninstall."
    fi
}
do_fetch() {
    installdepends
    echo "Fetching $port!"
    fetch
}
do_patch() {
    echo "Patching $port!"
    patch_internal
}
do_configure() {
    if [ "$useconfigure" = "true" ]; then
        echo "Configuring $port!"
        pre_configure
        chmod +x "${workdir}"/"$configscript"
        configure
    else
        echo "This port does not use a configure script. Skipping configure step."
    fi
}
do_build() {
    echo "Building $port!"
    build
}
do_install() {
    echo "Installing $port!"
    install
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
do_all() {
    do_fetch
    do_patch
    do_configure
    do_build
    do_install "${1:-}"
}

if [ -z "${1:-}" ]; then
    do_all
else
    case "$1" in
        fetch|patch|configure|build|install|clean|clean_dist|clean_all|uninstall)
            do_$1
            ;;
        --auto)
            do_all $1
            ;;
        *)
            >&2 echo "I don't understand $1! Supported arguments: fetch, patch, configure, build, install, clean, clean_dist, clean_all, uninstall."
            exit 1
            ;;
    esac
fi
