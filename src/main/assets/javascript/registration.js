window.addEventListener('load', delayedExecute, false);

function delayedExecute() {
    console.error("here");
    var token = $("input[name='adverigo_sid']")[0];
    var input = $("input[name='adverigo_captcha_answer']")[0];
    var send = $("input.submit")[0];
    if(!!token && !!input && !!send) {
        console.error("here1");
        send.type = 'button'
        send.onclick = passToHandler;
    } else {
        console.error("here2");
        console.error(token);
        console.error(input);
        console.error(send);
        window.setTimeout(delayedExecute, 100);
    }
}

function passToHandler() {
    var token = $("input[name='adverigo_sid']")[0];
    var input = $("input[name='adverigo_captcha_answer']")[0];
    console.error(RegisterHandler);
    RegisterHandler.grab(token.value, input.value);
}