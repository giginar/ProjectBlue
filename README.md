# Project Blue

Java/libGDX ile yazılmış, Android öncelikli özgün bir portre 2D sualtı shooter prototipi.
Denizaltıyla drone'ları etkisizleştir, plastikleri temizle, kaplumbağaları kurtar ve
**180 saniyelik The Quiet Reef** bölümünü tamamla. Temizlik/kurtarma ile su rengi,
mercanlar ve balık yoğunluğu gözle görülür biçimde iyileşir.

Sky Force yalnızca tür düzeyinde referanstır; isim, asset, UI, bölüm, düşman, hikâye
veya kod kopyası kullanılmamıştır. İnternetten oyun asset'i indirilmemiştir.

## Hızlı başlangıç — Windows / IntelliJ IDEA Community

1. Kök dizini **Gradle projesi olarak** aç. Gradle dağıtımı için **Wrapper** seç.
2. Project SDK ve Gradle JVM için **JDK 17 veya 21** seç.
3. Terminalde:

```powershell
.\gradlew.bat :lwjgl3:run
```

Gradle araç penceresinden `lwjgl3 > application > run` da çalıştırılabilir. Android geliştirme
eklentisine ihtiyaç duymadan Java oyun kodunu ve masaüstü sürümünü geliştirebilirsin.
Android paketleme için ayrıca Android SDK gerekir. Makineye özel SDK yolları repoya yazılmaz.

Linux/macOS: `sh ./gradlew :lwjgl3:run`. macOS için Gradle run görevi ilk iş parçacığı
JVM seçeneğini ekler. Bu oturumda doğrulanan platform Windows x64'tür.

## Kontroller

- Mouse sol tuşunu / tek parmağını oyun alanında tutup **sürükle**. Göreli hareket,
  denizaltının ilk dokunulan noktaya sıçramasını önler.
- Otomatik ateş sürekli çalışır. Camgöbeği mermiler oyuncuya, kırmızı mermiler drone'lara aittir.
- Şişeye 112 birim yaklaş: 0.42 saniyede temizlik ışını toplar.
- Kaplumbağanın 96 birim yakınında **kesintisiz 1.5 saniye** kal: ağ çözülür ve kaplumbağa uzaklaşır.
- Altın salvage parçaları yaklaşınca mıknatısla toplanır.
- Sağ üst pause düğmesi, **Esc**, **P** veya Android geri tuşu duraklatır.
- Pause ekranından devam edilir. Menüye dönmek mevcut dalışı bitirir.
- Ses ve müzik ana menüden/pause ekranından açılıp kapatılır; ayarlar kaydedilir.

## Sürümler ve uyumluluk

2026-09-16 tarihinde resmî kaynaklardan kontrol edildi:

| Bileşen | Sürüm / karar |
|---|---|
| Java kaynak ve bytecode | **17** (`--release 17`); yerel build/test JVM: Microsoft OpenJDK **21.0.12** |
| libGDX | **1.14.2**, güncel kararlı sürüm |
| LWJGL | **3.3.3**, libGDX backend'inin yayımladığı bağımlılık |
| Gradle Wrapper | **8.13**, resmî dağıtım SHA-256 kontrolü etkin |
| Android Gradle Plugin | **8.13.2**, Java 17 ve API 36 için uyumlu sabit sürüm |
| Android compile / target | **36 / 36** |
| Android min SDK | **26** (Android 8.0); kullanılan standart Java API'leri için sade taban |
| Android uygulama kimliği | `com.projectblue.game` |
| Android ABI | `arm64-v8a`, `armeabi-v7a`, `x86_64` |
| JUnit | **5.13.4**, Jupiter / JUnit Platform |
| Mantıksal alan | **540 × 960**, FitViewport, portre |

Google Play yeni uygulama/güncellemeler için 31 Ağustos 2026 itibarıyla en az API 36 istiyor.
AGP 8.13, API 36.1'e kadar destekliyor ve Gradle 8.13/JDK 17 ile uyumlu.
Gradle/AGP için “en son sürüm” iddiası yok; birlikte doğrulanmış sade sürüm çifti kullanıldı.

