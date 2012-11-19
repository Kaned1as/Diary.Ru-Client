var sender = null;
var sender_innerHTML = "";
var Busy = false;
document.domain = "diary.ru";

// returns true if an element is in an array
// array - the haystack
// element - the needle
// func - an optional function to compare the needle to each element in the haystack
function inArray(array, element, func)
{
	if (! func)	func = _argument;

	var l = array.length;
	for (var i = 0; i < l; i++) if (func(array[i]) == func(element)) return true;

	return false;
}

// the default comparison function for inArray
function _argument(r) {
	return r;
}

function pushHandler(object, event, handler)
{
	var handlersProp = '_handlerStack_'+event;
	var eventProp = 'on'+event;

	if (typeof(object[handlersProp]) == 'undefined' ) object[handlersProp] = Array(handler);
	else object[handlersProp][object[handlersProp].length] = handler;

	object[eventProp]=function(e)
	{
		for( var i=0; i<object[handlersProp].length; i++ ) eval( object[handlersProp][i](e) );
	}
//	var eventProp = 'on'+event;
//	object[eventProp] = function(e){ handler(e); }
}

function get(id)
{
	return document.getElementById(id);
}

function document_write($text)
{
	document.write($text);
	return false;
}
//=====================================================================================================================================================
function doGetCaretPosition (ctrl)
{
	var CaretPos = 0;
	// IE Support
	if (document.selection)
	{
		ctrl.focus ();
		var Sel = document.selection.createRange ();

		Sel.moveStart ('character', -ctrl.value.length);

		CaretPos = Sel.text.length;
	}
	// Firefox support
	else if (ctrl.selectionStart || ctrl.selectionStart == '0')
		CaretPos = ctrl.selectionStart;

	return (CaretPos);
}

function setCaretPosition(ctrl, pos)
{
	if(ctrl.setSelectionRange){
		ctrl.focus();
		ctrl.setSelectionRange(pos,pos);
	}
	else if (ctrl.createTextRange) {
		var range = ctrl.createTextRange();
		range.collapse(true);
		range.moveEnd('character', pos);
		range.moveStart('character', pos);
		range.select();
	}
}

function lTrim(sString){return leftTrim(sString);}
function leftTrim(sString)
{
	while (sString.substring(0,1) == ' '){
		sString = sString.substring(1, sString.length);
	}
	return sString;
}

function rTrim(sString){return rightTrim(sString);}
function rightTrim(sString)
{
	while (sString.substring(sString.length-1, sString.length) == ' '){
		sString = sString.substring(0,sString.length-1);
	}
	return sString;
}
//=====================================================================================================================================================
function swap3(id)
{
    var obj = get(id);
    var imgobj = get('img' + id);

    if(obj.style.display=='none'){
          obj.style.display='block';
          obj.style.visibility='visible';
		  if (imgobj!=null) imgobj.src = "http://static.diary.ru/images/-.gif";
    }else{
          obj.style.display='none';
          obj.style.visibility='hidden';
		  if (imgobj!=null) imgobj.src = "http://static.diary.ru/images/+.gif";
    }
	return false;
}
function swap2(id)
{
    var obj = get(id);
    var imgobj = get('img' + id);

    if(obj.style.display=='none'){
          obj.style.display='';
          obj.style.visibility='visible';
		  if (imgobj!=null) imgobj.src = "http://static.diary.ru/images/-.gif";
    }else{
          obj.style.display='none';
          obj.style.visibility='hidden';
		  if (imgobj!=null) imgobj.src = "http://static.diary.ru/images/+.gif";
    }
	return false;
}
function show(id){return ShowDiv(id);}
function ShowDiv(id)
{
	var obj = get(id);
	if(obj==null || obj.style.display!="none") return false;
	if(obj.className == 'hidden') obj.className = null;
	fadeOpacity(id, 'in');

	return false;
}
function Show2Div(id)
{
	var obj = get(id);
	if(obj==null) return false;
	obj.style.visibility='visible';

	return false;
}
function show3(id)
{
	var obj = get(id);
	if(obj==null || obj.style.display!="none") return false;
	if(obj.className == 'hidden') obj.className = null;
        obj.style.display = "";
        obj.style.visibility = "visible";

	return false;
}

