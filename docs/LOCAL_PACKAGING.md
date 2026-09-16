# Yerel APK ve Windows kurulum paketi

**`paketle.bat` dosyasına çift tıkla.** Testlerden sonra paylaşılabilir dosyalar hazırlanır,
çıktı klasörü açılır ve terminal kapanmadan önce tuş bekler. Hata olursa mesaj ekranda kalır.
GitHub hesabı, push veya Actions üzerinden dosya indirme adımı gerekmez.

IntelliJ terminalinden:

```powershell
# İki platform, pencere/tuş bekleme yok
.\paketle.bat All

# Yalnızca bir platform
.\paketle.bat Android
.\paketle.bat Windows

# Paketlenmiş EXE ile gerçek OpenGL oyun testi de çalıştır
.\paketle.bat All -SmokeTest

# İşlem sonunda çıktı klasörünü aç
.\paketle.bat All -OpenOutput
```

Script başka bir çalışma dizininden mutlak yoluyla da çağrılabilir. `-SmokeTest` gerçek
grafik oturumu gerektirir; menü, sürükleme, pause/resume, yaşam döngüsü, ekran oranı,
180 saniyelik bölüm, sonuç ve yeniden oynama kontrollerini çalıştırıp kapanır.
Sistemin `JAVA_HOME` ve Java PATH'i bu test boyunca kaldırılır; gömülü runtime sınanır.
Normal paketlemede JUnit/asset kontrolleri, Android seçildiyse lint/imza/hizalama zorunludur.

## Ne göndereceğim?

Örnek çıktı:

```text
dist/
  LATEST.txt
  0.1.7-gabc123def456/
    ProjectBlue-0.1.7-gabc123def456-android.apk
    ProjectBlue-0.1.7-gabc123def456-windows-x64-setup.exe
    ProjectBlue-0.1.7-gabc123def456-windows-x64-portable.zip
    BUILD.json
    SHA256SUMS.txt
    OKU.txt
```

| Arkadaşının cihazı | Gönderilecek dosya | Ne yapacak? |
|---|---|---|
| Android 8.0+ telefon/tablet | `*-android.apk` | Telefona kaydedip açacak; Android isterse açtığı uygulamaya APK yükleme izni verecek. |
| Windows x64 | `*-windows-x64-setup.exe` | Kurulum sihirbazını çalıştıracak, masaüstü/Başlat menüsünden oyunu açacak. |
| Windows x64, kurulum istemiyor | `*-windows-x64-portable.zip` | ZIP'in tamamını çıkarıp `ProjectBlue.exe` açacak. Tek başına EXE yeterli değildir. |

EXE ve ZIP **Java dahil** dağıtımdır. Oyuncunun Gradle, Git, JDK veya Android SDK'ya
ihtiyacı yoktur. Kurulum mevcut Windows kullanıcısına yapılır; yönetici kurulumu gerekmez.
Windows Ayarları'ndan kaldırılabilir. Oyun profili `%USERPROFILE%\.projectblue` altında
tutulur; kurulumu kaldırmak profili silmez. macOS/Linux installer bu Windows script'inin
kapsamında değildir; geliştirme için Gradle desktop run kullanılabilir.

APK debug imzalı test paketidir. Windows EXE henüz kod imzalı değildir; indirilmiş bir
pakette Windows yayıncı doğrulama uyarısı gösterebilir. Telefona hep bu bilgisayarda
üretilen APK gönderilirse yerel debug anahtarı korunduğu sürece güncelleme imzası sabit kalır.
Farklı bilgisayar/CI anahtarıyla üretilen APK eski kurulumun üzerine yüklenmeyebilir;
eski uygulamayı kaldırmak profili siler. Mağaza yayını/release imzası bu araçta yoktur.

## Sürüm nasıl ilerliyor?

Mevcut [commit tabanlı sürümleme](VERSIONING.md) kullanılır:

```text
<major>.<minor>.<commit sayısı>-g<12 karakter commit kimliği>[-dirty]
```

- Yeni commit → yeni sürüm; yerel commit yeterlidir, push gerekmez.
- Aynı commit'i tekrar paketlemek sürümü artırmaz.
- Commit'lenmemiş değişiklik varsa `-dirty` eklenir. Dirty paketler aynı sürümü paylaşabilir;
  arkadaşına ayırt edilebilir yeni güncelleme göndermek için önce değişikliklerini commit'le.
