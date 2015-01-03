/****************************************************************************
 Author: Luma (stubma@gmail.com)
 
 https://github.com/stubma/cocos2dx-better
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ****************************************************************************/
#ifndef __CCResourceLoader_h__
#define __CCResourceLoader_h__

#include "CCResourceLoaderListener.h"
#include "support/locale/CCLocalization.h"
#include "ccTypes.h"
#include <vector>
#include "actions/CCActionInstant.h"

using namespace std;

NS_CC_BEGIN

class CCImage;
class CCCallFunc;

/// load parameter
struct CCResourceLoadTask {
    /// idle time after loaded
    float idle;
    
    CCResourceLoadTask() : idle(0.1f) {
    }
    
    virtual ~CCResourceLoadTask() {}
    
    /// do loading
    virtual void load() {}
};

/// android string load task
struct AndroidStringLoadTask : public CCResourceLoadTask {
    /// language
    string lan;
    
    /// file path
    string path;
    
    /// merge or not
    bool merge;
    
    virtual ~AndroidStringLoadTask() {}
    
    virtual void load() {
        CCLocalization::sharedLocalization()->addAndroidStrings(lan, path, merge);
    }
};

/// cocosdenshion music load parameter
struct CDMusicTask : public CCResourceLoadTask {
    /// image name
    string name;
    
    virtual ~CDMusicTask() {}
    
    virtual void load();
};

/// cocosdenshion effect load parameter
struct CDEffectTask : public CCResourceLoadTask {
    /// image name
    string name;
    
    virtual ~CDEffectTask() {}
    
    virtual void load();
};

/// bitmap font load task
struct BMFontLoadTask : public CCResourceLoadTask {
    /// fnt file name
    string name;
    
    virtual ~BMFontLoadTask() {}
    
    virtual void load();
};

/// bitmap font load task, image is encrypted
struct EncryptedBMFontLoadTask : public CCResourceLoadTask {
    /// fnt file name
    string name;
    
    /// decrypt func
    CC_DECRYPT_FUNC func;
    
    virtual ~EncryptedBMFontLoadTask() {}
    
    virtual void load();
};

/// image load parameter
struct ImageLoadTask : public CCResourceLoadTask {
    /// image name
    string name;
    
    virtual ~ImageLoadTask() {}
    
    virtual void load();
};

/// encrypted image load parameter
struct EncryptedImageLoadTask : public CCResourceLoadTask {
    /// image name
    string name;
    
    /// decrypt function
    CC_DECRYPT_FUNC func;
    
    virtual ~EncryptedImageLoadTask() {}
    
    virtual void load();
};

/// zwoptex load parameter
struct ZwoptexLoadTask : public CCResourceLoadTask {
    /// plist name
    string name;
    
    virtual ~ZwoptexLoadTask() {}
    
    virtual void load();
};

/// encrypted zwoptex load task
struct EncryptedZwoptexLoadTask : public CCResourceLoadTask {
    /// plist name, plist is not encrypted
    string name;
    
    /// texture name, which is encrypted
    string texName;
    
    // decrypt func
    CC_DECRYPT_FUNC func;
    
    virtual ~EncryptedZwoptexLoadTask() {}
    
    virtual void load();
};

/// zwoptex animation load parameter
struct ZwoptexAnimLoadTask : public CCResourceLoadTask {
    /// frame list
    typedef vector<string> StringList;
    StringList frames;
    
    /// animation name
    string name;
    
    /// animation unit delay
    float unitDelay;
    
    /// restore original frame when animate is done
    bool restoreOriginalFrame;
    
    ZwoptexAnimLoadTask() :
				unitDelay(0),
				restoreOriginalFrame(false) {
                }
    
    virtual ~ZwoptexAnimLoadTask() {}
    
    virtual void load();
};

/// zwoptex animation load parameter
/// it can specify duration for every single frame
struct ZwoptexAnimLoadTask2 : public CCResourceLoadTask {
    /// frame list
    typedef vector<string> StringList;
    StringList frames;
    
    /// duration list
    typedef vector<float> TimeList;
    TimeList durations;
    
    /// restore original frame when animate is done
    bool restoreOriginalFrame;
    
    /// animation name
    string name;
    
    ZwoptexAnimLoadTask2() :
    restoreOriginalFrame(false) {
    }
    
    virtual ~ZwoptexAnimLoadTask2() {}
    
    virtual void load();
};

struct ArmatureTask : public CCResourceLoadTask {
    string configFilePath;
    
    ArmatureTask() {
    }
    
    virtual ~ArmatureTask() {
    }
    
    virtual void load();
};

struct CustomTask : public CCResourceLoadTask {
    CCCallFunc* func;
    
    CustomTask() :
    func(nullptr) {
    }
    
    virtual ~CustomTask() {
        CC_SAFE_RELEASE(func);
    }
    
    virtual void load() {
        func->execute();
    }
};

