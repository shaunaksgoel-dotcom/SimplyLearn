#!/bin/bash
set -e
echo "Installing ffmpeg static binary..."
cd /usr/local/bin
curl -L https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz -o ffmpeg.tar.xz
tar -xf ffmpeg.tar.xz
cd ffmpeg-*-static
cp ffmpeg ffprobe /usr/local/bin/
chmod +x /usr/local/bin/ffmpeg /usr/local/bin/ffprobe
echo "ffmpeg installed successfully"
ffmpeg -version