function hide(id){return HideDiv(id);}
function HideDiv(id)
{
	var obj = get(id);
	if(obj==null || obj.style.display=="none") return false;

	fadeOpacity(id, 'out');

	return false;
}
function Hide2Div(id)
{
	var obj = get(id);
	if (obj!=null)	obj.style.visibility='hidden';
	return false;
}
function hide3(id)
{
	var obj = get(id);
	if(obj==null || obj.style.display=="none") return false;
        obj.style.display = "none";
        obj.style.visibility = "hidden";

	return false;
}
/*
function fadeout (obj)
{
	obj.style.display='';
	obj.style.visibility='visible';
	var j = 0;
	for (i=30; i<101; i++) setTimeout(setOpacityTimeOut(obj, i/10), j++ * 100);
}
function fadein (obj)
{
	var j = 0;
	for (i=1; i<101; i++) setTimeout(setOpacityTimeOut(obj, 1 - i/10), j++ * 100);
	setTimeout(function() {obj.style.display='none'; obj.style.visibility='hidden';}, j * i);
}
var setOpacityTimeOut = function(elem, nOpacity) { var fn = function() {setOpacity(elem, nOpacity);}; return fn; }
function setOpacity(elem, nOpacity)
{
//	alert(nOpacity);
  var opacityProp = getOpacityProperty();

  if (!elem || !opacityProp) return;

  if (opacityProp=="filter")
  {
    nOpacity *= 100;

    var oAlpha = elem.filters['DXImageTransform.Microsoft.alpha'] || elem.filters.alpha;
    if (oAlpha) oAlpha.opacity = nOpacity;
    else elem.style.filter += "progid:DXImageTransform.Microsoft.Alpha(opacity="+nOpacity+")"; // Для того чтобы не затереть другие фильтры используем "+="
  }
  else elem.style[opacityProp] = nOpacity;
}

function getOpacityProperty()
{
  if (typeof document.body.style.opacity == 'string') // CSS3 compliant (Moz 1.7+, Safari 1.2+, Opera 9)
    return 'opacity';
  else if (typeof document.body.style.MozOpacity == 'string') // Mozilla 1.6 и младше, Firefox 0.8
    return 'MozOpacity';
  else if (typeof document.body.style.KhtmlOpacity == 'string') // Konqueror 3.1, Safari 1.1
    return 'KhtmlOpacity';
  else if (document.body.filters && navigator.appVersion.match(/MSIE ([\d.]+);/)[1]>=5.5) // Internet Exploder 5.5+
    return 'filter';

  return false; //нет прозрачности
}
/**/
//=====================================================================================================================================================
function _do_ajax(obj, jsn)
{
	sign = "";
	try{sign = '&signature=' + signature;}catch(e){}
	_show_loading(obj);
	loadV(obj.href + (obj.href.match(/[?]/) ? "&":"?") + "js" + (jsn ? jsn:'') + sign);
	return false;
}
function _show_loading(obj, clear)
{
	sender = obj;
	sender_innerHTML = obj.innerHTML;
	if(obj.innerHTML=="" || clear) obj.innerHTML = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
	obj.innerHTML = '<span style="background: url(http://static.diary.ru/img/progress2_10x10.gif) no-repeat center center;"><span style="visibility:hidden">' + obj.innerHTML + '</span></span>';
	obj.blur();
}
function sender_restore(){ if (sender!=null && sender_innerHTML!=null) sender.innerHTML = sender_innerHTML; }
var postTitle;

function clUploadData ()
{
	this.type = "text/javascript";
	this.oldScript  = document.createElement("SCRIPT");
	this.oldScript.type = "text/javascript";
	this.callId = 0;

//	document.body.appendChild(this.oldScript);
	document.getElementsByTagName('body')[0].appendChild(this.oldScript);
	this.upload = function (theparams, nocash)
	{

		var newScript = document.createElement("SCRIPT");
		newScript.type = "text/javascript";
		newScript.src  = theparams || "";
//		newScript.onLoad = function (){alert('load');};

//if(get('postTitle')!=null) postTitle = get('postTitle');
//if(postTitle!=null) postTitle.value+=" load";

		if (nocash)
		{
			this.callId += 1 + Math.random();
			var re = new RegExp("[?]", 'gi');
			if (re.test(newScript.src)) newScript.src += "&";
			else newScript.src += "?";
			newScript.src  += "callid=" + String(this.callId);
		}
//		var temp = document.getElementById('temp');
//		if(temp!=null) document.all["temp"].value = 'http://pay.diary.ru/mod/pay/' + newScript.src;

//		document.body.replaceChild(newScript,this.oldScript);
		document.getElementsByTagName('body')[0].replaceChild(newScript,this.oldScript);
		this.oldScript = newScript;
	}
}

