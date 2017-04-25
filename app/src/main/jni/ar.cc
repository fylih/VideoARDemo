/**
* Copyright (c) 2015-2016 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
* EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
* and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
*/

#include "ar.hpp"
#include <algorithm>
#ifdef ANDROID
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EasyAR", __VA_ARGS__)
#else
#define LOGI(...) printf(__VA_ARGS__)
#endif

namespace EasyAR{
namespace samples{

class HelloCallBack : public TargetLoadCallBack
{
public:
    virtual ~HelloCallBack() {};
    virtual void operator() (const Target target, const bool status)
    {
        LOGI("load target: %s (%d) %s\n", target.name(), target.id(), status ? "success" : "fail");
        delete this;
    }
};

AR::AR()
{
    portrait_ = false;
}

AR::~AR()
{
    clear();
}

bool AR::initCamera()
{
    bool status = true;
    status &= camera_.open();
    camera_.setSize(Vec2I(1280, 720));
    status &= tracker_.attachCamera(camera_);
    status &= augmenter_.attachCamera(camera_);
    return status;
}

void AR::loadFromImage(const std::string& path)
{
    ImageTarget target;
    std::string jstr = "{\n"
                       "  \"images\" :\n"
                       "  [\n"
                       "    {\n"
                       "      \"image\" : \"" + path + "\",\n"
                       "      \"name\" : \"" + path.substr(0, path.find_first_of(".")) + "\"\n"
                       "    }\n"
                       "  ]\n"
                       "}";
    target.load(jstr.c_str(), EasyAR::kStorageAssets | EasyAR::kStorageJson);
    tracker_.loadTarget(target, new HelloCallBack());
}

void AR::loadFromJsonFile(const std::string& path, const std::string& targetname)
{
    ImageTarget target;
    target.load(path.c_str(), EasyAR::kStorageAssets, targetname.c_str());
    tracker_.loadTarget(target, new HelloCallBack());
}

void AR::loadAllFromJsonFile(const std::string& path)
{
    TargetList targets = ImageTarget::loadAll(path.c_str(), EasyAR::kStorageAssets);
    for (int i = 0; i < targets.size(); ++i) {
        tracker_.loadTarget(targets[i], new HelloCallBack());
    }
}

bool AR::start()
{
    bool status = true;
    status &= camera_.start();
    camera_.setFocusMode(CameraDevice::kFocusModeContinousauto);
    status &= tracker_.start();
    return status;
}

bool AR::stop()
{
    bool status = true;
    status &= tracker_.stop();
    status &= camera_.stop();
    return status;
}

bool AR::clear()
{
    bool status = true;
    status &= stop();
    status &= camera_.close();
    camera_.clear();
    tracker_.clear();
    augmenter_.clear();
    return status;
}

void AR::resizeGL(int width, int height)
{
    Vec2I size = Vec2I(1, 1);
    if(camera_.isOpened())
        size = camera_.size();
    if (size[0] == 0 || size[1] == 0)
        return;
    if(portrait_)
        std::swap(size[0], size[1]);
    float scaleRatio = std::max((float)width / (float)size[0], (float)height / (float)size[1]);
    Vec2I viewport_size = Vec2I((int)(size[0] * scaleRatio), (int)(size[1] * scaleRatio));
    viewport_ = Vec4I(0, height - viewport_size[1], viewport_size[0], viewport_size[1]);
}

void AR::initGL()
{

}

void AR::render()
{

}

void AR::setPortrait(bool portrait)
{
    portrait_ = portrait;
}

ARVideo::ARVideo()
{
    prepared_ = false;
    found_ = false;
    callback_ = NULL;
}

ARVideo::~ARVideo()
{
    player_.close();
    if(callback_)
        delete callback_;
}

void ARVideo::openVideoFile(const std::string& path, int texid)
{
    if(!callback_)
        callback_ = new CallBack(this);
    path_ = path;
    player_.setRenderTexture(texid);
    player_.setVideoType(VideoPlayer::kVideoTypeNormal);
    player_.open(path.c_str(), kStorageAssets, callback_);
}

void ARVideo::openTransparentVideoFile(const std::string& path, int texid)
{
    if(!callback_)
        callback_ = new CallBack(this);
    path_ = path;
    player_.setRenderTexture(texid);
    player_.setVideoType(VideoPlayer::kVideoTypeTransparentSideBySide);
    player_.open(path.c_str(), kStorageAssets, callback_);
}

void ARVideo::openStreamingVideo(const std::string& url, int texid)
{
    if(!callback_)
        callback_ = new CallBack(this);
    path_ = url;
    player_.setRenderTexture(texid);
    player_.setVideoType(VideoPlayer::kVideoTypeNormal);
    player_.open(url.c_str(), kStorageAbsolute, callback_);
}

void ARVideo::setVideoStatus(VideoPlayer::Status status)
{
    LOGI("video: %s (%d)\n", path_.c_str(), status);
    if (status == VideoPlayer::kVideoReady) {
        prepared_ = true;
        if (found_)
            player_.play();
    }
    if (status == VideoPlayer::kVideoCompleted) {
        if (found_)
            player_.play();
    }
}

void ARVideo::onFound()
{
    found_ = true;
    if (prepared_) {
        player_.play();
    }
}

void ARVideo::onLost()
{
    found_ = false;
    if (prepared_)
        player_.pause();
}

void ARVideo::update()
{
    player_.updateFrame();
}

ARVideo::CallBack::CallBack(ARVideo* video)
{
    video_ = video;
}

void ARVideo::CallBack::operator() (VideoPlayer::Status status)
{
    video_->setVideoStatus(status);
}

}
}
