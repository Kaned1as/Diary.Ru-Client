var dom = document.getElementById?1:0;
var ie4 = document.all && document.all.item;
var opera = window.opera; //Opera
var ie5 = dom && ie4 && !opera; 
var nn4 = document.layers; 
var nn6 = dom && !ie5 && !opera;
var vers=parseInt(navigator.appVersion);
var ie = document.all && !self.opera;
var ie9 = ie && -[1,];
//=====================================================================================================================================================
function doPoll(id)
{
	var formpoll = get('formpoll' + id);
	formpoll.setAttribute("target", "NewContentFrame");
	formpoll.setAttribute("action", '/poll.php?js2');

	for (var i=0; i < formpoll.attributes.length; i++) if (formpoll.attributes[i].nodeName == 'action') {
		formpoll.attributes[i].nodeValue = '/poll.php?js2';
		break;
	}
	
	return true;
}
function swapPoll(obj)
{
	var pollid = obj.getAttribute('id');
	if(obj.innerHTML == "Результаты голосования")
	{
		eval("div_" + pollid +" = get('div" + pollid +"').innerHTML;");
		_do_ajax(obj);
	}
	else
	{
		obj.innerHTML = "Результаты голосования";
		eval("get('div" + pollid +"').innerHTML = div_" + pollid +";");
	}
		
	return false;
}
//=====================================================================================================================================================
// Проверяем знает ли браузер про HTMLElement.
if (typeof(HTMLElement) != "undefined" && !opera) {
    var _emptyTags = {
       "IMG": true,
       "BR": true,
       "INPUT": true,
       "META": true,
       "LINK": true,
       "PARAM": true,
       "HR": true
    };
    
	if (ie9)
	{
	}
	else
	{
    HTMLElement.prototype.__defineGetter__("outerHTML", function () {
       var attrs = this.attributes;
       var str = "<" + this.tagName;
       for (var i = 0; i < attrs.length; i++) str += " " + attrs[ i ].name + "=\"" + attrs[ i ].value + "\"";
    
       if (_emptyTags[this.tagName]) return str + ">";
    
       return str + ">" + this.innerHTML + "</" + this.tagName + ">";
    });
    
    HTMLElement.prototype.__defineSetter__("outerHTML", function (sHTML){
       var r = this.ownerDocument.createRange();
       r.setStartBefore(this);
       var df = r.createContextualFragment(sHTML);
       this.parentNode.replaceChild(df, this);
    });
	}
}

var open_win_img = null;
function openWinImg( vkl )
{
	if( open_win_img != null )
	{
		try
		{
			if( open_win_img.document !=null) open_win_img.focus();
			else if( vkl ==1 ) open_win_img = window.open('/diary.php?module=photolib&act=plist&va=1', this.target, 'width=800, height=650, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=1');
			else               open_win_img = window.open('/diary.php?module=photolib&act=plist&va=2', this.target, 'width=800, height=650, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=1');
		}
		catch(ex)
		{
			if( vkl ==1 ) open_win_img = window.open('/diary.php?module=photolib&act=plist&va=1', this.target, 'width=800, height=650, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=1');
			else          open_win_img = window.open('/diary.php?module=photolib&act=plist&va=2', this.target, 'width=800, height=650, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=1');
		}
	}
	else
	{
		if( vkl ==1 ) open_win_img = window.open('/diary.php?module=photolib&act=plist&va=1', this.target, 'width=800, height=650, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=1');
		else          open_win_img = window.open('/diary.php?module=photolib&act=plist&va=2', this.target, 'width=800, height=650, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=1');
	}
}

var open_win_smile = null;

function openWinSmile()
{
	if (open_win_smile != null)
	{
		try
		{
			if (open_win_smile.document != null) open_win_smile.focus();
			else open_win_smile = window.open('/smile.php', this.target, 'width=500, height=400, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=0');
		}
		catch(ex)
		{
			open_win_smile = window.open('/smile.php', this.target, 'width=500, height=400, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=0');
		}
	}
	else
	{
		open_win_smile = window.open('/smile.php', this.target, 'width=500, height=400, location=0, toolbar=0, menubar=0, status=0, scrollbars=1, resizable=0');
	}
}

// определение ширины области записи для sImg --------------------------------------------------------
var postW = 100;

function setPostW(obj)
{
	if (postW == 100) postW = obj.width;
	obj.style.display = 'none';
	obj.style.visibility = 'hidden';
}

function setSImg(img)
{
	if (img.width + 120 > postW)
		img.parentNode.className="img_div";
}

function ChangeRecordCoutn(value)
{
	var obj = document.getElementById('record_num');
	
	if (value) obj.innerHTML = Number(obj.innerHTML) + 1;
	else obj.innerHTML = Number(obj.innerHTML) - 1;
}
//----------------------------------------------------------------------------------------------------
// смена стиля полей формы ---------------------------------------------------------------------------
function form_style_changer(obj)
{
	target = get('addCommentArea');
	if (target==null) target = get('postsArea');
	target_side = get('side');
	
	if (obj.checked) 
	{
		target.className = 'bordered form_alt_style'; 
		target_side.className = 'form_alt_style'; 
	}
	else 
	{
		target.className = 'bordered';
		target_side.className = ''; 
	};
}
//----------------------------------------------------------------------------------------------------
function pp(name, e)
{
	cc(msg = document.getElementById('message'));
	if (name!="") msg.value += "[L]" + name + "[/L]" + (e!=null && e.ctrlKey ? "":",") + " ";
	return false;
}
function cc(obj, alrt)
{
	if (obj.value == "Владелец дневника видит IP-адреса пользователей, оставивших комментарии!")
	{
//		if (!alrt) alert("Владелец дневника видит IP-адреса пользователей, оставивших комментарии!");
		obj.value = "";
	}
}
