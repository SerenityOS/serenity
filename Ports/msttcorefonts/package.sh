#!/usr/bin/env -S bash ../.port_include.sh

port='msttcorefonts'
# Note: Using 'git' as a placeholder since msttcorefonts versioning is for .spec files, which are not used here.
version=git
archive='https://sourceforge.net/projects/corefonts/files/the%20fonts/final'
files=(
    "${archive}/andale32.exe#0524fe42951adc3a7eb870e32f0920313c71f170c859b5f770d82b4ee111e970"
    "${archive}/arial32.exe#85297a4d146e9c87ac6f74822734bdee5f4b2a722d7eaa584b7f2cbf76f478f6"
    "${archive}/arialb32.exe#a425f0ffb6a1a5ede5b979ed6177f4f4f4fdef6ae7c302a7b7720ef332fec0a8"
    "${archive}/comic32.exe#9c6df3feefde26d4e41d4a4fe5db2a89f9123a772594d7f59afd062625cd204e"
    "${archive}/courie32.exe#bb511d861655dde879ae552eb86b134d6fae67cb58502e6ff73ec5d9151f3384"
    "${archive}/georgi32.exe#2c2c7dcda6606ea5cf08918fb7cd3f3359e9e84338dc690013f20cd42e930301"
    "${archive}/impact32.exe#6061ef3b7401d9642f5dfdb5f2b376aa14663f6275e60a51207ad4facf2fccfb"
    "${archive}/times32.exe#db56595ec6ef5d3de5c24994f001f03b2a13e37cee27bc25c58f6f43e8f807ab"
    "${archive}/trebuc32.exe#5a690d9bb8510be1b8b4fe49f1f2319651fe51bbe54775ddddd8ef0bd07fdac9"
    "${archive}/verdan32.exe#c1cb61255e363166794e47664e2f21af8e3a26cb6346eb8d2ae2fa85dd5aad96"
    "${archive}/webdin32.exe#64595b5abc1080fba8610c5c34fab5863408e806aafe84653ca8575bed17d75a"
    "https://corefonts.sourceforge.net/eula.htm#2ce80d12c33e740344ade3f62ec162fa902704d0cc141f4f83e28901a8650bab"
)
workdir="."

build() {
    if ! command -v 7z >/dev/null 2>&1; then
        echo "Error: Host system requires p7zip to build ${port}."
        exit 1
    fi

    # Extract .ttf files
    mkdir -p fonts
    for f in *.exe; do
        7z x "$f" -o"fonts" -y -r -ir!*.ttf -ir!*.TTF -bso0
    done

    # Convert file names to lowercase
    cd fonts
    for f in *; do
        lower=$(echo "$f" | tr '[:upper:]' '[:lower:]')
        if [[ "$f" != "$lower" ]]; then
            mv -f "$f" "$lower"
        fi
        echo "${lower} extracted"
    done
}

install() {
    install_dir="${SERENITY_INSTALL_ROOT}/usr/local/share/fonts/truetype/msttcorefonts"
    mkdir -p "${install_dir}"
    cp -v ./fonts/* "${install_dir}"

    doc_dir="${SERENITY_INSTALL_ROOT}/usr/local/share/doc/msttcorefonts"
    mkdir -p "${doc_dir}"
    cp -v "eula.htm" "${doc_dir}/eula.html"
    echo "Microsoft's EULA applies. See /usr/local/share/doc/msttcorefonts/eula.html for details."
}
