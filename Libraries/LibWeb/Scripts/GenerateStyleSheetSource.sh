#!/bin/sh

echo "namespace Web::CSS {"
echo "extern const char $1[];"
echo "const char $1[] = \"\\"
grep -v '^ *#' < "$2" | while IFS= read -r line; do
  echo "$line""\\"
done
echo "\";"
echo "}"
