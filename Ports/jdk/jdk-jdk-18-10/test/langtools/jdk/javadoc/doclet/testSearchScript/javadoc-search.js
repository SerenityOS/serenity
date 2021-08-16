/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

var moduleSearchIndex;
var packageSearchIndex;
var typeSearchIndex;
var memberSearchIndex;
var tagSearchIndex;

var clargs = arguments;
var search;

function loadIndexFiles(docsPath) {
    tryLoad(docsPath, "module-search-index.js");
    tryLoad(docsPath, "package-search-index.js");
    tryLoad(docsPath, "type-search-index.js");
    tryLoad(docsPath, "member-search-index.js");
    tryLoad(docsPath, "tag-search-index.js");
    load(docsPath + "/search.js");
}

function tryLoad(docsPath, file) {
    try {
        load(docsPath + "/" + file);
    } catch (e) {
        print(e);
    }
}

var updateSearchResults = function() {};

function indexFilesLoaded() {
    return moduleSearchIndex
        && packageSearchIndex
        && typeSearchIndex
        && memberSearchIndex
        && tagSearchIndex;
}

var $ = function(f) {
    if (typeof f === "function") {
        f();
    } else {
        return {
            val: function() {
                return this;
            },
            prop: function() {
                return this;
            },
            addClass: function() {
                return this;
            },
            removeClass: function() {
                return this;
            },
            on: function() {
                return this;
            },
            focus: function() {
                return this;
            },
            blur: function() {
                return this;
            },
            click: function() {
                return this;
            },
            catcomplete: function(o) {
                o.close = function() {};
                search = function(term) {
                    var resultList = null;
                    o.source({
                            term: term
                        },
                        function(result) {
                            resultList = renderMenu(result);
                        }
                    );
                    return resultList;
                };
                for (var i = 0; i < clargs.length; i++) {
                    search(clargs[i]);
                }
            },
            "0": {
                setSelectionRange: function() {
                    return this;
                }
            }
        }
    }
};

$.each = function(arr, f) {
    for (var i = 0; i < arr.length; i++) {
        f(i, arr[i]);
    }
};

$.widget = function(a, b, c) {
};

$.ui = {
    autocomplete: {
        escapeRegex: function(re) {
            return re.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&');
        }
    }
};

var console = {
    log: function() {
        print.apply(this, arguments);
    }
};

var renderMenu = function(items) {
    var result = new java.util.ArrayList();
    var currentCategory = "";
    $.each(items, function(index, item) {
        var li;
        if (item.l !== noResult.l && item.category !== currentCategory) {
            // print(item.category);
            currentCategory = item.category;
        }
        result.add(renderItem(item));
    });
    return result;
};

var renderItem = function(item) {
    var label;
    if (item.category === catModules) {
        label = item.l;
    } else if (item.category === catPackages) {
        label = (item.m)
                ? item.m + "/" + item.l
                : item.l;
    } else if (item.category === catTypes) {
        label = (item.p)
                ? item.p + "." + item.l
                : item.l;
    } else if (item.category === catMembers) {
        label = item.p + "." + item.c + "." + item.l;
    } else if (item.category === catSearchTags) {
        label = item.l;
    } else {
        label = item.l;
    }
    return label;
};


