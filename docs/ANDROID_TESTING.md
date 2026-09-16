# Android'de Project Blue denemesi

Android Studio kurmadan IntelliJ IDEA Community ve JDK 17/21 ile APK hazırlanabilir.
Windows için kökteki `android.bat` kullanılır. Telefon için **Android 8.0 veya üzeri** gerekir.

## Bu bilgisayarda

```powershell
# SDK kurulu değilse Google'dan indirir, testleri çalıştırır ve APK üretir
.\android.bat build

# Yalnızca Android SDK kurulumu / paket hazırlığı
.\android.bat setup

# SDK kurmadan mevcut commit'in sürümünü göster
.\android.bat version
```

SDK varsayılan olarak `%LOCALAPPDATA%\Android\Sdk` altına kurulur. Mevcut
`local.properties`, `ANDROID_HOME` veya `ANDROID_SDK_ROOT` varsa bu konum kullanılır.
Başka konum seçmek için: `android.bat setup -SdkPath D:\Android\Sdk`.

Kurulum Google'ın **15859902** Windows command-line tools arşivini yayımlanmış
SHA-256 ile doğrular; SDK 36, build-tools 35.0.0 ve platform-tools paketlerini kurar.
Gerekli paketlerin SDK lisansları `sdkmanager` üzerinden kabul edilir.
[Resmî araçlar ve lisans](https://developer.android.com/studio),
[sdkmanager belgeleri](https://developer.android.com/tools/sdkmanager).

Sistem genelindeki PATH değiştirilmez. SDK yolu git dışında tutulan
`local.properties` dosyasına yazılır. İndirilen SDK, imza anahtarları ve APK'lar
kaynak kodu deposuna eklenmez.

Her build JUnit testlerini, asset doğrulamasını ve Android lint'i çalıştırır.
APK imzası ve 16 KB ZIP hizalaması doğrulandıktan sonra şu dosyalar hazırlanır:

- `build/artifacts/<sürüm>/ProjectBlue-<sürüm>-debug.apk`
- Aynı APK adıyla `.sha256` dosyası ve `BUILD.json`

Örnek: `ProjectBlue-0.1.5-gabc123def456-debug.apk`. Sürüm, Android paket bilgisi ve
dosya adında aynıdır; her sürüm ayrı klasörde tutulur. Önceki APK'lar korunur.
Sürüm kuralları: [VERSIONING.md](VERSIONING.md).

APK'yı telefona kopyalayıp dosya yöneticisinden açabilirsin. Android sorarsa APK'yı açtığın
dosya yöneticisi/tarayıcı için “bu kaynaktan uygulama yükleme” iznini ver.

## USB ile tek komutta kur ve aç

1. Telefonda geliştirici seçeneklerini ve **USB hata ayıklama** özelliğini aç.
2. Veri taşıyan USB kablosuyla bağla ve telefondaki bilgisayar yetkilendirmesini kabul et.
3. Çalıştır:

```powershell
.\android.bat devices
.\android.bat install
```

`install`, APK'yı yeniden derler, testleri geçirir, `adb install -r` ile yükler ve
`AndroidLauncher` activity'sini başlatır. Normal güncellemelerde profil korunur.
Birden fazla cihaz/emülatör bağlıysa açıkça seç:

```powershell
.\android.bat install -Serial DEVICE_ID
.\android.bat logs -Serial DEVICE_ID
```

`logs`, AndroidRuntime ve libGDX loglarını izler; Ctrl+C ile durdurulur.
Cihaz bulunamazsa veya yetkilendirilmemişse script açıklayıcı hata verir; rastgele
cihaz seçmez ve uygulamayı kendiliğinden silmez.
Windows'ta bazı telefonlar üreticinin ADB USB sürücüsünü gerektirebilir.
[Resmî cihaz bağlantısı adımları](https://developer.android.com/studio/run/device).

## GitHub'dan APK indir

[Android Debug APK iş akışı](https://github.com/giginar/ProjectBlue/actions/workflows/android-debug.yml)
Bütün dallara yapılan push'larda, pull request'lerde ve **Run workflow** ile elle çalışır.

1. GitHub hesabınla giriş yapıp iş akışının başarılı çalışmasını aç.
2. **Artifacts** bölümünden `ProjectBlue-<sürüm>-debug` arşivini indir.
3. ZIP'i açıp `ProjectBlue-<sürüm>-debug.apk` dosyasını telefona kopyala ve yükle.

Arşiv APK, SHA-256 ve sürüm/kaynak commit'ini belirten `BUILD.json` içerir. APK artifact'leri
30 gün, test/lint raporları 14 gün saklanır.
[GitHub artifact indirme belgesi](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

Bunlar **debug imzalı test paketleridir**. Yerel ve CI paketlerinin imza anahtarları
farklıdır; CI çalışmaları arasında da debug anahtarı değişebilir. Android
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` hatası verirse eski paketi telefondan kaldırıp
yenisini kurmak gerekir; kaldırma yerel profili siler. Script bu silmeyi otomatik yapmaz.
Mağazaya yayın, release anahtarı ve kalıcı CI imza yönetimi bu akışın kapsamı dışındadır.

## Telefonda kısa kontrol

- Menü açılıyor, ses/müzik ayarı çalışıyor ve dokunma doğru konuma karşılık geliyor.
- Sürükleme denizaltıyı hareket ettiriyor; otomatik ateş ve çarpışmalar çalışıyor.
- Şişenin yakınında temizlik, kaplumbağanın yanında 1.5 saniye bekleyince kurtarma oluyor.
- Pause düğmesi ve Android geri tuşu çalışıyor.
- Ana ekrana geçip geri dönünce oyun pause ekranında kalıyor; devam edince zaman sıçramıyor.
- Üç dakika sonunda sonuç ve yıldızlar görünüyor; yeniden oynama yeni bölüm başlatıyor.
- Süreç tamamen kapatılıp yeniden açıldığında menü ve kayıtlı ayarlar geliyor.

Fiziksel cihaz/emülatör bağlı olmadığından bu oturumda gerçek Android açılışı ve
dokunma/GPU context davranışı doğrulanamadı. APK derleme, imza, hizalama ve statik
lint kontrolü cihaz testinin yerine geçmez.
