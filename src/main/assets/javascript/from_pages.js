var bak = Object();
var bak_names = Object();

function showedit(id)
{
	obj = get('tag' + id + 'c');
	if (!obj) return;
	name = get('tag' + id).innerHTML;
	bak[id] = obj.innerHTML;
	obj.innerHTML = '<input id=tag' + id + 'i maxlength=100 class=text style=font-size:90%;height:1.2em; onkeypress="if(event.keyCode==13)return saveedit(' + id + ');" />' +
					'<a href=# class=other title=Подтвердить id=tag' + id + 'ok ><img src=http://static.diary.ru/images/ok.gif class="text op05" border=0 align=absmiddle hspace=2 onclick=saveedit(' + id +') ></a>' +
					'<a href=# class=other title=Отменить ><img src=http://static.diary.ru/images/nok.gif class="text op05"   border=0 align=absmiddle onclick=calceledit(' + id +') /></a>';
	get('tag' + id + 'i').value = bak_names[id] = name.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
	return false;
}
function calceledit(id)
{
	obj = get('tag' + id + 'c');
	if (!obj) return;
	obj.innerHTML = bak[id];
}
function saveedit(id)
{
	obj = get('tag' + id + 'i');
	if (!obj) return;
	
	if(get('tag' + id + 'i').value == bak_names[id]) return calceledit(id);
	
	sign = '';
	try{sign = '&signature=' + signature;}catch(e){}
	_show_loading(get('tag' + id + 'ok'));

	loadV2('?tags[' + id + ']=' + escape(obj.value).replace(/[+]/g,'%2B') + '&tag_save&js' + sign);
	return false;
}