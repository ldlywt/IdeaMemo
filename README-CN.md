<div align="center">
    <h1>IdeaMemo</h1>
    <p>一款用 Jetpack Compose 编写的轻量级卡片笔记应用，数据保存在本地或你的 WebDAV。</p>
    <a target="_blank" href="https://github.com/ldlywt/IdeaMemo/blob/master/README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;  简体中文&nbsp;&nbsp;</p>
    <a href="https://imgse.com/i/pEK8AXT"><img src="https://s21.ax1x.com/2025/02/15/pEK8AXT.md.jpg" alt="pEK8AXT.jpg" border="0" /></a>
    <br/>
    <br/>
</div>

## 🖌️ 截图

<div>
    <img src="https://s21.ax1x.com/2025/02/15/pEK8gEQ.png" width="32.2%" alt="" />
    <img src="https://s21.ax1x.com/2025/02/15/pEK86Hg.png" width="32.2%" alt="" />
    <img src="https://s21.ax1x.com/2025/02/15/pEK8yDS.png" width="32.2%" alt="" />
</div>

## ✨ 功能特性

**IdeaMemo** 是一款 Android 轻量级卡片便签 App，以 #标签(Tag) 为核心组织灵感，支持图文混排与 Markdown 渲染。

### 📝 记录与整理
- **卡片笔记**：图文混排，支持可选**笔记标题**，正文以 Markdown 渲染。
- **标签系统**：以 #TAG 索引笔记，独立的标签页 + 首页可点击的标签行。
- **搜索**：首页顶部搜索框，全文检索。
- **相册模式**：小红书风格瀑布流，集中浏览带图片的笔记。
- **多选操作**：首页多选后批量移入回收站。
- **回收站**：删除的笔记先进入回收站，可恢复、彻底删除或一键清空。

### 🔍 回顾与发现
- **日历视图**：配合贡献**热力图**，日期筛选采用 iOS 滚轮样式。
- **随机漫步**：随时随机翻出一条旧笔记，重温过去的想法。
- **统计分析**：记录天数、笔记数、总字数、图片数一目了然。

### 🎨 个性化
- Material 3 **动态取色**，明暗主题随心切换。
- **正文字号**四档可调（小 / 标准 / 大 / 特大），全局生效。
- 应用内切换语言：English、简体中文、繁體中文。

### 🔒 隐私与安全
- **隐私优先**：几乎不申请任何权限，所有数据牢牢保存在本地数据库。
- **生物识别锁**：指纹 / 人脸解锁保护笔记。
- **自动识别链接开关**：可关闭手机号、邮箱、网址的自动链接化。

### 🔄 备份、同步与导入导出
- **WebDAV**：备份与恢复到自己的私有云，配置可随时编辑。
- **本地备份**：完整备份/恢复为紧凑 ZIP（含附件，体积约为旧版一半）。
- **导出**：支持 Markdown / TXT / HTML 三种格式，并记住上次导出位置。
- **导入**：支持 Markdown / TXT（IdeaMemo 导出的多篇 Markdown 自动按时间分段）与 HTML 备份还原（含标题）。
- **局域网分享**：同一网络下用电脑浏览器直接查看手机上的笔记。
- **分享为图片**：任意笔记可生成精美分享卡片，笔记中的照片也可保存到本地。

### 🛠️ 技术栈
- Kotlin + Jetpack Compose（Material 3）+ Navigation Compose
- Room + Paging + DataStore + Hilt
- Coil、Markwon、kizitonwose Calendar、SaltUI
- Ktor（客户端 + 服务端，用于局域网分享）、sardine-android（WebDAV）、zt-zip
- AndroidX Biometric、WorkManager、SplashScreen

## ✈️ 下载

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.ldlywt.note)
[<img src="https://s1.ax1x.com/2023/01/12/pSu1a36.png" alt="Get it on GitHub" height="80">](https://github.com/ldlywt/IdeaMemo/releases/latest)     

## 🤝 贡献指南
欢迎贡献！请按照以下步骤进行贡献：
1. Fork 本仓库。
2. 创建一个新分支（git checkout -b feature-branch-name）。
3. 提交你的修改（git commit -am 'Add some feature'）。
4. 推送到分支（git push origin feature-branch-name）。
5. 创建一个 Pull Request。

## 🤗 鸣谢

该项目由 Compose 创建。该项目的部分代码源自优秀的开源项目。

**Open Source Projects**

- [ReadYou](https://github.com/Ashinch/ReadYou)
- [MoeMemosAndroid](https://github.com/mudkipme/MoeMemosAndroid)
- [Animius](https://github.com/lanlinju/Animius)
- [SaltUI](https://github.com/Moriafly/SaltUI)
- [memos](https://github.com/usememos/memos)
- ...

## 🧾 License
GNU GPL v3.0 © [IdeaMemo](https://github.com/ldlywt/IdeaMemo/blob/master/LICENSE)

您的star是我最大的动力，谢谢！ **🌟**

## ☕️ 捐助
可以给我买一杯咖啡，让我更有动力继续开发。

<img src="https://github.com/user-attachments/assets/737f222d-cdf4-4091-b95d-9262e678b460" alt="1742624588864" width="163.8" height="245.7" style="margin-right: 200px;">

## 🌸 捐赠名单

| 捐赠者名字         | 金额 |
|-------------------|------|
| 尧孟张             | 8.8   |
| +（李刚)           | 5   |
| Tomo Ebizuka      | 15   |
| Corcube           | 10   |
| 奶酪很热情         | 10   |
| KavenSu          | 10   |

## ⭐ Star History

[![Star History Chart](https://star-history.dera.page/svg?repos=ldlywt/IdeaMemo&type=Date)](https://star-history.dera.page/#ldlywt/IdeaMemo&Date)