if(document.body == null) document.write("<body></body>");
var uploadObject = new clUploadData();

function loadV(theparams, nocash)
{
//	window.status = 'загрузка...';
	uploadObject.upload(theparams, nocash);
}

function loadV2(link)
{
//	window.status = 'загрузка...';
	var script = document.createElement('script');
	script.defer = false;
	script.src = link;
	document.getElementsByTagName('body')[0].appendChild(script);
//	document.body.appendChild(script);
}

function LoadData(theparams,window_status)
{
	window.status = window_status || 'загрузка...';
	Busy = true;
	uploadObject.upload(theparams);
}


function showMsg(type,title,text)
{
	var obj;
	obj = document.getElementById("msgWin");

	if(obj==null)
	{
//		if (document.getElementById("message")!=null)
//		{
			var ndiv = document.createElement('div');
			ndiv.innerHTML = '<div id="msgWin" style="FILTER:progid:DXImageTransform.Microsoft.shadow(direction=135,color=#666666,strength=3);"><table id="msgTitle"><tr><td id="msgTitleText">Ошибка</td><td style="width:1%;padding:3px;"><input type="button" onclick="hide(\'msgWin\');" class="std_submit" value="X" style="font-size:70%;width:15px;heigth:15px;margin:-1px;padding:0;"/></td></tr></table><em id="msgText"><table width=100% height=100% cellpadding=0 cellspacing=0><tr><td id="msgTextContent" valign=top></td></tr><tr><td valign="bottom" align="right"><input type="button" onclick="hide(\'msgWin\');" class="std_submit" value="Ok" id="msgWinCloseBt" style="width:70px; margin-top: 5px;"/></td></tr></table></em></div>';
			document.getElementsByTagName('body')[0].appendChild(ndiv);

//			document.getElementsByTagName('body')[0].innerHTML = '<div id="msgWin"><table id="msgTitle"><tr><td id="msgTitleText">+шибка<td style="width:1%;padding:3px;"><input type="button" id="msgWinCloseBt" onclick="hide(\'msgWin\');" class="submit" value="X" style="font-size:70%;width:15px;heigth:15px;margin:-1px;padding:0;"/></table></strong><em id="msgText"></em></div>' + document.getElementsByTagName('body')[0].innerHTML;
			obj = document.getElementById("msgWin");
//		}
	}

	if(obj!=null)
	{
		document.getElementById("msgTitleText").innerHTML = "&nbsp;" + title;
		document.getElementById("msgTextContent").innerHTML = text;

		if(type == 'alert') document.getElementById("msgText").className = "msgErr";
		else  document.getElementById("msgTextContent").className = "";

		if (navigator.userAgent.indexOf("MSIE 6") != -1 || navigator.userAgent.indexOf("MSIE 5") != -1) obj.style.top = document.documentElement.scrollTop + 200 + "px";
		obj.style.visibility = "visible";
		obj.style.display = "block";
		document.getElementById("msgWinCloseBt").focus();
	}
	else alert(text.replace( "<br>", "\r\n" ));
}
//=====================================================================================================================================================
//Устанавливает cookie
// name - имя cookie
// value - значение cookie
// [expires] - дата окончания действия cookie (по умолчанию - конец текущей  сессии)
// [path] - путь, где cookie верны (по умолчанию - путь к текущему документу)
// [domain] - домен, где cookie верны (по умолчанию - домен вызываемого документа)
// [secure] - бинарная переменная, показывающая, что cookie должны передаваться через защищённое соединение
// * по умолчанию аргументу присвоено значение null
// * нулевое значение не требуется присваивать пропущенным переменным
function setCookie(name, value, expires, path, domain, secure) {
	var curCookie = name + "=" + escape(value) +
	((expires) ? "; expires=" + expires.toGMTString() : "") +
	((path) ? "; path=" + path : "") +
	((domain) ? "; domain=" + domain : "") +
	((secure) ? "; secure" : "");
	document.cookie = curCookie;
}

// name - имя cookie
// * строка возврата содержит значения необходимого cookie или null при его отсутствии
function getCookie(name) {
	var dc = document.cookie;
	var prefix = name + "=";
	var begin = dc.indexOf("; " + prefix);
	if (begin == -1) {
		begin = dc.indexOf(prefix);
		if (begin != 0) return null;
	} else
		begin += 2;
	var end = document.cookie.indexOf(";", begin);
	if (end == -1)
		end = dc.length;
	return unescape(dc.substring(begin + prefix.length, end));
}

