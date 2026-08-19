# exiftool-java

Java 实现 ExifTool 元数据读写库。从 [ExifTool](https://exiftool.org)（Perl，Phil Harvey）逐阶段移植，以 **golden test**（对比 `exiftool -json` 输出）保证等价性。

> **License**：GPL-3.0（ExifTool 的 Java 衍生作品，版权声明见 NOTICE，完整许可见 LICENSE）

## 项目来源

本项目由 **Qwen Code** 辅助创建（模型：**deepseek-v4-flash**），从零到完整功能分 20 个阶段迭代完成：读取链路 → 写路径 → CLI → 长尾格式 → 内嵌 EXIF 读写，每个阶段均以 `exiftool -json` 输出做 golden test 验证等价性。

## 快速开始

```java
// 读取
ExifTool et = new ExifTool();
Map<String, Object> info = et.imageInfo(new File("photo.jpg"));
// {Make=NIKON CORPORATION, Model=NIKON D70, ExposureTime=1/60, ...}

// 写入（修改/新增/删除标签）
byte[] out = et.writeImage(fileBytes, Map.of(
    "Artist", "John Doe",     // 新增或修改（值为显示字符串）
    "Software", null          // null 表示删除该标签
));
```

读取值为**显示字符串**（对应 exiftool 的 `GetValue('PrintConv')`），写入值也是显示字符串（自动逆转换编码），与 `exiftool -json` 输出一致。

## 构建与测试

```bash
mvn compile        # Java 17+
mvn test           # 80 个测试全绿
```

## 命令行

```bash
# 构建后运行（或 mvn package 后用 jar）
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -s -Model photo.jpg
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -json photo.jpg
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -n -s -Orientation photo.jpg   # raw 值
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -G1 -s -Make photo.jpg          # 组前缀（[IFD0]）
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -G2 -s -Make photo.jpg          # 类别（[Camera]）
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -G3 -s -CameraType photo.jpg    # 厂商（[Canon]）
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -Artist="John Doe" photo.jpg   # 写入（EXIF）
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -Title="My Title" photo.jpg    # 写入（XMP）
java -cp target/classes com.gdxsoft.easyweb.exiftool.cli.Main -Software= photo.jpg           # 删除

# 支持参数：-s（简短）、-json、-n（raw）、-G0~-G3（组前缀）、-TAG（打印）、-TAG=VALUE（写）、-TAG=（删）
```

## 已支持

| 范围 | 内容 |
|---|---|
| **文件格式** | JPEG、TIFF、PNG、GIF、WebP、HEIC、MOV/MP4、RAF、NEF、CR2、MRW、DNG |
| **EXIF 目录** | IFD0/IFD1、ExifIFD、GPS、InteropIFD、PrintIM、IPTC IIM、XMP（标准+扩展） |
| **MakerNotes** | Nikon、Canon、FujiFilm、Minolta、Kodak、Casio（+ Sony PrintIM） |
| **内嵌 EXIF 读写** | JPEG APP1、WebP EXIF chunk、PNG eXIf、HEIC/AVIF meta/mdat item（全部读写） |
| **值转换** | ~130 个查表 + 函数型转换器（CanonEv/APEX/GPS DMS/FormatString/UserComment/CFAPattern） |
| **写路径** | EXIF（改/增/删，5 格式任意大小）+ XMP（JPEG/PNG/TIFF 改增）；MakerNotes/子目录/缩略图保留；HEIC 无 EXIF 自动新增 item |
| **CLI** | exiftool 参数子集（-s/-json/-n/-G0~-G3/-TAG/-TAG=VALUE） |

### 写路径

- `writeImage(byte[], Map<tagName, value>)`：value 为显示字符串（新增/修改），null 表示删除
- 递归重建 IFD0/ExifIFD/GPS/Interop/IFD1，未修改标签原样保留
- MakerNotes 作为不透明块复制（Nikon/Fuji 偏移相对块内 base 随块平移，Canon 相对 TIFF 头位置不变——均无需 fixup）
- 缩略图数据复制 + ThumbnailOffset 修正
- 各格式写入策略：JPEG/TIFF 全重建；WebP/PNG chunk 替换/新增；HEIC iloc/mdat 原位或扩容插入
- 限制：新增标签写入 IFD0（ExifIFD 区间自动建目录）；rational 逆转换支持 `num/den` 与数值

## 架构

```
com.gdxsoft.easyweb.exiftool
├── ExifTool               # 主入口：imageInfo() → Map<tag, displayValue>
├── TagInfo / TagTable     # 标签元数据（含 SubDirectory 指针、优先级、转换器）
├── ExifFormat             # EXIF 格式系统（int16u/rational64u/...）
├── ValueConverter         # 转换链接口：valueConv → printConv（对应 ValueConv→PrintConv）
├── convert/               # 各厂商转换器（Exif/Nikon/Canon/FujiFilm/GPS）
├── read/
│   ├── ExifParser         # IFD 解析（镜像 ProcessExif）+ 0x927C MakerNote 分发
│   ├── BinaryDataParser   # 二进制目录（镜像 ProcessBinaryData）
│   ├── IptcParser         # IPTC IIM 数据集
│   ├── JpegParser / PngParser / GifParser / WebpParser
└── tables/                # 各厂商标签表（从 %tagInfo 移植）
```

### 数据流（对应 Perl 原实现）

```
imageInfo(byte[]) → 按魔数分发
  → JpegParser：段扫描 → APP1/Exif → ExifParser
    → processIFD：12 字节条目循环（tagID+format+count+offset）
      → ValueReader：格式解码（rational 用 %.10g 字符串化）
      → TagInfo 查找 → SubDirectory 递归 / BinaryData 解析 / MakerNote 分发
      → valueConv → printConv → foundTag（优先级模型）
```

## 移植方法论（关键 Perl 语义）

从 Perl 移植时确认的等价规则，任何新代码都应遵循：

| 语义 | 规则 |
|---|---|
| **FoundTag 优先级** | 同优先级**后出现者覆盖**（旧值移入 "Name (N)"，CLI 默认隐藏）；IFD1/PreviewIFD 是低优先级目录 |
| **rational 显示** | 读为 `%.10g` 字符串（RoundFloat）：`3443.946188`、`180` |
| **double 显示** | 15 位有效数字字符串化：`33.4179196429669` |
| **PrintConv 未命中** | 输出 `"Unknown (N)"` |
| **二进制目录偏移** | `偏移 = tagId × 表默认格式 size`（FIRST_ENTRY 不参与偏移） |
| **MakerNote 偏移** | 各厂商不同：Nikon 相对 valuePtr+10、Canon 相对 **EXIF TIFF 头**、FujiFilm 相对值起始（IFD 在 valuePtr+12） |
| **IPTC 数据集 ID** | 十进制编号（92=Sub-location、95=Province-State、101=Country） |

## 测试策略

以本机 ExifTool 13.59 为 oracle：

1. 测试图片来自 exiftool 仓库 `t/images/`（已复制到 `src/test/resources/`）
2. 对每张图运行 `exiftool -json` 生成参考值
3. JUnit golden test 断言逐标签匹配

```
src/test/resources/
├── Motorola.jpg   # Phase 1：核心 EXIF（无 MakerNotes）
├── NikonD70.jpg   # Phase 2：Nikon Type 2/3 MakerNotes（80+ 标签）
├── Canon.jpg      # Phase 2/3：Canon CameraSettings/ShotInfo/FileInfo
├── FujiFilm.jpg   # Phase 4：FujiFilm MakerNotes
├── Sony.jpg       # Phase 4：PrintIM
├── PNG.png / GIF.gif / RIFF.webp / ExifTool.tif / GeoTiff.tif
```

新增标签的流程：`exiftool -json` 取参考值 → 移植定义到 tables/ → golden test 断言 → 全量回归。

## 路线图

- [ ] **GoPro/Sigma 等其余 MakerNotes**
- [ ] **HEIC 新增 EXIF item**（当前仅更新已存在 item）
- [ ] **CLI -G 组输出、PNG/TIFF 的 XMP 写**
- [ ] **完整覆盖**（24.7 万行 Perl → 数年工程，当前聚焦常见场景）

## 参考

- 移植源：`/Volumes/T2/gits/exiftool`（13.59 tag）
- 核心对照：`lib/Image/ExifTool/Exif.pm`、`ExifTool.pm`、`Nikon.pm`、`Canon.pm`、`FujiFilm.pm`、`IPTC.pm`、`MakerNotes.pm`
- 原始项目：https://exiftool.org
