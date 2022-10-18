# Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
# SPDX-License-Identifier: BSD-2-Clause

domain=http://0.0.0.0:8000

RED="\e[31m"
GREEN="\e[32m"
YELLOW="\e[33m"
CYAN="\e[36m"
ENDCOLOR="\e[0m"

# testcase "title" "method" "path" "payload" "expected in output"
testcase() {
    echo "\nTest Case$YELLOW $1$ENDCOLOR";
    echo "Calling $2 $3"
    proOutput=$(pro -m $2 "$domain$3" -d $4 2>/dev/null)
    echo "Response: $proOutput"
    echo $proOutput | grep $5 >/dev/null;
    if [ $? -ne 0 ] {
        echo "Expected: $5"
        echo "$RED \rFAIL$ENDCOLOR"
    } else {
        echo "$GREEN \rPASS$ENDCOLOR"
    }
}

get_element_id() {
    proOutput=$(pro -m "POST" "$domain/session/0/element" -d "{\"using\": \"css selector\", \"value\": \"$1\"}" 2>/dev/null)
    # Hack of the century
    echo $proOutput > .foo
    x=$(js -i -c 'print(parseInt(loadJSON(".foo").value.value))')
    echo $x
}

start_section() {
    echo "$CYAN \n$1$ENDCOLOR"
}

setup() {
    killall WebDriver 2>/dev/null
    WebDriver >/dev/null &
    disown
    sleep 1

    start_section "Create Session"

    testcase "Setup Session" "POST" "/session" "" "sessionId"
    testcase "Destroy Session with wrong ID" "DELETE" "/session/100" "" "invalid session id"

    start_section "URL"

    testcase "Go to URL" "POST" "/session/0/url" '{"url": "file:///res/html/misc/webdriver-tests.html"}' "\"value\":null"
    sleep 1
}

cookies() {
    start_section "COOKIES"

    testcase "Add Cookie" "POST" "/session/0/cookie" '{"cookie":{"name": "cookieName", "value": "cookieValue"}}' "\"value\":null"
    testcase "Add Cookie without payload" "POST" "/session/0/cookie" '{}' "Payload doesn't have a cookie object"
    testcase "Add Cookie with wrong payload type" "POST" "/session/0/cookie" '{"cookie" : 42}' "Payload doesn't have a cookie object"
    testcase "Add Cookie without required keys (none)" "POST" "/session/0/cookie" '{"cookie" : {}}' "Cookie-Object doesn't contain all required keys"
    testcase "Add Cookie without required keys (name missing)" "POST" "/session/0/cookie" '{"cookie" : {"value": "aValue"}}' "Cookie-Object doesn't contain all required keys"
    testcase "Add Cookie without required keys (value missing)" "POST" "/session/0/cookie" '{"cookie" : {"name": "aName"}}' "Cookie-Object doesn't contain all required keys"
    testcase "Add Cookie with required keys Null (both)" "POST" "/session/0/cookie" '{"cookie" : {"name": null, "value": null}}' "Cookie-Object is malformed: name or value are null"
    testcase "Add Cookie with required keys Null (name)" "POST" "/session/0/cookie" '{"cookie" : {"name": null, "value": "aValue"}}' "Cookie-Object is malformed: name or value are null"
    testcase "Add Cookie with required keys Null (value)" "POST" "/session/0/cookie" '{"cookie" : {"name": "aName", "value": null}}' "Cookie-Object is malformed: name or value are null"
    testcase "Add Cookie with name not string" "POST" "/session/0/cookie" '{"cookie" : {"name": 42, "value": "aValue"}}' "Expect name attribute to be string"
    testcase "Add Cookie with value not string" "POST" "/session/0/cookie" '{"cookie" : {"name": "aName", "value": 42}}' "Expect value attribute to be string"
    testcase "Add Cookie with secure not bool" "POST" "/session/0/cookie" '{"cookie" : {"name": "aName", "value": "aValue", "secure": 42}}' "Cookie-Object is malformed: secure is not bool"
    testcase "Add Cookie with httpOnly not bool" "POST" "/session/0/cookie" '{"cookie" : {"name": "aName", "value": "aValue", "httpOnly": 42}}' "Cookie-Object is malformed: httpOnly is not bool"
    testcase "Add Cookie with secure and httpOnly" "POST" "/session/0/cookie" '{"cookie" : {"name": "cookieNameWithSecureAndHTTPOnly", "value": "aValue", "secure": true, "httpOnly": true}}' '"value":null'
    testcase "Add Cookie with expiry not u32" "POST" "/session/0/cookie" '{"cookie" : {"name": "aName", "value": "aValue", "expiry": 42.5}}' "Cookie-Object is malformed: expiry is not u32"
    testcase "Add Cookie With expiry date" "POST" "/session/0/cookie" '{"cookie":{"name": "cookieName2", "value": "cookieValue2", "expiry": 2000000000}}' "\"value\":null"
    testcase "Add Cookie With expiry date in the past" "POST" "/session/0/cookie" '{"cookie":{"name": "cookieName3", "value": "cookieValue2", "expiry": 0}}' "\"value\":null"
    testcase "Add Cookie with path not string" "POST" "/session/0/cookie" '{"cookie":{"name": "aName", "value": "aValue", "path": 42}}' "Expect path attribute to be string"
    testcase "Add Cookie with domain not string" "POST" "/session/0/cookie" '{"cookie":{"name": "aName", "value": "aValue", "domain": 42}}' "Expect domain attribute to be string"
    testcase "Add Cookie with path and domain" "POST" "/session/0/cookie" '{"cookie":{"name": "aName", "value": "aValue", "path": "/session", "domain": "0.0.0.0"}}' '"value":null'
    testcase "Get all cookies" "GET" "/session/0/cookie" "" '"cookieName"'
    testcase "Get one cookie" "GET" "/session/0/cookie/cookieName" "" '"cookieName"'
    testcase "Get cookie that doesn't exist" "GET" "/session/0/cookie/doesNotExist" "" "Cookie not found"
    testcase "Shouldn't get expired cookie" "GET" "/session/0/cookie/cookieName3" "" "Cookie not found"
}

