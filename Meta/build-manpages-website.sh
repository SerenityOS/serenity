#!/bin/bash
# shellcheck disable=SC1004 # literal backslash+linefeed is intended

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

export LC_ALL=C  # Make the directory order reproducible
export MAN_DIR=Base/usr/share/man/

if [[ -e output ]]; then
    echo "Directory 'output/' already exists. Delete it first."
    exit 1
fi

# Prepare output directories
for d in "${MAN_DIR}"*/; do
    dir_name=$(basename "$d")
    section="${dir_name/man}"
    mkdir -p "output/${dir_name}"
done

# Convert markdown to html

# If you're here because your local results are different from the website:
# Check that your pandoc version matches the pandoc-version specified in manpages.yaml.

for md_file in "${MAN_DIR}"*/*.md; do
    relative_path="$(realpath --relative-to="${MAN_DIR}" "${md_file}")"
    section="${relative_path%%/*}"
    section_number="${section#man}"
    filename="${relative_path#*/}"
    name="${filename%.md}"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --lua-filter=Meta/convert-markdown-links.lua \
        --metadata title="${name}(${section_number}) - SerenityOS man pages" \
        -o "output/${section}/${name}.html" \
        "${md_file}"
done

# Generate man page listings through pandoc
for d in output/*/; do
    section=$(basename "$d")
    section_number="${section#man}"
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --metadata title="Section ${section_number} - SerenityOS man pages" \
        -o "output/${section}/index.html" \
        <(
            for f in "$d"/*; do
                filename=$(basename "$f")
                name="${filename%.html}"
                if [[ "$filename" == "index.html" ]]; then
                    continue
                fi
                echo "- [${name}](${filename})"
            done
        )
done

# Generate man page listings through pandoc
for d in output/*/; do
    section=$(basename "$d")
    section_number="${section#man}"
    case "${section_number}" in
        1) section_title="User Programs";;
        2) section_title="System Calls";;
        3) section_title="Library Functions";;
        4) section_title="Special Files";;
        5) section_title="User Programs";;
        6) section_title="Games";; # TODO: Populate this section
        7) section_title="Miscellanea";;
        8) section_title="Sysadmin Tools";;
        *) section_title="SerenityOS man pages"; echo "WARNING: Unrecognized section ${section_number}?!";;
    esac
    pandoc -f gfm -t html5 -s \
        -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
        --metadata title="Section ${section_number} - ${section_title}" \
        -o "output/${section}/index.html" \
        <(
            for f in "$d"/*; do
                filename=$(basename "$f")
                name="${filename%.html}"
                if [[ "$filename" == "index.html" ]]; then
                    continue
                fi
                echo "- [${name}](${filename})"
            done
        )
done

# Generate main landing page listings through pandoc
pandoc -f gfm -t html5 -s \
    -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
    --metadata title="SerenityOS man pages" \
    -o output/index.html \
    Meta/Websites/man.serenityos.org/index.md
pandoc -f gfm -t html5 -s \
    -B Meta/Websites/man.serenityos.org/banner-preamble.inc \
    --metadata title="Can't run applications" \
    -o output/cant-run-application.html \
    Meta/Websites/man.serenityos.org/cant-run-application.md

# Copy pre-made files
cp Meta/Websites/man.serenityos.org/banner.png output/

# Copy icons
mkdir output/icons

while read -r p; do
  rsync -a --relative Base/res/icons/./"$p" output/icons/
done < icons.txt

rm icons.txt
