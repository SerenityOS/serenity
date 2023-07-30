#!/bin/sh

echo "#include <AK/StringView.h>"
echo "namespace Web::CSS {"
echo "extern StringView $1;"
echo "StringView $1 = \"\\"
grep -v '^ *#' < "$2" | while IFS= read -r line; do
  echo "$line""\\" | sed 's/"/\\\"/g'
done
echo "\"sv;"
echo "}"
