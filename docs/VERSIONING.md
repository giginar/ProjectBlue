# Commit tabanlı sürümleme

Her commit için sürüm build sırasında otomatik hesaplanır:

```text
versionName = <major>.<minor>.<commit sayısı>-g<commit'in ilk 12 karakteri>
versionCode = Git geçmişindeki erişilebilir commit sayısı
```

Örnek: `0.1.5-gabc123def456`, Android `versionCode=5`.
`major` ve `minor`, kökteki `version.properties` dosyasındadır.
Patch alanı commit sayısıdır; her commit'te elle dosya artırmaya gerek yoktur.
Yeni bir ana/alt ürün sürümüne geçerken yalnızca major/minor değiştirilir;
Android versionCode sayacı sıfırlanmaz.

Sürümün tek kaynağı `gradle/versioning.gradle` dosyasıdır. Bu değer:

- Android manifest'inin `versionName` ve `versionCode` alanlarına,
- Gradle modül/dağıtım sürümlerine,
- Üretilen `BuildInfo` sınıfı üzerinden menü ve desktop pencere başlığına,
- APK adına, artifact klasörüne ve GitHub artifact adına yazılır.

## Komutlar ve çıktılar

```powershell
.\android.bat version
.\android.bat build
.\android.bat install
```

SDK gerektirmeyen sürüm sorgusu: `gradlew.bat -q :printVersion`.

```text
build/version/version.json
build/artifacts/0.1.5-gabc123def456/
  ProjectBlue-0.1.5-gabc123def456-debug.apk
  ProjectBlue-0.1.5-gabc123def456-debug.apk.sha256
  BUILD.json
```

`BUILD.json`: sürüm adı/kodu, tam commit kimliği, dirty durumu, APK adı ve SHA-256.
Bu metadata ve APK aynı Gradle yapılandırmasından üretilir; Windows ve GitHub script'leri
ayrı sayaç tutmaz. APK export görevi `:android:packageDebugApk`'dir.
Standart AGP çıktısı `android/build/outputs/apk/debug/android-debug.apk` olarak kalır;
bunun içindeki sürüm de otomatik hesaplanır.

## Tekrarlanabilirlik ve Git geçmişi

- Aynı temiz commit'in yerel ve CI build'leri **aynı sürümü** alır. Yeniden çalıştırma
  sürümü artırmaz. İmza anahtarı/SDK ortamı farklı olduğundan APK byte'larının aynı
  olması garanti edilmez.
- Commit'lenmemiş değişiklik veya ignore edilmemiş yeni dosya varsa sürüme
  `-dirty` eklenir. Ignore edilen build/SDK dosyaları bunu etkilemez.
  Dirty build, yeni commit veya yeni versionCode yaratmaz.
- Git geçmişi tam olmalıdır. Shallow clone yanlış küçük bir versionCode üretmek yerine
  açıklayıcı hatayla durur; `git fetch --unshallow` ile düzeltilir.
- Proje kendi Git checkout'unun kökünde ve en az bir commit'e sahip olmalıdır.
  Git'siz ZIP kaynak veya üst dizindeki ilgisiz Git deposu sürüm kaynağı olarak kullanılmaz.
- Normal, geçmişi koruyan `main` ilerleyişinde versionCode artar. Merge'lerde sayaç
  erişilebilir bütün commit'leri sayar; değer birden fazla artabilir.
- Farklı dallar aynı commit sayısına sahip olabilir. Kısa hash sürüm adlarını ayırır;
  versionCode tüm dallar arasında küresel sıra değildir. Android'e dağıtım sırası
  `main` üzerinden izlenmelidir. Geçmişi yeniden yazmak/force-push sayaç garantisini bozar.
- Android versionCode için 1–2.100.000.000 aralığı kontrol edilir.
  [Android sürümleme kuralları](https://developer.android.com/studio/publish/versioning).

Git hook'u, otomatik sürüm commit'i veya build'in kaynak dosyalarını değiştirmesi yoktur.
Bu nedenle sürüm artışı kendini tetikleyen commit/push döngüsü oluşturmaz.

## CI ve doğrulama

GitHub checkout `fetch-depth: 0` kullanır. Pull request'lerde kaynak commit'in kendisi
derlenir; geçici merge commit'i sürüm kimliği olarak kullanılmaz. Her dal push'unda son
commit için APK oluşur. Birden fazla commit tek push ile gönderilirse CI son commit'i
derler; önceki commit'lerin sürümleri de checkout edilerek aynı kuralla hesaplanabilir.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/test-versioning.ps1
```

Geçici Git depolarıyla test edilenler: temiz/tekrarlanan build, dirty ve untracked
değişiklikler, commit sonrası sayaç artışı, eşit sayılı farklı dalların ayrılması,
commitsiz, iç içe ve shallow depo reddi. Aynı test GitHub'da PowerShell Core ile çalışır.
Test fixture'ları git dışında kalan `build/versioning-tests/` altında oluşturulur.
