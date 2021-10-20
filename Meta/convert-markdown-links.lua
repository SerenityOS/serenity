function Link(el)
    el.target = string.gsub(el.target, "%.md", ".html") -- change .md to .html links
    el.target = string.gsub(el.target, "man", "", 1) -- change man1/???.html to 1/???.html links
    return el
end
