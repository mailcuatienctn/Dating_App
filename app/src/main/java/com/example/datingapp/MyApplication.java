// MyApplication.java
package com.example.datingapp; // Đảm bảo đúng với tên package của bạn

import android.app.Application;
import com.cloudinary.android.MediaManager; // Import thư viện MediaManager của Cloudinary

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo Cloudinary SDK với thông tin tài khoản của bạn
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dmmf5ximm");
        config.put("api_key", "975318165359329");
        config.put("api_secret", "ydn0aOvPAOobKRity9EfdnAlzRE");

        MediaManager.init(this, config);
    }
}