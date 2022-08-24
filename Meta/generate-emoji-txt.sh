#!/usr/bin/env bash

set -e

if [ $# -ne 3 ]; then
  echo "Usage: $0 <input emoji-test.txt file> <emoji image directory> <output path>"
  exit 1
fi

INPUT_FILE="$1"
EMOJI_DIR="$2"
OUTPUT_PATH="$3"

# empty the generated file first
:>| "$OUTPUT_PATH"

first_heading=true
printed_group_header=false
printed_subgroup_header=false
while IFS= read -r line
do
    if [[ $line == \#\ group:\ * ]]; then
        current_group="$line"
        printed_group_header=false
    elif [[ $line == \#\ subgroup:\ * ]]; then
        current_subgroup="$line"
        printed_subgroup_header=false
    elif [[ ${#line} -ne 0 && $line != \#* ]]; then
        codepoints_string=${line%%;*}
        IFS=" " read -r -a codepoints <<< "$codepoints_string"
        for i in "${!codepoints[@]}"; do
            # strip leading zeros
            codepoints[$i]="${codepoints[$i]#"${codepoints[$i]%%[!0]*}"}"
            # add U+ prefix
            codepoints[$i]="U+${codepoints[$i]}"
        done

        # when doing a lookup we want to remove all U+FE0F (emoji presentation specifier) codepoints
        lookup_filename_parts=()
        for codepoint in "${codepoints[@]}"; do
            if [[ $codepoint != "U+FE0F" ]]; then
                lookup_filename_parts+=("$codepoint")
            fi
        done

        IFS=_
        lookup_filename="${lookup_filename_parts[*]}.png"

        if [ -f "$EMOJI_DIR/$lookup_filename" ]; then
            if [ $printed_group_header = false ]; then
                if [ $first_heading = false ]; then
                    echo "" >> "$OUTPUT_PATH"
                fi
                echo "$current_group" >> "$OUTPUT_PATH"
                first_heading=false
                printed_group_header=true
            fi
            if [ $printed_subgroup_header = false ]; then
                echo "" >> "$OUTPUT_PATH"
                echo "$current_subgroup" >> "$OUTPUT_PATH"
                printed_subgroup_header=true
            fi

            emoji_and_name=${line#*# }
            emoji=${emoji_and_name%% E*}
            name_with_version=${emoji_and_name#* }
            name=${name_with_version#* }
            qualification=${line#*; }
            qualification=${qualification%%#*}
            # remove trailing whitespace characters
            qualification="${qualification%"${qualification##*[![:space:]]}"}"

            IFS=" "
            echo "$emoji - ${codepoints[*]} ${name^^} ($qualification)" >> "$OUTPUT_PATH"
        fi
    fi
done < "$INPUT_FILE"
