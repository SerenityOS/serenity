test("regular comments", () => {
    const source = `var i = 0;

// i++;
/* i++; */
/*
i++;
*/
return i;`;

    expect(source).toEvalTo(0);
});

test("html comments", () => {
    const source = `var i = 0;
<!-- i++; --> i++;
<!-- i++;
i++;
--> i++;
return i;`;

    expect(source).toEvalTo(1);
});
