function t(){return Date.now()+700}async function i(e){const n=Number(e||0)-Date.now();n>0&&await new Promise(a=>window.setTimeout(a,n))}export{t as h,i as w};