- Menü, pencere başlığı, APK manifest'i ve dosya adları aynı tam sürümü taşır.
- Windows Installer'ın sayısal sürümü `major.minor.commitSayısı` olur. Git kimliği ve
  `-dirty` dosya adında ve oyun içinde korunur. Windows alan sınırları `255.255.65535`'tir;
  aşıldığında script yanlış sürüm üretmek yerine durur.
- Yeni commit'in Windows kurulum paketi sabit upgrade kimliğiyle önceki sürümü günceller.
  Aynı sayısal sürümün farklı dirty/dal paketini kurarken önce eski kurulumu kaldırmak
  gerekebilir; hızlı denemeler için portable ZIP kullanılabilir.
- Aynı sürüm klasörü ancak yeni seçilmiş paketler tamamen hazırsa değiştirilir. Önceki
  aynı sürüm çıktısı `build/packaging/previous-*` altında saklanır; diğer sürümler korunur.
  `Windows` veya `Android` seçimi bu klasöre yalnızca seçilen platformu koyar.
  `dist/LATEST.txt` son başarılı çıktı klasörünün adını içerir.

`BUILD.json` tam commit'i, sürümü, hedefi, gömülü Java sürümünü ve her paketin
boyut/SHA-256 değerlerini içerir. Installer byte'larının aynı kalması garanti edilmez.
Build sırasında kaynak kodunu değiştirme; iki platform tek Gradle çağrısıyla aynı sürüm
görüntüsünden hazırlanır. İki paketleme script'inin aynı anda çalışması kilitle engellenir.

## Geliştirici bilgisayarında gerekenler

- Windows x64, Git geçmişi tam checkout, **x64 JDK 17 veya 21** (`JAVA_HOME` veya PATH).
  Yerelde doğrulanan JDK: Microsoft OpenJDK **21.0.12**; kaynak kodu Java 17 kalır.
- İlk çalıştırmada Gradle/Maven bağımlılıkları, eksikse Android SDK ve WiX araçları için
  internet gerekir. Araç hazırlığı otomatik yapılır; GitHub'da hesap açılmaz.
- Android SDK hazırlığı mevcut `android.bat prepare` üzerinden yapılır:
  SDK 36 / build-tools 35.0.0. Konum kuralları [Android rehberinde](ANDROID_TESTING.md).
- Windows installer için **WiX 3.14.1** taşınabilir arşivi resmî WiX sürümünden indirilir.
  SHA-256 sabitlenmiştir:
  `6ac824e1642d6f7277d0ed7ea09411a508f6116ba6fae0aa5f2c7daa2ff43d31`.
  Bu hash ilgili resmî arşiv indirilerek ölçülmüştür; upstream imza iddiası değildir.
  Önbellek `%LOCALAPPDATA%\ProjectBlue\build-tools`; araç lisansı arşivin `LICENSE.TXT`
  dosyasındadır (Microsoft Reciprocal License). Sistem PATH'i/kurulu programlar değiştirilmez.
- JDK'nın `jpackage` aracı, oyunu ve gerekli Java modüllerini beraber paketler. Java'nın
  `runtime/legal` bildirimleri ve jar dosyalarındaki bağımlılık lisansları korunur.
  Paket simgesi özgün Android geometrisinden yerelde üretilir; asset envanterinde kayıtlıdır.

Build çıktıları, araçlar ve imza anahtarları kaynak koduna commit edilmez. Hata olursa
`dist/LATEST.txt` değiştirilmez; geçici çalışma dosyaları belirtilen `build/packaging/`
dizininde teşhis için kalır. Scriptin başarılı çıktısı `READY: ...` satırıdır.

## Doğrulama durumu

32 JUnit testi, Android lint/imza/16 KB hizalama, iki platformun birlikte paketlenmesi,
Android'in ayrı/başka çalışma dizininden paketlenmesi ve paket içindeki Java ile gerçek
OpenGL oyun testi doğrulandı. Eşzamanlı paketleme ve geçersiz hedef/test birleşimi reddedildi.
Bu geliştirme ortamında sessiz Windows kurulum denemesi sonuçlanmadı; installer oluşturma
ve iç sürüm bilgisi kontrol edildi, kurulum/güncelleme/kaldırma akışı henüz uçtan uca
doğrulanmadı. Fiziksel Android cihazında deneme de bağlı cihaz gerektirir.

Resmî teknik kaynaklar: [JDK 21 jpackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html),
[WiX 3.14.1](https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm),
[Windows Installer sürüm alanları](https://learn.microsoft.com/en-us/windows/win32/msi/productversion).
