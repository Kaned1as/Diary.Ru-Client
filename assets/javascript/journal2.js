function checkResult()
{
	if (!get('msg_form')) return;
	
	try
	{
		clearTimeout(checkResultTimer);
	}
	catch(e){}
	
	try
	{
		ShowSaveBt();
		if (get('save_action')!=null && get('save_action').value=="draft_autosave")
		{
			try
			{
				result = NewContentFrame.document.getElementById("result");
				if(result == null)
				{
					ShowDiv('autosave_err');
					if (get('autosave_err_img')!=null) get('autosave_err_img').title='����������� ������';
				}
			}
			catch(err)
			{
				ShowDiv('autosave_err');
				if (get('autosave_err_img')!=null) get('autosave_err_img').title='����������� ������';
			}
			return;
		}
	}
	catch(err)
	{
//		showMsg("alert","������","<b>��������:</b> " + err.description);
	}
	
	try
	{
		result = NewContentFrame.document.getElementById("result");
//		alert(result);
		if(result == null) showMsg("alert","������ #42","����������� ������,<br> ���������� ����� 20 ������");
		else if(result.value==1)
		{
			obj=document.getElementById('message');
			if(obj!=null) obj.value = "";
		}
	}
	catch(err)
	{
		showMsg("alert","������ #51","����������� ������,<br> ���������� ����� 20 ������");
//		showMsg("alert","������","<b>��������:</b> " + err.description);
	}
}
//=====================================================================================================================================================
