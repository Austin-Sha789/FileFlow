# FFmpeg video preview component

FileFlow uses an LGPL shared FFmpeg build from BtbN, a build provider linked from the FFmpeg project's download page.

- FFmpeg: https://ffmpeg.org/
- FFmpeg source revision: https://github.com/FFmpeg/FFmpeg/commit/ae19868753
- Build/release: https://github.com/BtbN/FFmpeg-Builds/releases/tag/autobuild-2026-10-29-19-17
- Original asset: `ffmpeg-n9.0.2-37-gae19868753-win64-lgpl-shared-9.0.zip`
- Original SHA-256: `44280849ca9ad4cffa466e1f244b8eaf3c4f28c3b1bef00801fadba9ec4f676b`

The prepared bundle retains upstream LICENSE.txt and the shared libraries needed by ffmpeg.exe. FileFlow invokes the executable as a separate process. Build configuration is available from `ffmpeg -buildconf`, and license information from `ffmpeg -L`. Published distributions should retain the upstream notice and license and provide corresponding source materials under the applicable licenses. See scripts/setup-video-thumbnails.ps1 for reproducible preparation.