find_element() {
    start_section "FIND ELEMENT"

    testcase "No property called using" "POST" "/session/0/element" '{"notUsing": "css selector"}' "No property called 'using' present"
    testcase "using is not a string" "POST" "/session/0/element" '{"using": 42}' "Property 'using' is not a String"
    testcase "no valid location strategy" "POST" "/session/0/element" '{"using": "notValid"}' "No valid location strategy"

    testcase "No property called value" "POST" "/session/0/element" '{"using": "css selector", "notValue": "no value"}' "No property called 'value' present"
    testcase "value is not a string" "POST" "/session/0/element" '{"using": "css selector", "value": 42}' "Property 'value' is not a String"

    testcase "Get Element H1" "POST" "/session/0/element" '{"using": "css selector", "value": "h1"}' '"name":"element-6066-11e4-a52e-4f735466cecf"'
    testcase "Get nonexistent element" "POST" "/session/0/element" '{"using": "css selector", "value": "h1#not-a-selector"}' "no such element"
}

find_elements() {
    start_section "FIND ELEMENTS"

    testcase "No property called using" "POST" "/session/0/elements" '{"notUsing": "css selector"}' "No property called 'using' present"
    testcase "using is not a string" "POST" "/session/0/elements" '{"using": 42}' "Property 'using' is not a String"
    testcase "no valid location strategy" "POST" "/session/0/elements" '{"using": "notValid"}' "No valid location strategy"

    testcase "No property called value" "POST" "/session/0/elements" '{"using": "css selector", "notValue": "no value"}' "No property called 'value' present"
    testcase "value is not a string" "POST" "/session/0/elements" '{"using": "css selector", "value": 42}' "Property 'value' is not a String"

    testcase "Get elements p" "POST" "/session/0/elements" '{"using": "css selector", "value": "p"}' '"name":"element-6066-11e4-a52e-4f735466cecf"'
}

find_element_from_element() {
    start_section "FIND ELEMENT FROM ELEMENT"

    elementId=$(get_element_id "#foo")
    testcase "No property called using" "POST" "/session/0/element/$elementId/element" '{"notUsing": "css selector"}' "No property called 'using' present"
    testcase "using is not a string" "POST" "/session/0/element/$elementId/element" '{"using": 42}' "Property 'using' is not a String"
    testcase "no valid location strategy" "POST" "/session/0/element/$elementId/element" '{"using": "notValid"}' "No valid location strategy"

    testcase "No property called value" "POST" "/session/0/element/$elementId/element" '{"using": "css selector", "notValue": "no value"}' "No property called 'value' present"
    testcase "value is not a string" "POST" "/session/0/element/$elementId/element" '{"using": "css selector", "value": 42}' "Property 'value' is not a String"

    testcase "element_id is not i32" "POST" "/session/0/element/Buggie/element" '{"using": "css selector", "value": "h1"}' "Element ID is not an i32"

    testcase "Get h2 child" "POST" "/session/0/element/$elementId/element" '{"using": "css selector", "value": "h2"}' '"name":"element-6066-11e4-a52e-4f735466cecf"'
}