/**
 * A self-retain class for resource loading. It schedule resource loading in OpenGL thread in
 * every tick. One resource is handled by one Task, the loading logic is encapsulated in task so
 * you don't care about that. For CPU intensive task, you can set idle time to avoid blocking OpenGL
 * thread too long. If you display an animation feedback, don't use CCAction mechanism because a long
 * task will cause animation to skip frames. The better choice is invoking setDisplayFrame one by one.
 *
 * \par
 * Decryption is supported and you can provide a decrypt function pointer to load method. Of course you
 * need write an independent tool to encrypt your resources, that's your business.
 *
 * \par
 * Resources supported
 * <ul>
 * <li>Android style strings xml file. It is handy tool to use it with CCLocalization</li>
 * <li>Single image file, encrypted or not</li>
 * <li>Atlas image file, encrypted or not</li>
 * <li>atlas animation</li>
 * <li>Audio file supported by CocosDenshion</li>
 * </ul>
 * If not supported, just adding a task to support it.
 */
class CC_DLL CCResourceLoader : public CCObject {
private:
    /// type of load operation
    enum ResourceType {
        IMAGE,
        ZWOPTEX,
        ANIMATION
    };
    
private:
	/// listener
	CCResourceLoaderListener* m_listener;

	/// remaining delay time
    float m_remainingIdle;
    
    /// next loading item
    int m_nextLoad;
    
    /// load list
    typedef vector<CCResourceLoadTask*> LoadTaskPtrList;
    LoadTaskPtrList m_loadTaskList;
    
    /// flag indicating it is running
    bool m_loading;

private:
	/// perform loading
	void doLoad(float delta);

public:
    CCResourceLoader(CCResourceLoaderListener* listener);
	virtual ~CCResourceLoader();
    
    /// abort all active resource loading
    static void abortAll();
	
    /**
     * load a file and return raw data
     *
     * @param name file path
     * @param decFunc decrypt function or nullptr if file is not encrypted
     * @return raw data of file, caller should release it
     */
    static unsigned char* loadRaw(const string& name, unsigned long* size, CC_DECRYPT_FUNC decFunc = nullptr);
    
    /**
     * load a file and return a c string
     *
     * @param name file path
     * @param decFunc decrypt function or nullptr if file is not encrypted
     * @return c string of file content, caller should release it
     */
    static char* loadCString(const string& name, CC_DECRYPT_FUNC decFunc = nullptr);
    
    /**
     * load a file and return a c++ string
     *
     * @param name file path
     * @param decFunc decrypt function or nullptr if file is not encrypted
     * @return c string of file content, caller should release it
     */
    static string loadString(const string& name, CC_DECRYPT_FUNC decFunc = nullptr);
    
	/**
	 * a static method used to load an encrypted image
	 *
	 * @param name name of image file, it should be encrypted
	 * @param decFunc decrypt func
	 */
	static void loadImage(const string& name, CC_DECRYPT_FUNC decFunc);
    
	/**
	 * a static method used to load an encrypted zwoptex resource, the plist should not be encrypted
	 *
	 * @param plistName name of plist file, it should not be encrypted
	 * @param texName name of image file, it should be encrypted
	 * @param decFunc decrypt func
	 */
	static void loadZwoptex(const string& plistName, const string& texName, CC_DECRYPT_FUNC decFunc);
    
    /// unload image
    static void unloadImages(const string& tex);
    
    /// unload images
    static void unloadImages(const string& texPattern, int start, int end);
    
    /// unload sprite frames
    static void unloadSpriteFrames(const string& plistPattern, const string& texPattern, int start, int end);
	
    /// unload armature
    static void unloadArmatures(string plistPattern, string texPattern, int start, int end, string config);
    
    /// start loading
    void run();
    
    /// do loading in block mode
    void runInBlockMode();
    
    /// abort loading, you can't abort it if runs in block mode
    void abort();
    
    /// directly add a load task
    void addLoadTask(CCResourceLoadTask* t);
    
    /// add a custom task and the task is executing a function
    void addCustomTask(CCCallFunc* func);
    
    /// add bitmap font loading task
    void addBMFontTask(const string& fntFile, float idle = 0);
    
    /// add bitmap font loading task, and the bitmap font atlas is encrypted
    void addBMFontTask(const string& fntFile, CC_DECRYPT_FUNC decFunc, float idle = 0);
    
    /**
     * add an Android string loading task
     *
     * @param lan language ISO 639-1 code
     * @param path string XML file platform-independent path
     * @param merge true means merge new strings, or false means replace current strings
     */
    void addAndroidStringTask(const string& lan, const string& path, bool merge = false);
	
	/// add a image loading task
	void addImageTask(const string& name, float idle = 0);
	
	/**
	 * add a image task, but the texture is encrypted. So a decrypt function must be provided.
	 *
	 * @param name name of image file, it should be encrypted
	 * @param decFunc decrypt func
	 * @param idle idle time after loaded
	 */
	void addImageTask(const string& name, CC_DECRYPT_FUNC decFunc, float idle = 0);
	
	/// add a zwoptex image loading task
	void addZwoptexTask(const string& name, float idle = 0);
	
	/// add a multipack zwoptex image loading task
	void addZwoptexTask(const string& pattern, int start, int end, float idle = 0);
	
