let r = BigInt(1);

for(let a=BigInt(2),b=2; b<500000; a++,b++)
    r *= a;

console.log(r);