// name - имя cookie
// [path] - путь, где cookie верны (должен быть тем же, что и путь при создании cookie)
// [domain] - домен, где cookie верны (должен быть тем же, что и домен при создании cookie
// * путь и домен по умолчанию присвоены в null и не требуется присваивать этого значения
function deleteCookie(name, path, domain) {
  if (getCookie(name)) {
	document.cookie = name + "=" +
	((path) ? "; path=" + path : "") +
	((domain) ? "; domain=" + domain : "") +
	"; expires=Thu, 01-Jan-70 00:00:01 GMT";
  }
}

// date - экземпляр объекта Date
// * все экземпляры объекта Date передаются этой функции "для
// ремонта"
function fixDate(date) {
	var base = new Date(0);
	var skew = base.getTime();
	if (skew > 0)
		date.setTime(date.getTime() - skew);
}
//=====================================================================================================================================================
// Возвращает координаты объекта
function get_pos(obj){
	var left = 0;
	var top  = 0;
	while(obj){
		left += obj.offsetLeft;
		top += obj.offsetTop;
		if(obj.style.borderTopWidth != ''){
			top +=  Number(parseInt(obj.style.borderTopWidth));
		}
		if(obj.style.borderLeftWidth!=''){
			left += Number(parseInt(obj.style.borderLeftWidth));
		}
		obj=obj.offsetParent;
	}
	ret_obj = {
		x:left,
		y:top
	}
	return ret_obj;
}
//=====================================================================================================================================================
function SaveOptionStatus(name, value)
{
	var now = new Date();
	fixDate(now);
	now.setTime(now.getTime() + 365 * 24 * 60 * 60 * 1000);
	deleteCookie(name);
	setCookie(name, value, now, "/", "diary.ru");//Устанавливаем cookie
}
//=====================================================================================================================================================
function ord(chr)
{
    return chr.charCodeAt(0);
}
function chr(chr)
{
	return String.fromCharCode(chr);
}
//=====================================================================================================================================================
function swapMore(oid, alt)
{
	if (alt) return true;
	swap2('more'+oid);
	swap2('linkmore'+oid);

	return false;
}
function swapMore2(oid, shortname, draft, alt)
{
	var obj = get('more' + oid);
	if(obj.innerHTML == '')
	{
		_show_loading(get('linkmore'+oid));
		loadV((shortname ? shortname:"") + '?post=' + oid + (draft==null ? '':'&draft') + (alt ? '&oam':'') + '&js');
	}
	else swapMore(oid);

	return false;
}
//=====================================================================================================================================================
function getBodyScrollTop()
{
	return self.pageYOffset || (document.documentElement && document.documentElement.scrollTop) || (document.body && document.body.scrollTop);
}

function getBodyScrollLeft()
{
  return self.pageXOffset || (document.documentElement && document.documentElement.scrollLeft) || (document.body && document.body.scrollLeft);
}
//=====================================================================================================================================================
var open_win_list = null;
function openWinList(target, type)
{
	if (open_win_list != null)
	{
		try
		{
			if (open_win_list.document != null) open_win_list.focus();
			else open_win_list = window.open('/options/site/?lists&target=' + target + '&type=' + type, this.target, 'width=500, height=400, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=0');
		}
		catch(ex)
		{
			open_win_list = window.open('/options/site/?2&lists&target=' + target + '&type=' + type, this.target, 'width=500, height=400, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=0');
		}
	}
	else open_win_list = window.open('/options/site/?lists&target=' + target + '&type=' + type, this.target, 'width=500, height=400, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=0');
}
//=====================================================================================================================================================
/*
wwww.tigir.com - 06.07.2006

Source: http://www.tigir.com/js/opacity.js

Библиотека opacity.js к статье "CSS прозрачность (css opacity, javascript opacity)" - http://www.tigir.com/opacity.htm

setElementOpacity - установка прозрачности
getOpacityProperty - проверка, есть ли возможность менять прозрачность
fadeOpacity - плавное изменение прозрачности
*/

