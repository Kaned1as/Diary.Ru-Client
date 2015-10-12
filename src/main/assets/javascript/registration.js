window.addEventListener('load', delayedExecute, false);

function delayedExecute() {
    var token = $("input[name='adverigo_sid']")[0];
    var input = $("input[name='adverigo_captcha_answer']")[0];
    var send = $("input.submit")[0];
    if(!!token && !!input && !!send) {
        send.type = 'button'
        send.onclick = passToHandler;
    } else {
        window.setTimeout(delayedExecute, 100);
    }
}

function passToHandler() {
    var token = $("input[name='adverigo_sid']")[0];
    var input = $("input[name='adverigo_captcha_answer']")[0];
    RegisterHandler.grab(token.value, input.value);
}