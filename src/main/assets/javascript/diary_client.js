document.domain = "diary.ru";

function handleIMGDown(input, source)
{
  var image = new Image();
  image.src=source;
  
  input.parentNode.insertBefore(image, input);
  input.parentNode.removeChild(input);
  //alert(image.parentNode.innerHTML);
  return true;
}

function handleADown(input, link, source)
{
  var image = new Image();
  image.src=source;
  
   var linkImage = document.createElement('a');
   linkImage.href = link;
   linkImage.appendChild(image);
  
  input.parentNode.insertBefore(linkImage, input);
  input.parentNode.removeChild(input);
  //alert(image.parentNode.innerHTML);
  return true;
}