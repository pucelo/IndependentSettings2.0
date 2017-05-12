set app_package=preference.android.testpref2
set dir_app_name=TestPref2
set MAIN_ACTIVITY=SettingsActivity


set ADB=J:\Android\SDK\platform-tools\adb.exe
set ADB_SH=shell su -c
set path_sysapp=/system/priv-app
set apk_host= %CD%\app\build\outputs\apk\app-debug.apk
set apk_name=%dir_app_name%.apk
set apk_target_dir=%path_sysapp%/%dir_app_name%
set apk_target_sys=%apk_target_dir%/%apk_name%


%ADB% push %apk_host% /sdcard
%ADB% %ADB_SH% mount -o rw,remount /system
TIMEOUT /T 3
%ADB% %ADB_SH% rm -rf %path_sysapp%/%dir_app_name%
%ADB% %ADB_SH% mkdir %path_sysapp%/%dir_app_name%
%ADB% %ADB_SH% chmod 755 %path_sysapp%/%dir_app_name%
%ADB% %ADB_SH% mv /sdcard/app-debug.apk %apk_target_dir%
%ADB% %ADB_SH% rename %apk_target_dir%/app-debug.apk %apk_target_dir%/%apk_name%
%ADB% %ADB_SH% chmod 644 %apk_target_dir%/%apk_name%
%ADB% %ADB_SH% mount -o ro,remount /system
TIMEOUT /T 3
%ADB% shell am force-stop %app_package%
%ADB% shell am start -n %app_package%/%app_package%.%MAIN_ACTIVITY% -a android.intent.action.MAIN -c android.intent.category.LAUNCHER 


