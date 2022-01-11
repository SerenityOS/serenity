function Link(el)
    el.target = string.gsub(el.target, "file:///bin/.*", "../cant-run-application.html")
    el.target = string.gsub(el.target, "help://man/([^/]*)/(.*)", "../man%1/%2.html")
    return el
end
