var sel = {
	creatselect : function( id_obj, objw, w_correct ){
		if(w_correct==null) w_correct = 0;
		var obj = get(id_obj);

		//var obj_h = obj.clientHeight || obj.style.height;
		//var obj_w = obj.offsetWidth  || obj.style.width;

		//if(isMSIE5) obj_w = Number(obj_w.replace( /px/i, '' ));
		//if(isMSIE5) obj_h = Number(obj_h.replace( /px/i, '' ));
		var obj_w = objw;

		opt = '';
		for( var i=0; i<obj.options.length; i++ ){
			if (obj.options[i].selected){
				name_sel = obj.options[i].text;
				val_sel  = obj.options[i].value;
			}
		}

		content  = '<div title="'+obj.getAttribute('title')+'">';
		content += '<input onClick="sel.showsel(event)" onmouseover="sel.block=true;" onmouseout="sel.block=false;" class="'+obj.className+'" id="'+obj.getAttribute('id')+'newsel" style="width:'+(obj_w-14)+'px; cursor:default" class="text" type="text" readonly="readonly" value="'+name_sel+'" />';
		content += '<img onmouseover="sel.block=true;" onmouseout="sel.block=false;" align="top" id="'+obj.getAttribute('id')+'btid" onClick="sel.showsel(event)" alt="v" title="" src="http://static.diary.ru/images/space.gif" />';
		content += '</div>';
		content += '<input type="hidden" name="'+obj.getAttribute('name')+'" id="select_val'+obj.getAttribute('id')+'" value="'+val_sel+'" />';
		content += '<div class="search_select_list" style="display:none;" id="'+obj.getAttribute('id')+'sform"></div>';

		pushHandler(document, 'mouseup', hide_sel );


		// Ñîçäàåì îïòèîíû ñåëåêòà
		var opt = '';
		for( var i=0; i<obj.options.length; i++ ){
			opt += '<div id="mscs_'+i+'" onMouseOver="sel.act(this)" class="unselect_line" onClick="sel.selval(this)">'+obj.options[i].text+'</div>';
			opt += '<div id="mscs_'+i+'_hv" style="display:none">'+obj.options[i].value+'</div>';
		}

		// Áëîê ïåðåìåííûõ
		this.old_obj   = obj;
		this.objid     = 'select_val'+obj.getAttribute('id');
		this.idform    = obj.getAttribute('id')+'sform';
		this.row_sel   = 'mscs_0';
		this.sel_oppen = false;
		this.inputid   = obj.getAttribute('id')+'newsel';
		this.butid     = obj.getAttribute('id')+'btid';
		this.block     = false;
		this.bl2       = false;


		obj.setAttribute("id",this.objid + "_bak");
		obj.setAttribute("name","");
		obj.style.display = 'none';

		document.write(content);
		get(this.idform).style.width = (obj_w+w_correct)+'px';
		get(this.idform).innerHTML = opt;
	},
	act : function(obj){
		if(!this.sel_oppen) return;
		obj.focus();
		get(this.row_sel).className = 'unselect_line';

		this.row_sel = obj.getAttribute('id');
		obj.className = 'select_line';
	},
	showsel : function(e){

		if( this.sel_oppen ){this.block = false; this.hidesel(); this.block = true; return; }

		if (!e) e = window.event || null;
		var num_button = e.keyCode?e.keyCode:e.charCode;
		if(num_button==40 ||  num_button==38 || num_button==13) return;
		get(this.idform).style.display = '';
		this.sel_oppen = true;
		sel.act(get('mscs_0'));
	},
	selval : function(obj){
		get(this.inputid).value = obj.innerHTML;
		get(this.objid).value = get(obj.getAttribute('id')+'_hv').innerHTML;

		this.hidesel();
	},
	hidesel : function(){
		if( this.block ){ return ;}
		this.sel_oppen = false;
		row_sel = 'mscs_0';
		get(this.idform).style.display = 'none';
	}
}
function hide_sel(){
	sel.hidesel();
}