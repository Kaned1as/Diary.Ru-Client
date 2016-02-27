Diary.Ru-Client
=====

Андроид-клиент для сайта [@Дневники](www.diary.ru)

Для работы использует парсинг самих страниц сайта, не использует API (был разработан до его появления)

##Возможности:

###Авторизация
* Вход/выход из аккаунта
* Регистрация
* Сохранение списка логинов

###Просмотр
* Просмотр списка избранных дневников / списка постоянных читателей
* Просмотр ленты избранного
* Просмотр/редактирование цитатника
* Просмотр любых дневников/сообществ (ручной ввод/личный дневник/из избранного)
* Просмотр профилей пользователей
* Просмотр/редактирование/отправка постов и комментариев
* Участие в опросах

###U-Mail
* Просмотр U-Mail (список, отдельные письма)
* Отправка и приём U-Mail
* Цитирование и пересылка U-Mail
* Уведомление о доставке
* Копирование в Отправленные
* Пакетное удаление U-Mail (долгий тап на списке)

###Дискуссии
* Просмотр всего списка дискуссий пользователя
* Просмотр списка дискуссий по дневнику
* Просмотр списка дискуссий с фильтром по непрочитанным (долгий тап на дискуссии)

###Уведомления
* Возможность периодической проверки сайта дневников для уведомления пользователя
* Возможность удерживания сервиса в памяти (невытеснения другими приложениями системы Android)

###Комментирование
* Черновики
* Дополнительные параметры (темы, настроение, музыка)
* Создание закрытых постов с любыми параметрами приватности, поддерживаемыми сайтом
* Создание опросов с любыми параметрами, поддерживаемыми сайтом
* Запрет комментирования
* Подгружаемые шаблонные темы постов (с сообществ/дневников)
* Прикрепление изображений с любыми параметрами, поддерживаемыми сайтом
* Смайлы (+анимированные)
* Спецсимволы для клавиатур без них (Samsung)
* Специальная вставка (курсив, полужирный и т.д.)

###Иное
+ Темы оформления (в меню)
+ Возможность отключения загрузки внешних изображений для экономии трафика
+ Запрос "Кто онлайн"
+ Возможность обхода CloudFlare-валидации (только без капчи)

###Используемые компоненты
1. `com.android.support:support-v4`
2. `com.android.support:appcompat-v7`
3. `com.android.support:design`  
[Android Support Libraries (Apache 2.0)](http://developer.android.com/tools/support-library/index.html)
4. `pl.droidsonroids.gif:android-gif-drawable`  
[Android animated GIF library (MIT)](https://github.com/koral--/android-gif-drawable)
5. `com.afollestad:material-dialogs`  
[Android Material-style dialogs (MIT)](https://github.com/afollestad/material-dialogs)
6. `com.github.ragunathjawahar:android-saripaar:android-saripaar`  
[Android forms validation (Apache 2.0)](https://github.com/ragunathjawahar/android-saripaar)
7. `com.j256.ormlite:ormlite-android`  
[Java lightweight ORM (ISC)](https://github.com/j256/ormlite-android)
8. `org.mozilla:rhino`  
[Java JavaScript plugin (MPL 2.0)](https://github.com/mozilla/rhino)
9. `com.squareup.okhttp:okhttp`  
[HTTP+HTTP/2 client (Apache 2.0)](https://github.com/square/okhttp)
10. `com.google.code.gson:gson`  
[Java JSON (de)serialization (Apache 2.0)](https://github.com/google/gson)

#Лицензия: GPLv3+
Данная программа является свободным программным обеспечением. Вы вправе распространять ее и/или модифицировать в соответствии с условиями версии 3 либо, по вашему выбору, с условиями более поздней версии Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation.
Я распространяю данную программу в надежде на то, что она будет вам полезной, однако **не предоставляю на нее никаких гарантий**, в том числе **гарантии товарного состояния при продаже и пригодности для использования в конкретных целях**. Для получения более подробной информации ознакомьтесь со [Стандартной Общественной Лицензией GNU](http://www.gnu.org/copyleft/gpl.html).