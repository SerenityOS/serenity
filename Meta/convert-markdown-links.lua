function Link(el)
    el.target = string.gsub(el.target, "%.md", ".html") -- change .md to .html links
    return el
end
