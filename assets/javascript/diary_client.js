document.domain = "diary.ru";

function handleIMGDown(index, source)
{
  var filename = source.substring(source.lastIndexOf('/')+1);
  var source_input = document.getElementById("imageLoader" + index);
  var image = new Image();
  image.src=source;
  
  source_input.parentNode.insertBefore(image, source_input);
  source_input.parentNode.removeChild(source_input);
  //alert(image.parentNode.innerHTML);
  return true;
}