	/**
	 * add a zwoptex task, but the texture is encrypted. So a decrypt function must be provided.
	 *
	 * @param plistName name of plist file, it should not be encrypted
	 * @param texName name of image file, it should be encrypted
	 * @param decFunc decrypt func
	 * @param idle idle time after loaded
	 */
	void addZwoptexTask(const string& plistName, const string& texName, CC_DECRYPT_FUNC decFunc, float idle = 0);
	
	/**
	 * add a multipack zwoptex task, but the texture is encrypted. So a decrypt function must be provided.
	 *
	 * @param plistPattern pattern of plist file, it should not be encrypted
	 * @param texPattern name pattern of image file, it should be encrypted
	 * @param start start index in pattern
	 * @param end end index in pattern
	 * @param decFunc decrypt func
	 * @param idle idle time after loaded
	 */
	void addZwoptexTask(const string& plistPattern, const string& texPattern, int start, int end, CC_DECRYPT_FUNC decFunc, float idle = 0);
	
	/// add a cocosdenshion effect task
	void addCDEffectTask(const string& name, float idle = 0);
	
	/// add a cocosdenshion music task
	void addCDMusicTask(const string& name, float idle = 0);
	
	/// add a zwoptex animation loading task
	/// the endIndex is inclusive
	void addZwoptexAnimTask(const string& name,
							float unitDelay,
							const string& pattern,
							int startIndex,
							int endIndex,
							bool restoreOriginalFrame = false,
							float idle = 0);
	
	/// add a zwoptex animation loading task
	/// the endIndex is inclusive
	/// this method can specify two sets of start/end index so the
	/// animation can have two stage
	void addZwoptexAnimTask(const string& name,
							float unitDelay,
							const string& pattern,
							int startIndex,
							int endIndex,
							int startIndex2,
							int endIndex2,
							bool restoreOriginalFrame = false,
							float idle = 0);
    
    /**
     * add a zwoptex animation loading task, you can specify delay for every frame
     *
     * @param name animation name
     * @param pattern sprite frame pattern, something likes frame_%d.png, the parameter
     *      must be an integer
     * @param startIndex sprite frame pattern start index
     * @param endIndex sprite frame pattern end index
     * @param delayString delay time string in format "[float,float,...]"
     * @param restoreOriginalFrame restore original frame or not
     * @param idle idle time after task is completed
     */
    void addZwoptexAnimTask(const string& name,
                            const string& pattern,
                            int startIndex,
                            int endIndex,
                            const string& delayString,
                            bool restoreOriginalFrame = false,
                            float idle = 0);

    /**
     * add a zwoptex animation loading task, you can specify delay for every frame
     *
     * @param name animation name
     * @param pattern sprite frame pattern, something likes frame_%d.png, the parameter
     *      must be an integer
     * @param indicesString a string in format "[num,num,...]"
     * @param delay delay time
     * @param restoreOriginalFrame restore original frame or not
     * @param idle idle time after task is completed
     */
    void addZwoptexAnimTask(const string& name,
                            const string& pattern,
                            const string& indicesString,
                            float delay,
                            bool restoreOriginalFrame = false,
                            float idle = 0);
    
    /**
     * add a zwoptex animation loading task, you can specify delay for every frame
     *
     * @param name animation name
     * @param pattern sprite frame pattern, something likes frame_%d.png, the parameter
     *      must be an integer
     * @param indicesString a string in format "[num,num,...]"
     * @param delayString delay time string in format "[float,float,...]
     * @param restoreOriginalFrame restore original frame or not
     * @param idle idle time after task is completed
     */
    void addZwoptexAnimTask(const string& name,
                            const string& pattern,
                            const string& indicesString,
                            const string& delayString,
                            bool restoreOriginalFrame = false,
                            float idle = 0);
    
    /**
     * add an armature config file load task, you should add related image task for this
     *
     * @param config path of config file
     * @param idle idle time after task is completed
     */
    void addArmatureTask(string config, float idle = 0);
    
    /**
     * add an armature config file task, also load related image files. Decryption is 
     * optional and you should not encrypt plist and config file
     * 
     * @param plist path of atlas plist file
     * @param tex path of atlas image file
     * @param config path of armature config file
     * @param func decrypte func, default is nullptr
     * @param idle idle time after task is completed
     */
    void addArmatureTask(string plist, string tex, string config, CC_DECRYPT_FUNC func = nullptr, float idle = 0);
    
    /**
     * add an armature config file task, also load related image files. Decryption is
     * optional and you should not encrypt plist and config file
     *
     * @param plistPattern path pattern of atlas plist file
     * @param texPattern path pattern of atlas image file
     * @param start start parameter in pattern
     * @param end end parameter in pattern
     * @param config path of armature config file
     * @param func decrypte func, default is nullptr
     * @param idle idle time after task is completed
     */
    void addArmatureTask(string plistPattern, string texPattern, int start, int end, string config, CC_DECRYPT_FUNC func = nullptr, float idle = 0);
	
	/// delay time before start to load
	CC_SYNTHESIZE(float, m_delay, Delay);
};

NS_CC_END

#endif // __CCResourceLoader_h__
