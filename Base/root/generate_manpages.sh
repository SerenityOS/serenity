#!/bin/Shell

export ARGSPARSER_EMIT_MARKDOWN=1

# Qemu likes to start us in the middle of a line, so:
echo

rm -rf generated_manpages || exit 1

for i in ( \
            (Eyes 1) \
            (UserspaceEmulator 1) \
            (TelnetServer 1) \
            (WebServer 1) \
            (config 1) \
            (fortune 1) \
            (grep 1) \
            (gunzip 1) \
            (gzip 1) \
            (ifconfig 1) \
            (lsof 1) \
            (nc 1) \
            (netstat 1) \
            (nl 1) \
            (ntpquery 1) \
            (passwd 1) \
            (profile 1) \
            (readelf 1) \
            (shot 1) \
            (sql 1) \
            (strace 1) \
            (tail 1) \
            (tr 1) \
            (traceroute 1) \
            (tree 1) \
            (truncate 1) \
            (usermod 8) \
            (utmpupdate 1) \
            (wc 1) \
        ) {
    filename="generated_manpages/man$i[1]/$i[0].md"
    mkdir -p "generated_manpages/man$i[1]"
    echo "Generating for $i[0] in $filename ..."
    $i[0] --help > "$filename" || exit 1
    echo -e "\n<!-- Auto-generated through ArgsParser -->"  >> "$filename" || exit 1
}

echo "Successful."

if test $DO_SHUTDOWN_AFTER_GENERATE {
    shutdown -n
}