Kaynaklar: [libGDX sürümleri](https://libgdx.com/dev/versions/),
[Google Play hedef API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en),
[AGP 8.13 uyumluluğu](https://developer.android.com/build/releases/agp-8-13-0-release-notes),
[JUnit 5.13.4](https://docs.junit.org/5.13.4/release-notes/),
[16 KB Android sayfa desteği](https://developer.android.com/guide/practices/page-sizes).

## Test, dağıtım ve Android build

Android APK üretimi ve telefona kurulum için [Android deneme rehberi](docs/ANDROID_TESTING.md).
Windows'ta `android.bat build` eksik SDK'yı kurar, test/lint kontrollerini çalıştırır ve
`build/artifacts/ProjectBlue-debug.apk` üretir. USB ile bağlı telefon için
`android.bat install` APK'yı kurup oyunu açar.
GitHub Actions, her `main` push'unda indirilebilir APK artifact'i hazırlar.

```powershell
# Android SDK olmadan saf Java testleri ve asset lisans kontrolü
.\gradlew.bat :check

# Gerçek OpenGL penceresinde otomatik masaüstü kontrolü; tamamlanınca kapanır
.\gradlew.bat :lwjgl3:run --args=--smoke

# Java runtime gerektiren masaüstü dağıtımı
.\gradlew.bat :lwjgl3:installDist
.\lwjgl3\build\install\lwjgl3\bin\lwjgl3.bat

# SDK olmadan üç Android ABI kütüphanesini hazırlama
.\gradlew.bat :android:extractNatives

# Android SDK kuruluysa
.\gradlew.bat :android:assembleDebug
```

**Başındaki `:` önemlidir:** `:check` yalnızca kök kontrol görevini çalıştırır;
`check` alt projelerin Android lint görevlerini de seçebilir ve SDK isteyebilir.

Android SDK'da `platforms;android-36`, `build-tools;35.0.0` ve `platform-tools`
bulunmalı; SDK lisansları SDK Manager üzerinden kabul edilmiş olmalı.
AGP 8.13'ün varsayılan build-tools sürümü 35.0.0'dır; compile/target SDK yine 36'dır.
SDK yöneticisi mevcutsa kurulum komutu:

```text
sdkmanager "platforms;android-36" "build-tools;35.0.0" "platform-tools"
```

SDK yolunu `ANDROID_HOME` ile tanımla veya kökte git tarafından dışlanan
`local.properties` oluştur:

```properties
sdk.dir=C\:/Users/YOUR_USER/AppData/Local/Android/Sdk
```

APK: `android/build/outputs/apk/debug/android-debug.apk`.
Bağlı cihazda: `adb install -r android/build/outputs/apk/debug/android-debug.apk`.

## Mimari

```text
core/       com.projectblue.game
  ProjectBlueGame             Uygulama ve kaynak sahipliği
  config/GameConfig          Oyun dengesi, süre, skor, limitler
  logic/                     Saf Java GameWorld, Rules, LevelResult, seed'li random
  events/GameEvents          Senkron, nesne üretmeyen oyun olayı dağıtımı
  input/                     PlayerInput, PointerInput, MenuInput
  render/OceanRenderer       Programatik sualtı görselleri
  ui/                        HUD, palet, ortak çizim kaynakları
  screens/                   Boot, MainMenu, Game, Pause, Result, ScreenRouter
  assets/GameAssets          Merkezi AssetManager
  audio/AudioService         Ses/müzik ayarı ve yaşam döngüsü kontrolü
  save/                      Sürümlü profil, checksum, güvenli varsayılanlar
  platform/                  Servis interface'leri ve ortak No-Op davranış
lwjgl3/                      Masaüstü launcher, platform adaptörü, GL smoke kontrolü
android/                     Android launcher, güvenli pencere inset'leri, No-Op adaptör
assets/                      Özgün font/ses ve lisans envanteri
tools/GenerateAssets.java     Asset'lerin çevrimdışı yeniden üretimi
```

- Simülasyon **60 sabit adım/saniye**; uzun frame/resume aralığı en fazla 0.1 saniye.
  `GameWorld`, Android veya libGDX import etmez. UI kurallara müdahale etmez.
- Mermi, drone, plastik, kaplumbağa, salvage ve parçacıklar sabit kapasiteli,
  önceden oluşturulmuş havuzlardan gelir. Görsel efekt random akışı gameplay'den ayrıdır.
- HUD yeniden kullanılan StringBuilder'larla saniyede 10 kez güncellenir.
  Ekranlar ortak GPU kaynaklarını dispose etmez; uygulama kapatırken sahipleri dispose eder.
- ScreenRouter geçişleri frame sonunda uygular. Pause ekranı mevcut GameScreen'i tutar,
  simülasyonu ilerletmez; sonuç veya menüde önceki bölümün abonelikleri bırakılır.
- Android `onPause/onResume` libGDX üzerinden yönlendirilir. Arka plana geçişte input
  sıfırlanır, oyun ve ses durur, profil yazılır. Geri gelince kullanıcı **Resume Dive** seçer.
- Android sistem çubukları ve cutout inset'leri oyun View'ına uygulanır. FitViewport,
  geniş ekran/tablet veya yeniden boyutlandırmada oyun alanını kırpmaz; boş alan bırakır.
- `AdsService`, `ConsentService`, `AchievementService`, `AnalyticsService`,
  `PlatformService` üzerinden platform sınırı çizilir. No-Op reklam servisi hazır
  değildir ve hiçbir ödül vermez; ağ izni, reklam SDK'sı veya hesap bağlantısı yoktur.
- Profil şeması **v1**, açık v0 geçişi ve CRC32 ile yazım bozulması kontrolü içerir.
  Yazarken geçici dosya/yedek kullanılır; eksik, bozuk veya bilinmeyen şemada varsayılan
  profil açılır. Depolama hatası oyunu kapatmaz ve menü/sonuçta gösterilir.
  CRC32 güvenlik/anti-cheat amacı taşımaz.
- Desktop kayıt: kullanıcı klasöründe `.projectblue/profile.properties`.
  Android kayıt: uygulamanın özel files dizini. Smoke modu ayrı `build/smoke/profile` kullanır.
- İşletim sistemi süreci tamamen öldürürse yeni açılış ana menüdür; devam eden dalış
  diskten geri yüklenmez. Ayarlar ve tamamlanan dalışların sonuçları kalıcıdır.

## Bölüm ve değerlendirme kuralları

Bölümde 40 drone, 36 plastik, 5 kaplumbağa bulunur. Sabit seed, değişmeyen spawn takvimi
ve bölümün sonundaki boşluk aynı bölümün öğrenilebilir olmasını sağlar.

- Combat = yok edilen / 40; Cleanup = toplanan / 36; Rescue = kurtarılan / 5.
- Integrity = kalan sağlık / 100. Yüzdeler 0–100 aralığında tutulur.
- Drone 100, plastik 40, kurtarma 300, her salvage birimi 20 puan.
  Her drone 5 salvage bırakır. Bölüm tamamlanınca 500 + kalan sağlık × 5 eklenir.
- Başarısız dalış **0 yıldız**; tamamlanan dalış en az **1 yıldız**.
  Dört kategori ortalaması ≥45 ise **2**; ortalama ≥75 ve her kategori ≥50 ise **3 yıldız**.
- Görsel iyileşme: Cleanup × %55 + Rescue × %45.

## Doğrulananlar ve sınırlar

2026-09-16 Windows x64 oturumunda:

- **32 JUnit 5 testi başarılı:** istenen yedi kural grubu yanında çarpışma, havuz,
  sürekli kurtarma, salvage, seed tekrarlanabilirliği, 180 saniyelik hayatta kalma,
  kayıt round-trip, bozulma, şema geçişi ve yazma hatası.
- Gerçek LWJGL3/OpenGL penceresinde boot → menü → drag → pause → resume →
  lifecycle pause/resume → geniş viewport → 180 saniye → sonuç → kayıt → tekrar oynama başarılı.
- Masaüstü dağıtımı üretildi. Test raporu `core/build/reports/tests/test/index.html`;
  ekran görüntüleri `build/smoke/` içinde.
- Android SDK 36 kuruldu; **debug APK üretildi**. Paket kimliği, min/target SDK,
  üç ABI, APK imzası ve **16 KB ZIP hizalaması** doğrulandı. arm64-v8a/x86_64 ELF LOAD
  segmentleri de **16384 bayt hizalı**. Android lint hata vermeden tamamlandı;
  portre yönü, sürüm önerisi ve manifest uyumluluğuyla ilgili uyarılar raporda görülebilir.
- **Bağlı Android cihaz/emülatör olmadığından gerçek Android açılışı, dokunma ve GPU
  context kaybı testi yapılmadı.** APK üretimi bu testlerin veya Play mağazasına yayın
  hazırlığının yerine geçmez.
- Görsel/sesler özgün placeholder'lardır; profesyonel sanat, gerçek müzik prodüksiyonu,
  lokalizasyon, farklı cihazlarda performans profilleme ve kapsamlı oyun dengelemesi yapılmadı.
- Bölüm/pilot seçimi, upgrade sistemi, gerçek reklam, consent SDK, Google Play Games,
  çevrimiçi analytics ve ek bölümler bu aşamanın kapsamı dışındadır.

Asset politikası ve kaynak envanteri: [ASSET_LICENSES.md](assets/licenses/ASSET_LICENSES.md).
Kaynağı/lisansı doğrulanmayan veya doğrulanmış hash'i değişen asset paketlemeyi durdurur.
