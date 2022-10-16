#!/bin/Shell

export ARGSPARSER_EMIT_MARKDOWN=1

# Qemu likes to start us in the middle of a line, so:
echo

ERROR_FILE="generate_manpages_error.log"
rm -f "$ERROR_FILE"

exit_for_error()
{
    if test $DO_SHUTDOWN_AFTER_GENERATE {
        touch "$ERROR_FILE" # Ensure it exists, in case there wasn't any stderr output.
        shutdown -n
    } else {
        exit 1
    }
}

rm -rf generated_manpages 2> "$ERROR_FILE" || exit_for_error

for i in ( \
            (UserspaceEmulator 1) \
            (fortune 1) \
            (gunzip 1) \
            (gzip 1) \
            (ifconfig 1) \
            (lsof 1) \
            (nc 1) \
            (netstat 1) \
            (ntpquery 1) \
            (profile 1) \
            (readelf 1) \
            (shot 1) \
            (sql 1) \
            (strace 1) \
            (traceroute 1) \
            (tree 1) \
            (truncate 1) \
        ) {
    filename="generated_manpages/man$i[1]/$i[0].md"
    mkdir -p "generated_manpages/man$i[1]"
    echo "Generating for $i[0] in $filename ..."
    $i[0] --help > "$filename" 2> "$ERROR_FILE" || exit_for_error
    echo -e "\n<!-- Auto-generated through ArgsParser -->"  >> "$filename" 2> "$ERROR_FILE" || exit_for_error
}

rm -f "$ERROR_FILE"
echo "Successful."

if test $DO_SHUTDOWN_AFTER_GENERATE {
    shutdown -n
}