/* Функция кроссбраузерной установки прозрачности

Пример: setElementOpacity(document.body, 0.5); //сделать документ прозрачным на половину
*/
function setElementOpacity(oElem, nOpacity)
{
	var p = getOpacityProperty();
	(setElementOpacity = p=="filter"?new Function('oElem', 'nOpacity', 'nOpacity *= 100;	var oAlpha = oElem.filters["DXImageTransform.Microsoft.alpha"] || oElem.filters.alpha;	if (oAlpha) oAlpha.opacity = nOpacity; else oElem.style.filter += "progid:DXImageTransform.Microsoft.Alpha(opacity="+nOpacity+")";'):p?new Function('oElem', 'nOpacity', 'oElem.style.'+p+' = nOpacity;'):new Function)(oElem, nOpacity);
}

// Функция getOpacityProperty() возвращает свойство которое используется для смены прозрачности или undefined, и может использоваться для проверки возможности изменения прозрачности
function getOpacityProperty()
{
	var p;
	if (typeof document.body.style.opacity == 'string') p = 'opacity';
	else if (typeof document.body.style.MozOpacity == 'string') p =  'MozOpacity';
	else if (typeof document.body.style.KhtmlOpacity == 'string') p =  'KhtmlOpacity';
	else if (document.body.filters && navigator.appVersion.match(/MSIE ([\d.]+);/)[1]>=5.5) p =  'filter';

	return (getOpacityProperty = new Function("return '"+p+"';"))();
}

/* Функции для плавного изменения прозрачности:

1) fadeOpacity.addRule('opacityRule1', 1, 0.5, 30); //вначале создаем правило, задаем имя правила, начальную прозрачность и конечную, необязательный параметр задержки, влийяющий на скорость смены прозрачности
2) fadeOpacity('elemID', 'opacityRule1'); // выполнить плавную смену прозрачности элемента с id равным elemID, по правилу opacityRule1
3) fadeOpacity.back('elemID'); //вернуться в исходное сотояние прозрачности
*/
function fadeOpacity(sElemId, sRuleName, bBackward)
{
	var elem = document.getElementById(sElemId);
	elem.style.display = "";
	elem.style.visibility = "visible";

	if (!elem || !getOpacityProperty() || !fadeOpacity.aRules[sRuleName]) return;

	var rule = fadeOpacity.aRules[sRuleName];
	var nOpacity = rule.nStartOpacity;

	if (fadeOpacity.aProc[sElemId]) {clearInterval(fadeOpacity.aProc[sElemId].tId); nOpacity = fadeOpacity.aProc[sElemId].nOpacity;}


	if ((nOpacity==rule.nStartOpacity && bBackward) || (nOpacity==rule.nFinishOpacity && !bBackward)) return;

	fadeOpacity.aProc[sElemId] = {'nOpacity':nOpacity, 'tId':setInterval('fadeOpacity.run("'+sElemId+'")', fadeOpacity.aRules[sRuleName].nDalay), 'sRuleName':sRuleName, 'bBackward':Boolean(bBackward)};
}

fadeOpacity.addRule = function(sRuleName, nStartOpacity, nFinishOpacity, nDalay){fadeOpacity.aRules[sRuleName]={'nStartOpacity':nStartOpacity, 'nFinishOpacity':nFinishOpacity, 'nDalay':(nDalay || 30),'nDSign':(nFinishOpacity-nStartOpacity > 0?1:-1)};};
fadeOpacity.back = function(sElemId){fadeOpacity(sElemId,fadeOpacity.aProc[sElemId].sRuleName,true);};

fadeOpacity.run = function(sElemId)
{
	var proc = fadeOpacity.aProc[sElemId];
	var rule = fadeOpacity.aRules[proc.sRuleName];

	obj = document.getElementById(sElemId);
	proc.nOpacity = Math.round(( proc.nOpacity + .1*rule.nDSign*(proc.bBackward?-1:1) )*10)/10;
	setElementOpacity(obj, proc.nOpacity);

	if (proc.nOpacity <= 0.3 && proc.nOpacity==rule.nFinishOpacity) {obj.style.visibility = "hidden"; obj.style.display = "none";}

	if (proc.nOpacity==rule.nStartOpacity || proc.nOpacity==rule.nFinishOpacity) clearInterval(fadeOpacity.aProc[sElemId].tId);
}
fadeOpacity.aProc = {};
fadeOpacity.aRules = {};
fadeOpacity.addRule('out', 1, 0, 30);
fadeOpacity.addRule('in', 0, 1, 30);

//=========================================================================================================================================================================================================