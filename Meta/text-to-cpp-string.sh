#!/usr/bin/env bash
# $1 name of the variable
# $2 input path

echo "#include <AK/StringView.h>"
echo
echo "extern StringView $1;"
printf "StringView %s = R\"~~~(" "$1"
grep -v '^ *#' < "$2" | while IFS= read -r line; do
  echo "$line"
done
echo ")~~~\"sv;"