find_elements_from_element() {
    start_section "FIND ELEMENTS FROM ELEMENT"

    elementId=$(get_element_id "#foo")
    testcase "No property called using" "POST" "/session/0/element/$elementId/elements" '{"notUsing": "css selector"}' "No property called 'using' present"
    testcase "using is not a string" "POST" "/session/0/element/$elementId/elements" '{"using": 42}' "Property 'using' is not a String"
    testcase "no valid location strategy" "POST" "/session/0/element/$elementId/elements" '{"using": "notValid"}' "No valid location strategy"

    testcase "No property called value" "POST" "/session/0/element/$elementId/elements" '{"using": "css selector", "notValue": "no value"}' "No property called 'value' present"
    testcase "value is not a string" "POST" "/session/0/element/$elementId/elements" '{"using": "css selector", "value": 42}' "Property 'value' is not a String"

    testcase "element_id is not i32" "POST" "/session/0/element/Buggie/elements" '{"using": "css selector", "value": "h1"}' "Element ID is not an i32"

    testcase "Get a children" "POST" "/session/0/element/$elementId/elements" '{"using": "css selector", "value": "a"}' '"name":"element-6066-11e4-a52e-4f735466cecf"'
}

get_element_attribute() {
    start_section "GET ELEMENT ATTRIBUTE"

    elementId=$(get_element_id "#find_me")
    testcase "element_id is not i32" "GET" "/session/0/element/Buggie/attribute/href" "" "Element ID is not an i32"
    testcase "Get href of a" "GET" "/session/0/element/$elementId/attribute/href" "" "#thisisalink"

    elementId=$(get_element_id "#dis")
    testcase "Get disabled of input (its there)" "GET" "/session/0/element/$elementId/attribute/disabled" "" '"value":"true"'
    testcase "Get checked of input (its not)" "GET" "/session/0/element/$elementId/attribute/checked" "" '"value":null'
}

get_element_property() {
    start_section "GET ELEMENT PROPERTY"

    elementId=$(get_element_id "#find_me")
    testcase "element_id is not i32" "GET" "/session/0/element/Buggie/property/href" "" "Element ID is not an i32"
    testcase "Get href of a" "GET" "/session/0/element/$elementId/property/href" "" '{"value":"file:///res/html/misc/webdriver-tests.html#thisisalink"}'

    elementId=$(get_element_id "#dis")
    testcase "Get nodeType of input" "GET" "/session/0/element/$elementId/property/nodeType" "" '{"value":"1"}'
}

get_element_css_value() {
    start_section "GET ELEMENT CSS VALUE"

    elementId=$(get_element_id "#withinlinestyle")
    testcase "element_id is not i32" "GET" "/session/0/element/Buggie/css/background-color" "" "Element ID is not an i32"
    testcase "Get background color from inline style" "GET" "/session/0/element/$elementId/css/background-color" "" '{"value":"rgb(255, 0, 0)"}'

    elementId=$(get_element_id "#withstylefromheader")
    testcase "Get color from stylesheet applied style" "GET" "/session/0/element/$elementId/css/color" "" '{"value":"rgb(0, 0, 255)"}'
}

get_element_tag_name() {
    start_section "GET ELEMENT TAG NAME"

    elementId=$(get_element_id "#a_ul")
    testcase "element_id is not i32" "GET" "/session/0/element/Buggie/name" "" "Element ID is not an i32"
    testcase "Get tag name of ul" "GET" "/session/0/element/$elementId/name" "" '{"value":"UL"}'

    elementId=$(get_element_id ".a_li")
    testcase "Get tag name of li" "GET" "/session/0/element/$elementId/name" "" '{"value":"LI"}'
}

cleanup() {
    testcase "Destroy Session" "DELETE" "/session/0" "" "\"value\":null"
}

setup
cookies
find_element
find_elements
find_element_from_element
find_elements_from_element
get_element_attribute
get_element_property
get_element_property
get_element_css_value
get_element_tag_name
cleanup
