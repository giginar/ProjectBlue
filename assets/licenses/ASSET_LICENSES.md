# Project Blue — Asset lisans envanteri

Doğrulama tarihi: 2026-09-16. Oyunda kullanılan bütün görsel ve sesler bu proje için
programatik olarak oluşturulmuştur. İnternetten font, görsel, müzik veya ses indirilmemiştir.
Bu kayıt yalnızca oyun asset'lerini kapsar; libGDX ve LWJGL gibi yazılım bağımlılıkları
kendi lisanslarıyla dağıtılır.

| Asset | Kaynak | Üretici | Lisans | İndirme adresi | Kullanım notu |
|---|---|---|---|---|---|
| `fonts/blue.fnt` | `tools/GenerateAssets.java`, özgün 5x7 glyph matrisi | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — yerel üretim | BMFont metrikleri; haricî font türevi değil; doğrulandı |
| `fonts/blue.png` | Aynı üretici araç, glyph matrisi | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — yerel üretim | Font atlası; alt/üst harfler aynı matris; doğrulandı |
| `audio/pulse.wav` | Aynı araç, matematiksel sinüs taraması | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — yerel üretim | 0.09 saniye ateş sesi, mono PCM; doğrulandı |
| `audio/collect.wav` | Aynı araç, iki sinüs harmonisi | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — yerel üretim | 0.28 saniye toplama tonu; sample veya kayıt yok; doğrulandı |
| `audio/ocean.wav` | Aynı araç, periyodik sinüs harmonileri | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — yerel üretim | 8 saniye kesintisiz ortam döngüsü, müzik kanalı; doğrulandı |
| Denizaltı, drone, mermiler, plastik, salvage | `OceanRenderer.java` | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — çalışma anında çizim | Kod içindeki geometrik şekiller; doğrulandı |
| Kaplumbağa, mercan, balık, akıntı, parçacık | `OceanRenderer.java` | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — çalışma anında çizim | Kod içindeki geometrik şekiller; doğrulandı |
| Menü, HUD, yıldızlar, yükleme çubuğu | `ui/` ve `screens/` | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — çalışma anında çizim | Haricî UI skin veya ikon yok; doğrulandı |
| `android/src/main/res/drawable/ic_blue.xml` | Özgün Android vector path | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — yerel üretim | Launcher simgesi; doğrulandı |
| Windows `ProjectBlue.ico` (build çıktısı) | `tools/GenerateWindowsIcon.java`, özgün Android simgesinin geometrisi | Project Blue için Codex tarafından üretildi | Proje için üretilmiş özgün içerik; üçüncü taraf asset lisansı yok | Yok — paketleme sırasında yerel üretim | Windows EXE/installer simgesi; haricî görsel kullanılmaz; doğrulandı |

Bu envanter üçüncü taraf materyale hak atfetmez. Özgün oluşturulan dosyalar Project Blue
kapsamında kullanım ve değişiklik için sağlanır; projenin genel kaynak kodu dağıtım lisansı
henüz proje sahibi tarafından seçilmemiştir.

## Build kuralı

`verifyAssetLicenses`, paketlenebilir dosyaları açık allowlist ve SHA-256 envanteri ile kontrol
eder. Listede olmayan veya doğrulama sonrasında değiştirilmiş dosya build'i durdurur.
Hash listesi `verified-assets.properties` içindedir. Lisans belgelemek tek başına hak sağlamaz:
yeni asset ancak kaynağı ve kullanım hakkı gerçekten doğrulandıktan sonra listeye eklenebilir.
Koddan üretilen görsellerin değişiklikleri kod incelemesine tabidir.

İleride yalnızca özgün üretilmiş, satın alınmış, CC0/public-domain veya ticari kullanıma
açık asset kabul edilir. Kaynak, üretici, lisans, adres ve kullanım/atıf koşulları her asset
için kaydedilir; gerekli lisans metni ve satın alma kanıtı uygun yerde saklanır.
Sky Force'a ait asset, arayüz, bölüm veya ayırt edici tasarım kullanılmaz.

Yeniden üretim: proje kökünde `java tools/GenerateAssets.java`.
