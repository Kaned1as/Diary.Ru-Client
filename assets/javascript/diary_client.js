document.domain = "diary.ru";

function handleIMGDown(index, source)
{
  var filename = source.substring(source.lastIndexOf('/')+1);
  
  document.images["imageLoader" + index].src=source;
  //document.write("clicked!");
  return true;
}